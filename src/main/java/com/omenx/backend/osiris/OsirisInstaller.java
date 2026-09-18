package com.omenx.backend.osiris;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// =========================================================
// OSIRIS INSTALLER & CACHE MANAGER
// Handles 5-stage setup: Runtime, Clone (Git/JGit), npm install,
// npm build, asset copying, and .osiris-setup-complete caching.
// Windows-hardened file cleanup, locked folder staging, and
// descriptive stderr extraction.
// =========================================================
public class OsirisInstaller {

    private final OsirisConfig config;
    private final OsirisRuntimeManager runtimeManager;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private Runnable processStopper;

    public OsirisInstaller(OsirisConfig config, OsirisRuntimeManager runtimeManager) {
        this.config = config;
        this.runtimeManager = runtimeManager;
    }

    public void setProcessStopper(Runnable processStopper) {
        this.processStopper = processStopper;
    }

    // =========================================================
    // CACHE VALIDATION
    // Complete install requires marker, package.json, node_modules,
    // .git, and .next build output.
    // =========================================================
    public boolean isSetupValid() {
        Path marker = config.getMarkerFile();
        Path appDir = config.getAppDir();
        Path nodeModules = appDir.resolve("node_modules");
        Path nextDir = appDir.resolve(".next");
        Path pkgJson = appDir.resolve("package.json");
        Path gitDir = appDir.resolve(".git");

        if (!Files.isRegularFile(marker)) {
            return false;
        }

        if (!Files.isRegularFile(pkgJson) || !Files.isDirectory(nodeModules) ||
                !Files.isDirectory(nextDir) || !Files.isDirectory(gitDir)) {
            return false;
        }

        // Check if build artifact exists (either standalone server.js or standard BUILD_ID)
        boolean hasStandalone = Files.isRegularFile(nextDir.resolve("standalone").resolve("server.js"));
        boolean hasBuildId = Files.isRegularFile(nextDir.resolve("BUILD_ID"));

        return hasStandalone || hasBuildId;
    }

    public void cancel() {
        cancelled.set(true);
    }

