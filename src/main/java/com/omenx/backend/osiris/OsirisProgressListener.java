package com.omenx.backend.osiris;

// =========================================================
// OSIRIS PROGRESS LISTENER & STATE ENUMS
// Decoupled callbacks for staged progress, logs, and state
// =========================================================
public interface OsirisProgressListener {

    enum OsirisStage {
        CHECK_RUNTIME(1, "CHECK RUNTIME", "Verifying Node.js and Git environment"),
        CLONE_REPOSITORY(2, "CLONE REPOSITORY", "Fetching OSIRIS repository from GitHub"),
        INSTALL_DEPENDENCIES(3, "INSTALL DEPENDENCIES", "Running npm install (may take a few minutes)"),
        BUILD_PRODUCTION(4, "BUILD PRODUCTION", "Compiling Next.js production bundle"),
        START_SERVER(5, "START SERVER", "Launching local server & probing health endpoint"),
        READY(6, "SYSTEM READY", "OSIRIS is online and responding");

        private final int stepNumber;
        private final String title;
        private final String description;

        OsirisStage(int stepNumber, String title, String description) {
            this.stepNumber = stepNumber;
            this.title = title;
            this.description = description;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    enum OsirisState {
        NOT_INSTALLED,
        INSTALLING,
        READY_CACHED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        FAILED
    }

    void onStageChanged(OsirisStage stage, String stageTitle, String stageDesc);

    void onProgress(double percent, String message);

    void onLog(String logLine, boolean isError);

    void onStateChanged(OsirisState newState);

    void onSuccess(String targetUrl);

    void onError(String errorMessage, Throwable error);
}
