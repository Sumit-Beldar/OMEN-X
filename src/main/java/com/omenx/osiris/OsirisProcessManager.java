package com.omenx.osiris;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// =========================================================
// OSIRIS PROCESS MANAGER
// Lifecycle supervision, port probing, health checks,
// background execution, tree termination, and auto-browser.
// =========================================================
public class OsirisProcessManager {

    private final OsirisConfig config;
    private final OsirisRuntimeManager runtimeManager;
    private final OsirisInstaller installer;

    private Process currentProcess;
    private int activePort = 3000;
    private volatile OsirisProgressListener.OsirisState currentState = OsirisProgressListener.OsirisState.NOT_INSTALLED;

    private final List<String> logBuffer = Collections.synchronizedList(new LinkedList<>());
    private static final int MAX_LOG_LINES = 1500;

    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "osiris-watchdog");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private Thread shutdownHook;

    public OsirisProcessManager(OsirisConfig config, OsirisRuntimeManager runtimeManager, OsirisInstaller installer) {
        this.config = config;
        this.runtimeManager = runtimeManager;
        this.installer = installer;
        this.installer.setProcessStopper(this::stopSync);

        // Register JVM shutdown hook
        this.shutdownHook = new Thread(this::stopSync, "osiris-shutdown-hook");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (Exception ignored) {
        }

        // Start background process watchdog
        watchdog.scheduleWithFixedDelay(this::checkProcessHealth, 2, 3, TimeUnit.SECONDS);

        // Initial state assessment
        if (installer.isSetupValid()) {
            currentState = OsirisProgressListener.OsirisState.READY_CACHED;
        } else {
            currentState = OsirisProgressListener.OsirisState.NOT_INSTALLED;
        }
    }

    public OsirisProgressListener.OsirisState getCurrentState() {
        return currentState;
    }

    public int getActivePort() {
        return activePort;
    }

    public List<String> getLogBuffer() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    public void appendLog(String line) {
        synchronized (logBuffer) {
            if (logBuffer.size() >= MAX_LOG_LINES) {
                logBuffer.remove(0);
            }
            logBuffer.add(line);
        }
    }

    public void clearLogs() {
        synchronized (logBuffer) {
            logBuffer.clear();
        }
    }

    // =========================================================
    // LAUNCH OSIRIS (Async)
    // =========================================================
    public void launch(OsirisProgressListener listener) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Check if already running on configured port
                int configuredPort = config.getPort();
                if (isPortServingOsiris(configuredPort)) {
                    listener.onLog("[PROBE] OSIRIS is already serving on port " + configuredPort + ". Opening browser...", false);
                    activePort = configuredPort;
                    currentState = OsirisProgressListener.OsirisState.RUNNING;
                    listener.onStateChanged(currentState);
                    String url = "http://localhost:" + activePort;
                    listener.onSuccess(url);
                    openBrowser(url, listener);
                    return;
                }

                // 2. Setup / Cache Validation
                boolean isCached = installer.isSetupValid();
                if (!isCached) {
                    listener.onLog("[SETUP] Cached setup not found. Executing 5-stage setup pipeline...", false);
                    installer.runSetup(listener);
                } else {
                    listener.onLog("[CACHE] Valid OSIRIS installation detected. Skipping clone & build.", false);
                    listener.onStageChanged(
                            OsirisProgressListener.OsirisStage.START_SERVER,
                            "STARTING OSIRIS",
                            "Booting cached production build..."
                    );
                    listener.onProgress(0.25, "Booting cached production server...");
                }

                // 3. Port Allocation
                activePort = resolveAvailablePort(configuredPort, listener);
                listener.onLog("[PORT] Allocated port " + activePort + " for OSIRIS server.", false);

                // 4. Spawn Server Process
                currentState = OsirisProgressListener.OsirisState.STARTING;
                listener.onStateChanged(currentState);
                listener.onProgress(0.60, "Spawning Next.js production server...");

                spawnServerProcess(activePort, listener);

                // 5. Poll Health Endpoint
                listener.onProgress(0.85, "Probing http://localhost:" + activePort + " for HTTP 200...");
                boolean healthy = pollHealth(activePort, 22, listener);

                if (healthy) {
                    currentState = OsirisProgressListener.OsirisState.RUNNING;
                    listener.onStateChanged(currentState);
                    listener.onProgress(1.0, "OSIRIS is online!");
                    String targetUrl = "http://localhost:" + activePort;
                    listener.onSuccess(targetUrl);
                    listener.onLog("[SERVER] OSIRIS server is online and responding at " + targetUrl, false);
                    openBrowser(targetUrl, listener);
                } else {
                    throw new IOException("OSIRIS server failed to respond within timeout window.");
                }

            } catch (Exception e) {
                currentState = OsirisProgressListener.OsirisState.FAILED;
                listener.onStateChanged(currentState);
                listener.onError(e.getMessage() != null ? e.getMessage() : "Unknown execution error", e);
                appendLog("[ERROR] " + e.getMessage());
            }
        });
    }

    // =========================================================
    // SPAWN SERVER PROCESS
    // =========================================================
    private void spawnServerProcess(int targetPort, OsirisProgressListener listener) throws Exception {
        Path appDir = config.getAppDir();
        Path standaloneServer = appDir.resolve(".next").resolve("standalone").resolve("server.js");

        List<String> command = new ArrayList<>();
        if (Files.isRegularFile(standaloneServer)) {
            listener.onLog("[START] Detected Next.js standalone build. Launching standalone server.js...", false);
            command.add(runtimeManager.getNodeExecutable());
            command.add(".next/standalone/server.js");
        } else {
            listener.onLog("[START] Launching via npm start -- -p " + targetPort + "...", false);
            command.addAll(runtimeManager.buildCommand(runtimeManager.getNpmExecutable(), "start", "--", "-p", String.valueOf(targetPort)));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(appDir.toFile());

        // Configure environment
        Map<String, String> env = pb.environment();
        env.putAll(config.getEnvironmentMap(targetPort));

        // Inject node binary directory to PATH
        if (runtimeManager.getNodeExecutable() != null && !runtimeManager.getNodeExecutable().equals("node")) {
            Path nodeParent = Paths.get(runtimeManager.getNodeExecutable()).getParent();
            if (nodeParent != null) {
                String existingPath = env.getOrDefault("PATH", "");
                env.put("PATH", nodeParent.toAbsolutePath() + File.pathSeparator + existingPath);
            }
        }

        listener.onLog("[EXEC] " + String.join(" ", command) + " (PORT=" + targetPort + ")", false);

        currentProcess = pb.start();

        // Stream stdout
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(line);
                    listener.onLog(line, false);
                }
            } catch (Exception ignored) {
            }
        });

        // Stream stderr
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog("[ERR] " + line);
                    listener.onLog(line, true);
                }
            } catch (Exception ignored) {
            }
        });
    }

    // =========================================================
    // PORT RESOLUTION & PROBING
    // =========================================================
    private int resolveAvailablePort(int preferredPort, OsirisProgressListener listener) {
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }

        listener.onLog("[PORT] Preferred port " + preferredPort + " is in use. Searching for next free port...", false);
        for (int p = preferredPort + 1; p <= preferredPort + 30; p++) {
            if (isPortAvailable(p)) {
                return p;
            }
        }

        // Ephemeral fallback
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            return preferredPort;
        }
    }

    public boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // =========================================================
    // PROBE IF PORT IS SERVING OSIRIS
    // =========================================================
    public boolean isPortServingOsiris(int port) {
        try {
            URI uri = new URI("http://127.0.0.1:" + port + "/");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(800);
            conn.setReadTimeout(1200);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "OMEN-X-Probe");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                // Check body or headers for OSIRIS / Next.js signatures
                String xPowered = conn.getHeaderField("x-powered-by");
                if (xPowered != null && xPowered.toLowerCase().contains("next")) {
                    return true;
                }

                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder snippet = new StringBuilder();
                    String line;
                    int linesRead = 0;
                    while ((line = r.readLine()) != null && linesRead++ < 40) {
                        snippet.append(line).append(" ");
                    }
                    String body = snippet.toString().toLowerCase();
                    if (body.contains("osiris") || body.contains("global") || body.contains("__next")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    // =========================================================
    // POLL HEALTH ENDPOINT
    // =========================================================
    private boolean pollHealth(int port, int timeoutSeconds, OsirisProgressListener listener) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            if (currentProcess != null && !currentProcess.isAlive()) {
                listener.onLog("[SERVER] Server process terminated prematurely (exit: " + currentProcess.exitValue() + ")", true);
                return false;
            }

            try {
                URI uri = new URI("http://127.0.0.1:" + port + "/");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setConnectTimeout(600);
                conn.setReadTimeout(800);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "OMEN-X-HealthCheck");

                int code = conn.getResponseCode();
                if (code >= 200 && code < 400) {
                    return true;
                }
            } catch (Exception ignored) {
            }

            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    // =========================================================
    // OPEN DEFAULT BROWSER
    // =========================================================
    public void openBrowser(String url, OsirisProgressListener listener) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
        }

        // Cross-platform CLI fallback
        try {
            if (OsirisRuntimeManager.isWindows()) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (OsirisRuntimeManager.isMac()) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onLog("[WARN] Could not automatically launch web browser: " + e.getMessage(), true);
            }
        }
    }

    // =========================================================
    // PROCESS TERMINATION & RESILIENCE
    // =========================================================
    public synchronized void stop() {
        CompletableFuture.runAsync(this::stopSync);
    }

    public synchronized void stopSync() {
        if (currentProcess == null || !currentProcess.isAlive()) {
            currentState = OsirisProgressListener.OsirisState.STOPPED;
            return;
        }

        currentState = OsirisProgressListener.OsirisState.STOPPING;
        appendLog("[PROCESS] Terminating OSIRIS server process tree...");

        long pid = -1;
        try {
            pid = currentProcess.pid();
        } catch (Exception ignored) {
        }

        // Destroy child processes first
        try {
            currentProcess.toHandle().descendants().forEach(h -> {
                try {
                    h.destroyForcibly();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }

        // Standard destroy
        currentProcess.destroy();

        try {
            boolean exited = currentProcess.waitFor(2000, TimeUnit.MILLISECONDS);
            if (!exited) {
                currentProcess.destroyForcibly();
            }
        } catch (InterruptedException ignored) {
            currentProcess.destroyForcibly();
        }

        // Windows taskkill fallback to guarantee no orphan node processes
        if (OsirisRuntimeManager.isWindows() && pid > 0) {
            try {
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid));
                pb.start().waitFor(1500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }
        }

        currentState = OsirisProgressListener.OsirisState.STOPPED;
        appendLog("[PROCESS] OSIRIS server terminated cleanly.");
    }

    // =========================================================
    // BACKGROUND PROCESS WATCHDOG
    // =========================================================
    private void checkProcessHealth() {
        if (currentState == OsirisProgressListener.OsirisState.RUNNING) {
            if (currentProcess != null && !currentProcess.isAlive()) {
                int exitCode = currentProcess.exitValue();
                currentState = OsirisProgressListener.OsirisState.FAILED;
                appendLog("[WATCHDOG] OSIRIS server process crashed unexpectedly with exit code " + exitCode);
            }
        }
    }

    public void shutdown() {
        isShuttingDown.set(true);
        try {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        } catch (Exception ignored) {
        }
        watchdog.shutdownNow();
        stopSync();
    }
}