    // =========================================================
    // RUN FULL SETUP PIPELINE
    // =========================================================
    public void runSetup(OsirisProgressListener listener) throws Exception {
        cancelled.set(false);

        // ─────────────────────────────────────────────────────
        // 0. CHECK IF ALREADY A VALID COMPLETE INSTALL
        // ─────────────────────────────────────────────────────
        if (isSetupValid()) {
            listener.onLog("[SETUP] Valid complete OSIRIS installation detected. Skipping clone & build.", false);
            listener.onProgress(1.0, "OSIRIS installation is valid.");
            listener.onStateChanged(OsirisProgressListener.OsirisState.READY_CACHED);
            return;
        }

        // ─────────────────────────────────────────────────────
        // STAGE 1: CHECK RUNTIME (0% - 5%)
        // ─────────────────────────────────────────────────────
        listener.onStateChanged(OsirisProgressListener.OsirisState.INSTALLING);
        listener.onStageChanged(
                OsirisProgressListener.OsirisStage.CHECK_RUNTIME,
                OsirisProgressListener.OsirisStage.CHECK_RUNTIME.getTitle(),
                OsirisProgressListener.OsirisStage.CHECK_RUNTIME.getDescription()
        );
        listener.onProgress(0.02, "Probing Node.js, npm, and Git runtimes...");

        runtimeManager.detectAll();

        if (!runtimeManager.isNodeAvailable()) {
            throw new IllegalStateException(
                    "Node.js runtime not found.\n" +
                    "Please install Node.js 18+ from https://nodejs.org/\n" +
                    "or configure a custom Node path in the Settings modal."
            );
        }

        if (!runtimeManager.isNpmAvailable()) {
            String diag = runtimeManager.getNpmDiagnostics();
            listener.onLog("[RUNTIME] npm not found. Diagnostics:\n" + diag, true);
            throw new IllegalStateException(
                    "npm not found after searching all known paths.\n" +
                    diag + "\n\n" +
                    "Please install Node.js LTS from https://nodejs.org/ " +
                    "— npm.cmd is bundled with Node.js."
            );
        }

        listener.onLog("[RUNTIME] Node: " + runtimeManager.getNodeExecutable() + " (" + runtimeManager.getNodeVersion() + ")", false);
        listener.onLog("[RUNTIME] npm : " + runtimeManager.getNpmExecutable() + " (" + runtimeManager.getNpmVersion() + ")", false);
        if (runtimeManager.hasSystemGit()) {
            listener.onLog("[RUNTIME] Git : " + runtimeManager.getGitExecutable(), false);
        } else {
            listener.onLog("[RUNTIME] System Git not detected; will use embedded pure Java JGit engine.", false);
        }

        listener.onProgress(0.05, "Runtime verification complete.");

        if (cancelled.get()) throw new InterruptedException("Setup cancelled by user.");

        // ─────────────────────────────────────────────────────
        // STAGE 2: CLONE REPOSITORY (5% - 30%)
        // Stop any child process, thoroughly wipe destination,
        // fallback to staging directory if locked, and never clone into non-empty dir.
        // ─────────────────────────────────────────────────────
        listener.onStageChanged(
                OsirisProgressListener.OsirisStage.CLONE_REPOSITORY,
                OsirisProgressListener.OsirisStage.CLONE_REPOSITORY.getTitle(),
                OsirisProgressListener.OsirisStage.CLONE_REPOSITORY.getDescription()
        );
        listener.onProgress(0.08, "Preparing clean destination directory...");

        stopActiveProcesses(listener);

        Path appDir = config.getAppDir();
        Files.createDirectories(appDir.getParent());

        Path targetCloneDir = appDir;
        boolean usingTempStaging = false;

        // Clean existing incomplete destination
        if (Files.exists(appDir) && !isDirectoryEmpty(appDir)) {
            listener.onLog("[CLONE] Incomplete directory detected; purging: " + appDir, false);
            boolean wiped = wipeDirectory(appDir, listener);

            if (!wiped || (Files.exists(appDir) && !isDirectoryEmpty(appDir))) {
                // Windows lock fallback: clone into clean sibling folder
                String tmpName = "app-tmp-" + System.currentTimeMillis();
                targetCloneDir = appDir.getParent().resolve(tmpName);
                usingTempStaging = true;
                listener.onLog("[CLONE] Destination locked by background process; staging in fresh folder: " + targetCloneDir, false);
                Files.createDirectories(targetCloneDir.getParent());
            }
        }

        // Strict guarantee: NEVER run git clone into a non-empty directory
        if (Files.exists(targetCloneDir) && !isDirectoryEmpty(targetCloneDir)) {
            wipeDirectory(targetCloneDir, listener);
            if (Files.exists(targetCloneDir) && !isDirectoryEmpty(targetCloneDir)) {
                String freshTmp = "app-tmp-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
                targetCloneDir = appDir.getParent().resolve(freshTmp);
                usingTempStaging = true;
            }
        }

        if (cancelled.get()) throw new InterruptedException("Setup cancelled by user.");

        // Execute clone
        if (runtimeManager.hasSystemGit()) {
            cloneWithSystemGit(listener, targetCloneDir);
        } else {
            cloneWithJGit(listener, targetCloneDir);
        }

        // If cloned into temporary staging folder, replace primary appDir
        if (usingTempStaging) {
            listener.onLog("[CLONE] Replacing primary application folder with staged clone...", false);
            replaceDirectoryWithStaged(targetCloneDir, appDir, listener);
        }

        listener.onProgress(0.30, "Repository clone complete.");

        if (cancelled.get()) throw new InterruptedException("Setup cancelled by user.");

        // ─────────────────────────────────────────────────────
        // STAGE 3: INSTALL DEPENDENCIES (30% - 70%)
        // ─────────────────────────────────────────────────────
        listener.onStageChanged(
                OsirisProgressListener.OsirisStage.INSTALL_DEPENDENCIES,
                OsirisProgressListener.OsirisStage.INSTALL_DEPENDENCIES.getTitle(),
                OsirisProgressListener.OsirisStage.INSTALL_DEPENDENCIES.getDescription()
        );
        listener.onProgress(0.35, "Running npm install (first run may take a few minutes)...");

        runProcessStream(
                appDir,
                listener,
                0.35,
                0.70,
                runtimeManager.buildCommand(runtimeManager.getNpmExecutable(), "install", "--no-audit", "--no-fund")
        );

        listener.onProgress(0.70, "Dependencies installed successfully.");

        // Verify node_modules/next is present — if not, npm install was corrupt/incomplete
        Path nextModule = appDir.resolve("node_modules").resolve("next");
        if (!Files.isDirectory(nextModule)) {
            // Wipe partial node_modules and surface a clear error
            listener.onLog("[INSTALL] node_modules/next missing after npm install — install appears corrupt.", true);
            Path nm = appDir.resolve("node_modules");
            if (Files.isDirectory(nm)) {
                listener.onLog("[INSTALL] Wiping corrupt node_modules before surfacing error.", true);
                wipeDirectory(nm, listener);
            }
            throw new IOException(
                    "npm install completed but node_modules/next is missing.\n" +
                    "This usually means npm download was interrupted or the registry returned ENOENT errors.\n" +
                    "Click Retry — the launcher will clean and re-run npm install."
            );
        }
        listener.onLog("[INSTALL] Verified: node_modules/next present.", false);

        if (cancelled.get()) throw new InterruptedException("Setup cancelled by user.");

        // ─────────────────────────────────────────────────────
        // STAGE 4: BUILD PRODUCTION (70% - 90%)
        // ─────────────────────────────────────────────────────
        listener.onStageChanged(
                OsirisProgressListener.OsirisStage.BUILD_PRODUCTION,
                OsirisProgressListener.OsirisStage.BUILD_PRODUCTION.getTitle(),
                OsirisProgressListener.OsirisStage.BUILD_PRODUCTION.getDescription()
        );
        listener.onProgress(0.72, "Compiling Next.js production build...");

        // Ensure .env.local is present for build
        config.syncEnvLocal();

        runProcessStream(
                appDir,
                listener,
                0.72,
                0.90,
                runtimeManager.buildCommand(runtimeManager.getNpmExecutable(), "run", "build")
        );

        // Copy standalone assets if Next.js standalone mode was triggered
        postBuildStandaloneAssetCopy(appDir, listener);

        listener.onProgress(0.90, "Next.js build complete.");

        if (cancelled.get()) throw new InterruptedException("Setup cancelled by user.");

        // ─────────────────────────────────────────────────────
        // STAGE 5: FINALIZE CACHE (90% - 100%)
        // ─────────────────────────────────────────────────────
        writeSetupMarker(appDir);
        listener.onProgress(1.0, "OSIRIS setup complete and cached.");
        listener.onStateChanged(OsirisProgressListener.OsirisState.READY_CACHED);
    }

