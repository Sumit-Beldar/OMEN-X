package com.omenx.backend.osiris;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

// =========================================================
// OSIRIS RUNTIME MANAGER
// Detects Node.js, npm, and Git binaries across Windows/Mac/Linux
// Windows-hardened: resolves absolute paths via `where.exe`,
// common well-known install directories, and ProcessBuilder
// list-form invocation (never shell injection, never bare "npm").
// =========================================================
public class OsirisRuntimeManager {

    private final OsirisConfig config;

    private String nodeExecutable;
    private String npmExecutable;
    private String gitExecutable;
    private boolean hasSystemGit;
    private String nodeVersion = "Unknown";
    private String npmVersion = "Unknown";

    // Tracks all paths tried during npm detection for diagnostics
    private final List<String> npmPathsTried = new ArrayList<>();

    public OsirisRuntimeManager(OsirisConfig config) {
        this.config = config;
        detectAll();
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    public static boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("nux") || os.contains("nix") || os.contains("aix");
    }

    public static String getOsArchKey() {
        String os = isWindows() ? "windows" : (isMac() ? "darwin" : "linux");
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return os + "-arm64";
        }
        return os + "-x64";
    }

    public synchronized void detectAll() {
        npmPathsTried.clear();
        detectNode();
        detectNpm();
        detectGit();
    }

    // =========================================================
    // DETECT NODE
    // =========================================================
    private void detectNode() {
        nodeExecutable = null;

        // 1. Custom path in config
        if (!config.getCustomNodePath().isBlank()) {
            Path custom = Paths.get(config.getCustomNodePath());
            if (Files.isRegularFile(custom)) {
                String abs = custom.toAbsolutePath().toString();
                if (testAbsoluteBinary(abs, "-v")) {
                    nodeExecutable = abs;
                    nodeVersion = runAbsoluteBinaryOutput(abs, "-v");
                    return;
                }
            }
        }

        // 2. Bundled paths (resources/node/<os-arch>/node[.exe])
        String exeName = isWindows() ? "node.exe" : "node";
        String osArch = getOsArchKey();
        List<Path> bundledCandidates = List.of(
                Paths.get("resources", "node", osArch, exeName),
                config.getBaseDir().resolve("bin").resolve("node").resolve(exeName),
                Paths.get(System.getProperty("user.home"), ".omenx", "bin", "node", osArch, exeName),
                Paths.get("node", osArch, exeName)
        );
        for (Path p : bundledCandidates) {
            if (Files.isRegularFile(p)) {
                ensureExecutable(p);
                String abs = p.toAbsolutePath().toString();
                if (testAbsoluteBinary(abs, "-v")) {
                    nodeExecutable = abs;
                    nodeVersion = runAbsoluteBinaryOutput(abs, "-v");
                    return;
                }
            }
        }

        // 3. System PATH via `where.exe node.exe` (Windows) or `which node` (Unix)
        String fromWhere = resolveViaWhere(isWindows() ? "node.exe" : "node");
        if (fromWhere != null && Files.isRegularFile(Paths.get(fromWhere))) {
            if (testAbsoluteBinary(fromWhere, "-v")) {
                nodeExecutable = fromWhere;
                nodeVersion = runAbsoluteBinaryOutput(nodeExecutable, "-v");
                return;
            }
        }

        // 4. Well-known Windows installation directories
        if (isWindows()) {
            List<Path> winCandidates = buildWindowsNodeCandidates();
            for (Path p : winCandidates) {
                if (Files.isRegularFile(p)) {
                    String abs = p.toAbsolutePath().toString();
                    if (testAbsoluteBinary(abs, "-v")) {
                        nodeExecutable = abs;
                        nodeVersion = runAbsoluteBinaryOutput(nodeExecutable, "-v");
                        return;
                    }
                }
            }
        }

        // 5. Unix fallback: `which node`
        if (!isWindows()) {
            String fromWhich = resolveViaWhere("node");
            if (fromWhich != null && Files.isRegularFile(Paths.get(fromWhich))) {
                if (testAbsoluteBinary(fromWhich, "-v")) {
                    nodeExecutable = fromWhich;
                    nodeVersion = runAbsoluteBinaryOutput(nodeExecutable, "-v");
                }
            }
        }
    }

    // =========================================================
    // DETECT NPM — Windows-Hardened
    // Never returns bare "npm" on Windows; always an absolute path
    // to npm.cmd (or npm on Unix). Verifies via absolute invocation.
    // =========================================================
    private void detectNpm() {
        npmExecutable = null;
        npmPathsTried.clear();

        // 1. If node was found at a custom/bundled/absolute path, look for sibling npm.cmd/npm
        if (nodeExecutable != null &&
                !nodeExecutable.equals("node") &&
                !nodeExecutable.equals("node.exe")) {
            Path nodeDir = Paths.get(nodeExecutable).getParent();
            if (nodeDir != null) {
                String siblingName = isWindows() ? "npm.cmd" : "npm";
                Path sibling = nodeDir.resolve(siblingName);
                npmPathsTried.add(sibling.toAbsolutePath().toString());
                if (Files.isRegularFile(sibling) && testAbsoluteBinary(sibling.toAbsolutePath().toString(), "-v")) {
                    npmExecutable = sibling.toAbsolutePath().toString();
                    npmVersion = runAbsoluteBinaryOutput(npmExecutable, "-v");
                    return;
                }
            }
        }

        // 2. `where.exe npm.cmd` on Windows, `which npm` on Unix — returns absolute path
        String whereResult = resolveViaWhere(isWindows() ? "npm.cmd" : "npm");
        if (whereResult != null) {
            npmPathsTried.add(whereResult);
            if (Files.isRegularFile(Paths.get(whereResult)) && testAbsoluteBinary(whereResult, "-v")) {
                npmExecutable = whereResult;
                npmVersion = runAbsoluteBinaryOutput(npmExecutable, "-v");
                return;
            }
        }

        // 3. Well-known Windows install locations (absolute paths, never bare names)
        if (isWindows()) {
            List<Path> candidates = buildWindowsNpmCandidates();
            for (Path p : candidates) {
                npmPathsTried.add(p.toAbsolutePath().toString());
                if (Files.isRegularFile(p) && testAbsoluteBinary(p.toAbsolutePath().toString(), "-v")) {
                    npmExecutable = p.toAbsolutePath().toString();
                    npmVersion = runAbsoluteBinaryOutput(npmExecutable, "-v");
                    return;
                }
            }
        }

        // 4. Unix fallback: look for npm relative to detected node executable dir
        if (!isWindows() && nodeExecutable != null) {
            Path dir = Paths.get(nodeExecutable).getParent();
            if (dir != null) {
                Path unixNpm = dir.resolve("npm");
                npmPathsTried.add(unixNpm.toAbsolutePath().toString());
                if (Files.isRegularFile(unixNpm) && testAbsoluteBinary(unixNpm.toAbsolutePath().toString(), "-v")) {
                    npmExecutable = unixNpm.toAbsolutePath().toString();
                    npmVersion = runAbsoluteBinaryOutput(npmExecutable, "-v");
                }
            }
        }
    }

    // =========================================================
    // DETECT GIT
    // =========================================================
    private void detectGit() {
        gitExecutable = null;
        hasSystemGit = false;

        if (!config.getCustomGitPath().isBlank()) {
            Path custom = Paths.get(config.getCustomGitPath());
            if (Files.isRegularFile(custom)) {
                String abs = custom.toAbsolutePath().toString();
                if (testAbsoluteBinary(abs, "--version")) {
                    gitExecutable = abs;
                    hasSystemGit = true;
                    return;
                }
            }
        }

        String fromWhere = resolveViaWhere(isWindows() ? "git.exe" : "git");
        if (fromWhere != null && Files.isRegularFile(Paths.get(fromWhere)) && testAbsoluteBinary(fromWhere, "--version")) {
            gitExecutable = fromWhere;
            hasSystemGit = true;
            return;
        }

        // Well-known Windows Git paths
        if (isWindows()) {
            List<Path> candidates = List.of(
                    Paths.get("C:\\Program Files\\Git\\bin\\git.exe"),
                    Paths.get("C:\\Program Files (x86)\\Git\\bin\\git.exe"),
                    Paths.get(System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files"), "Git", "bin", "git.exe"),
                    Paths.get(System.getenv().getOrDefault("LOCALAPPDATA", ""), "Programs", "Git", "bin", "git.exe")
            );
            for (Path p : candidates) {
                if (Files.isRegularFile(p) && testAbsoluteBinary(p.toAbsolutePath().toString(), "--version")) {
                    gitExecutable = p.toAbsolutePath().toString();
                    hasSystemGit = true;
                    return;
                }
            }
        }
    }

    // =========================================================
    // resolve via `where.exe` (Windows) or `which` (Unix)
    // Returns first absolute path found, or null.
    // =========================================================
    private String resolveViaWhere(String name) {
        try {
            List<String> cmd = isWindows()
                    ? List.of("where.exe", name)
                    : List.of("which", name);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean done = p.waitFor(4, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && Files.isRegularFile(Paths.get(line))) {
                        return line;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // =========================================================
    // Build well-known Windows Node.js install paths
    // =========================================================
    private List<Path> buildWindowsNodeCandidates() {
        List<Path> candidates = new ArrayList<>();
        Map<String, String> env = System.getenv();
        String progFiles   = env.getOrDefault("ProgramFiles",   "C:\\Program Files");
        String progFilesX86 = env.getOrDefault("ProgramFiles(x86)", "C:\\Program Files (x86)");
        String localAppData = env.getOrDefault("LOCALAPPDATA",  "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local");

        // Standard nvm / official LTS installer locations
        for (String root : List.of(progFiles, progFilesX86, localAppData + "\\Programs")) {
            candidates.add(Paths.get(root, "nodejs", "node.exe"));
        }
        // nvm-windows
        String nvmHome = env.getOrDefault("NVM_HOME", "");
        if (!nvmHome.isBlank()) {
            candidates.add(Paths.get(nvmHome, "node.exe"));
        }
        // Root of drives A-Z (unusual installs like E:\node.exe)
        for (char drive = 'C'; drive <= 'Z'; drive++) {
            candidates.add(Paths.get(drive + ":\\node.exe"));
        }
        // Chocolatey, Scoop, nvm
        candidates.add(Paths.get(localAppData, "Scoop", "apps", "nodejs", "current", "node.exe"));
        candidates.add(Paths.get(System.getProperty("user.home"), ".nvm", "versions", "node"));

        return candidates;
    }

    // =========================================================
    // Build well-known Windows npm.cmd install paths
    // =========================================================
    private List<Path> buildWindowsNpmCandidates() {
        List<Path> candidates = new ArrayList<>();
        Map<String, String> env = System.getenv();
        String progFiles    = env.getOrDefault("ProgramFiles",   "C:\\Program Files");
        String progFilesX86 = env.getOrDefault("ProgramFiles(x86)", "C:\\Program Files (x86)");
        String localAppData = env.getOrDefault("LOCALAPPDATA",   "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local");
        String appData      = env.getOrDefault("APPDATA",        "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Roaming");

        // Official Node.js installer puts npm.cmd next to node.exe
        for (String root : List.of(progFiles, progFilesX86, localAppData + "\\Programs")) {
            candidates.add(Paths.get(root, "nodejs", "npm.cmd"));
        }
        // npm global prefix (Roaming\npm)
        candidates.add(Paths.get(appData, "npm", "npm.cmd"));

        // nvm-windows
        String nvmHome = env.getOrDefault("NVM_HOME", "");
        if (!nvmHome.isBlank()) {
            candidates.add(Paths.get(nvmHome, "npm.cmd"));
        }

        // Same drive root as node, if node was found at absolute path
        if (nodeExecutable != null) {
            Path nodeDir = Paths.get(nodeExecutable).getParent();
            if (nodeDir != null) {
                candidates.add(nodeDir.resolve("npm.cmd"));
                candidates.add(nodeDir.resolve("node_modules").resolve("npm").resolve("bin").resolve("npm.cmd"));
            }
        }

        // Scoop, Chocolatey
        candidates.add(Paths.get(localAppData, "Scoop", "apps", "nodejs", "current", "npm.cmd"));
        candidates.add(Paths.get("C:\\ProgramData", "chocolatey", "bin", "npm.cmd"));

        // Drives A-Z root (handles E:\npm.cmd, unusual portable installs)
        for (char drive = 'C'; drive <= 'Z'; drive++) {
            candidates.add(Paths.get(drive + ":\\npm.cmd"));
        }

        return candidates;
    }

    // =========================================================
    // TEST A BINARY AT AN ABSOLUTE PATH
    // On Windows: .cmd/.bat via cmd.exe /c; others direct.
    // Never uses bare "npm" / "node" — always absolute.
    // =========================================================
    private boolean testAbsoluteBinary(String absolutePath, String arg) {
        try {
            List<String> cmd = buildInvokeCmd(absolutePath, arg);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // Drain stdout so the process can exit
            try (InputStream is = p.getInputStream()) {
                is.transferTo(OutputStream.nullOutputStream());
            }
            boolean done = p.waitFor(5, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception ignored) {
        }
        return false;
    }

    private String runAbsoluteBinaryOutput(String absolutePath, String arg) {
        try {
            List<String> cmd = buildInvokeCmd(absolutePath, arg);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                // drain rest
                r.lines().forEach(l -> {});
                p.waitFor(4, TimeUnit.SECONDS);
                return line != null ? line.trim() : "Unknown";
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    private List<String> buildInvokeCmd(String absolutePath, String... extraArgs) {
        List<String> cmd = new ArrayList<>();
        if (isWindows() && (absolutePath.endsWith(".cmd") || absolutePath.endsWith(".bat"))) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(absolutePath);
        } else {
            cmd.add(absolutePath);
        }
        for (String a : extraArgs) {
            cmd.add(a);
        }
        return cmd;
    }

    private void ensureExecutable(Path path) {
        if (!isWindows() && Files.exists(path)) {
            try {
                File f = path.toFile();
                if (!f.canExecute()) {
                    f.setExecutable(true, false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    // =========================================================
    // BUILD PROCESS COMMAND (always list-form, never shell-injected)
    // =========================================================
    public List<String> buildCommand(String... args) {
        if (args.length == 0) return new ArrayList<>();

        String executable = args[0];
        List<String> cmd = new ArrayList<>();

        // Determine if this is an absolute .cmd/.bat path (Windows) or a bare name
        boolean isCmdFile = isWindows() && (executable.endsWith(".cmd") || executable.endsWith(".bat"));
        boolean isBareNpmOnWindows = isWindows() && (executable.equalsIgnoreCase("npm") || executable.equalsIgnoreCase("npx"));

        if (isCmdFile) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(executable);
        } else if (isBareNpmOnWindows) {
            // Resolve absolute path before adding to command
            String resolvedNpm = npmExecutable != null ? npmExecutable : resolveViaWhere("npm.cmd");
            if (resolvedNpm != null) {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(resolvedNpm);
            } else {
                cmd.add("cmd.exe");
                cmd.add("/c");
                cmd.add(executable);
            }
        } else {
            cmd.add(executable);
        }

        // Add remaining args (skip first since we added the executable above)
        for (int i = 1; i < args.length; i++) {
            cmd.add(args[i]);
        }
        return cmd;
    }

    // =========================================================
    // DIAGNOSTICS — paths attempted during npm resolution
    // =========================================================
    public String getNpmDiagnostics() {
        StringBuilder sb = new StringBuilder("npm resolution — paths tried:\n");
        for (String path : npmPathsTried) {
            sb.append("  ").append(path)
              .append(Files.isRegularFile(Paths.get(path)) ? " [EXISTS]" : " [NOT FOUND]")
              .append("\n");
        }
        sb.append("Resolution result: ").append(npmExecutable != null ? npmExecutable : "NOT FOUND");
        return sb.toString();
    }

    // =========================================================
    // ACCESSORS
    // =========================================================
    public boolean isNodeAvailable() {
        return nodeExecutable != null;
    }

    public boolean isNpmAvailable() {
        return npmExecutable != null;
    }

    public boolean hasSystemGit() {
        return hasSystemGit;
    }

    public String getNodeExecutable() {
        return nodeExecutable;
    }

    public String getNpmExecutable() {
        return npmExecutable;
    }

    public String getGitExecutable() {
        return gitExecutable;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public String getNpmVersion() {
        return npmVersion;
    }

    public List<String> getNpmPathsTried() {
        return Collections.unmodifiableList(npmPathsTried);
    }
}