    // =========================================================
    // SYSTEM GIT CLONE WITH REAL STDERR CAPTURE
    // =========================================================
    private void cloneWithSystemGit(OsirisProgressListener listener, Path targetDir) throws Exception {
        listener.onLog("[CLONE] Executing system Git clone from " + config.getRepoUrl() + " into " + targetDir.toAbsolutePath() + "...", false);

        List<String> cmd = List.of(
                runtimeManager.getGitExecutable(),
                "clone",
                "--progress",
                config.getRepoUrl(),
                targetDir.toAbsolutePath().toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        List<String> outputLines = new ArrayList<>();
        List<String> errorLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
                boolean isErr = line.toLowerCase().contains("fatal:") || line.toLowerCase().contains("error:");
                if (isErr) {
                    errorLines.add(line);
                }
                listener.onLog("[GIT] " + line, isErr);

                if (line.contains("Receiving objects:") && line.contains("%")) {
                    try {
                        int pctIdx = line.indexOf('%');
                        int spaceIdx = line.lastIndexOf(' ', pctIdx);
                        if (spaceIdx != -1) {
                            int pct = Integer.parseInt(line.substring(spaceIdx + 1, pctIdx).trim());
                            double mapped = 0.08 + (pct / 100.0) * 0.22;
                            listener.onProgress(mapped, "Cloning OSIRIS repository (" + pct + "%)...");
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        int exit = p.waitFor();
        if (exit != 0) {
            String errorDetail;
            if (!errorLines.isEmpty()) {
                errorDetail = String.join("\n", errorLines);
            } else if (!outputLines.isEmpty()) {
                errorDetail = outputLines.get(outputLines.size() - 1);
            } else {
                errorDetail = "Git process exited with code " + exit;
            }
            throw new IOException("Git clone failed (exit " + exit + "):\n" + errorDetail);
        }
    }

    // =========================================================
    // PURE JAVA JGIT CLONE FALLBACK
    // =========================================================
    private void cloneWithJGit(OsirisProgressListener listener, Path targetDir) throws Exception {
        listener.onLog("[CLONE] Executing embedded JGit clone from " + config.getRepoUrl() + " into " + targetDir.toAbsolutePath() + "...", false);

        ProgressMonitor monitor = new ProgressMonitor() {
            private int totalTasks = 1;
            private int currentTask = 0;
            private int totalWork = 0;
            private int completedWork = 0;

            @Override
            public void start(int totalTasks) {
                this.totalTasks = Math.max(1, totalTasks);
            }

            @Override
            public void beginTask(String title, int totalWork) {
                this.currentTask++;
                this.totalWork = totalWork;
                this.completedWork = 0;
                listener.onLog("[JGIT] Task " + currentTask + "/" + totalTasks + ": " + title, false);
            }

            @Override
            public void update(int completed) {
                completedWork += completed;
                if (totalWork > 0) {
                    double taskFraction = (double) completedWork / totalWork;
                    double overallFraction = ((currentTask - 1) + taskFraction) / totalTasks;
                    double mapped = 0.08 + (overallFraction * 0.22);
                    listener.onProgress(mapped, "Cloning via JGit (" + (int) (taskFraction * 100) + "%)...");
                }
            }

            @Override
            public void endTask() {
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }

            @Override
            public void showDuration(boolean enabled) {
            }
        };

        try (Git git = Git.cloneRepository()
                .setURI(config.getRepoUrl())
                .setDirectory(targetDir.toFile())
                .setProgressMonitor(monitor)
                .call()) {
            listener.onLog("[JGIT] Clone finished successfully at HEAD " + git.getRepository().getBranch(), false);
        } catch (Exception e) {
            throw new IOException("JGit clone failed: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }
    }

    // =========================================================
    // RUN PROCESS & STREAM LOGS WITH ERROR CAPTURE
    // =========================================================
    private void runProcessStream(
            Path workDir,
            OsirisProgressListener listener,
            double startPct,
            double endPct,
            List<String> command
    ) throws Exception {
        listener.onLog("[EXEC] " + String.join(" ", command) + " (in " + workDir + ")", false);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        // Inject node and npm parent directories into PATH so child npm scripts can find node/npm
        Map<String, String> env = pb.environment();
        String existingPath = env.getOrDefault("PATH", "");
        Set<String> prefixes = new java.util.LinkedHashSet<>();

        if (runtimeManager.getNodeExecutable() != null) {
            Path nodeParent = Paths.get(runtimeManager.getNodeExecutable()).getParent();
            if (nodeParent != null) prefixes.add(nodeParent.toAbsolutePath().toString());
        }
        if (runtimeManager.getNpmExecutable() != null) {
            Path npmParent = Paths.get(runtimeManager.getNpmExecutable()).getParent();
            if (npmParent != null) prefixes.add(npmParent.toAbsolutePath().toString());
        }
        if (!prefixes.isEmpty()) {
            env.put("PATH", String.join(File.pathSeparator, prefixes) + File.pathSeparator + existingPath);
        }

        Process p = pb.start();
        List<String> lastLines = new ArrayList<>();
        List<String> errorLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            long lastProgressUpdate = System.currentTimeMillis();
            double curPct = startPct;

            while ((line = reader.readLine()) != null) {
                if (lastLines.size() >= 15) lastLines.remove(0);
                lastLines.add(line);

                boolean isErr = line.toLowerCase().contains("npm error") || line.toLowerCase().contains("failed to compile");
                if (isErr) errorLines.add(line);

                listener.onLog(line, isErr);

                // Smooth progress creep during long compiles
                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate > 1500) {
                    if (curPct < endPct - 0.02) {
                        curPct += 0.01;
                        listener.onProgress(curPct, "Processing: " + abbreviate(line, 45));
                    }
                    lastProgressUpdate = now;
                }

                if (cancelled.get()) {
                    p.destroyForcibly();
                    throw new InterruptedException("Setup cancelled by user.");
                }
            }
        }

        int exit = p.waitFor();
        if (exit != 0) {
            String errDetail = !errorLines.isEmpty() ? String.join("\n", errorLines) : String.join("\n", lastLines);
            throw new IOException("Process failed with exit code " + exit + ":\n" + errDetail);
        }
    }

    // =========================================================
    // NEXT.JS STANDALONE ASSET COPY
    // =========================================================
    private void postBuildStandaloneAssetCopy(Path appDir, OsirisProgressListener listener) {
        Path standaloneDir = appDir.resolve(".next").resolve("standalone");
        if (!Files.isDirectory(standaloneDir)) {
            return;
        }

        try {
            // 1. Copy .next/static -> .next/standalone/.next/static
            Path staticSrc = appDir.resolve(".next").resolve("static");
            Path staticDst = standaloneDir.resolve(".next").resolve("static");
            if (Files.isDirectory(staticSrc)) {
                copyDirectory(staticSrc, staticDst);
                listener.onLog("[BUILD] Synchronized .next/static to standalone bundle.", false);
            }

            // 2. Copy public -> .next/standalone/public
            Path publicSrc = appDir.resolve("public");
            Path publicDst = standaloneDir.resolve("public");
            if (Files.isDirectory(publicSrc)) {
                copyDirectory(publicSrc, publicDst);
                listener.onLog("[BUILD] Synchronized public assets to standalone bundle.", false);
            }
        } catch (Exception e) {
            listener.onLog("[WARN] Standalone asset sync notice: " + e.getMessage(), false);
        }
    }

    // =========================================================
    // WRITE SETUP MARKER
    // =========================================================
    private void writeSetupMarker(Path appDir) {
        Path marker = config.getMarkerFile();
        Properties props = new Properties();
        props.setProperty("installed_at", String.valueOf(System.currentTimeMillis()));
        props.setProperty("date", new Date().toString());
        props.setProperty("node_version", runtimeManager.getNodeVersion());
        props.setProperty("os", System.getProperty("os.name"));
        props.setProperty("arch", System.getProperty("os.arch"));

        try (OutputStream out = Files.newOutputStream(marker)) {
            props.store(out, "OMEN-X OSIRIS Setup Completion Marker");
        } catch (IOException ignored) {
        }
    }

    // =========================================================
    // CLEAN AND REINSTALL
    // =========================================================
    public void cleanAndReinstall(OsirisProgressListener listener) throws Exception {
        listener.onLog("[MAINTENANCE] Preparing clean reinstall of OSIRIS...", false);
        stopActiveProcesses(listener);

        Path appDir = config.getAppDir();

        // Wipe node_modules first — it's the most likely to be corrupt/locked and largest
        Path nodeModules = appDir.resolve("node_modules");
        if (Files.isDirectory(nodeModules)) {
            listener.onLog("[MAINTENANCE] Wiping corrupt node_modules (" + nodeModules + ")...", false);
            wipeDirectory(nodeModules, listener);
            if (Files.isDirectory(nodeModules) && !isDirectoryEmpty(nodeModules)) {
                listener.onLog("[MAINTENANCE] node_modules still locked; proceeding to full directory wipe.", false);
            }
        }

        // Wipe the whole app directory (includes .git, .next, etc.)
        listener.onLog("[MAINTENANCE] Wiping OSIRIS app directory (" + appDir + ")...", false);
        wipeDirectory(appDir, listener);

        // Delete the setup marker so isSetupValid() returns false
        Path marker = config.getMarkerFile();
        if (Files.exists(marker)) {
            try { Files.deleteIfExists(marker); } catch (Exception ignored) {}
        }

        listener.onLog("[MAINTENANCE] Clean complete. Starting fresh install pipeline...", false);
        runSetup(listener);
    }

    // =========================================================
    // STOP RUNNING PROCESSES
    // =========================================================
    private void stopActiveProcesses(OsirisProgressListener listener) {
        if (processStopper != null) {
            try {
                processStopper.run();
            } catch (Exception ignored) {
            }
        }
        if (OsirisRuntimeManager.isWindows()) {
            // Kill any hanging git, node, or npm processes that might lock repo/node_modules
            for (String proc : new String[]{"git.exe", "node.exe"}) {
                try {
                    new ProcessBuilder("taskkill", "/F", "/IM", proc)
                            .start().waitFor(2, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
            // Brief settle time after kills to allow OS to release file handles
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
    }

    // =========================================================
    // ROBUST DIRECTORY WIPING (Windows Read-Only & Lock Hardened)
    // =========================================================
    public static boolean wipeDirectory(Path path, OsirisProgressListener listener) {
        if (!Files.exists(path)) {
            return true;
        }

        for (int attempt = 1; attempt <= 4; attempt++) {
            // 1. On Windows: strip read-only, hidden, system flags using attrib
            if (OsirisRuntimeManager.isWindows()) {
                try {
                    new ProcessBuilder("cmd.exe", "/c", "attrib", "-r", "-h", "-s", path.toAbsolutePath() + "\\*.*", "/s", "/d")
                            .start().waitFor(3, TimeUnit.SECONDS);
                    new ProcessBuilder("cmd.exe", "/c", "attrib", "-r", "-h", "-s", path.toAbsolutePath().toString())
                            .start().waitFor(1, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }

            // 2. Walk file tree bottom-up and delete each file
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        clearAttributes(file);
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            file.toFile().setWritable(true, false);
                            Files.deleteIfExists(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        clearAttributes(dir);
                        try {
                            Files.deleteIfExists(dir);
                        } catch (IOException e) {
                            dir.toFile().setWritable(true, false);
                            Files.deleteIfExists(dir);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception ignored) {
            }

            if (!Files.exists(path) || isDirectoryEmpty(path)) {
                if (Files.exists(path)) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }

            // 3. On Windows: fallback to cmd rd /s /q
            if (OsirisRuntimeManager.isWindows()) {
                try {
                    Process p = new ProcessBuilder("cmd.exe", "/c", "rd", "/s", "/q", "\"" + path.toAbsolutePath() + "\"").start();
                    p.waitFor(3, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }

            if (!Files.exists(path) || isDirectoryEmpty(path)) {
                return true;
            }

            // Backoff pause
            try {
                Thread.sleep(attempt * 250L);
            } catch (InterruptedException ignored) {
            }
        }

        return !Files.exists(path) || isDirectoryEmpty(path);
    }

    private static void clearAttributes(Path p) {
        try {
            File f = p.toFile();
            if (!f.canWrite()) {
                f.setWritable(true, false);
            }
            if (OsirisRuntimeManager.isWindows()) {
                try {
                    Files.setAttribute(p, "dos:readonly", false);
                } catch (Exception ignored) {
                }
                try {
                    Files.setAttribute(p, "dos:hidden", false);
                } catch (Exception ignored) {
                }
                try {
                    Files.setAttribute(p, "dos:system", false);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isDirectoryEmpty(Path path) {
        if (!Files.exists(path)) return true;
        if (!Files.isDirectory(path)) return false;
        try (var s = Files.list(path)) {
            return !s.findAny().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // REPLACE DIRECTORY WITH STAGED CLONE
    // =========================================================
    private void replaceDirectoryWithStaged(Path stagedDir, Path primaryDir, OsirisProgressListener listener) {
        try {
            if (Files.exists(primaryDir)) {
                Path oldDir = primaryDir.getParent().resolve("app-old-" + System.currentTimeMillis());
                try {
                    Files.move(primaryDir, oldDir, StandardCopyOption.REPLACE_EXISTING);
                    CompletableFuture.runAsync(() -> wipeDirectory(oldDir, null));
                } catch (Exception moveEx) {
                    wipeDirectory(primaryDir, listener);
                }
            }

            if (!Files.exists(primaryDir)) {
                try {
                    Files.move(stagedDir, primaryDir, StandardCopyOption.ATOMIC_MOVE);
                    return;
                } catch (Exception ignored) {
                }
            }

            // Fallback: copy recursively and wipe stagedDir
            copyDirectory(stagedDir, primaryDir);
            wipeDirectory(stagedDir, null);
        } catch (Exception e) {
            if (listener != null) {
                listener.onLog("[WARN] Notice replacing staging directory: " + e.getMessage(), false);
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            for (Path src : stream.toList()) {
                Path relative = source.relativize(src);
                Path dest = target.resolve(relative);
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private String abbreviate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}
