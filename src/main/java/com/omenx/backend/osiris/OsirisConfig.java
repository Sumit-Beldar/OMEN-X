package com.omenx.backend.osiris;

import java.io.*;
import java.nio.file.*;
import java.util.*;

// =========================================================
// OSIRIS CONFIG & STORAGE MANAGER
// Manages directories, properties, API keys, and .env.local
// =========================================================
public class OsirisConfig {

    private static final String REPO_URL = "https://github.com/simplifaisoul/osiris.git";
    private static final int DEFAULT_PORT = 3000;

    private final Path baseDir;
    private final Path appDir;
    private final Path markerFile;
    private final Path configFile;

    private int port = DEFAULT_PORT;
    private String openskyClientId = "";
    private String openskyClientSecret = "";
    private String n2yoApiKey = "";
    private String firmsApiKey = "";
    private String aisApiKey = "";
    private String customNodePath = "";
    private String customGitPath = "";

    public OsirisConfig() {
        String userHome = System.getProperty("user.home", ".");
        this.baseDir = Paths.get(userHome, ".omenx", "tools", "osiris");
        this.appDir = baseDir.resolve("app");
        this.markerFile = appDir.resolve(".osiris-setup-complete");
        this.configFile = Paths.get(userHome, ".omenx", "config", "osiris.properties");

        try {
            Files.createDirectories(baseDir);
            Files.createDirectories(configFile.getParent());
        } catch (IOException ignored) {
        }

        load();
    }

    public String getRepoUrl() {
        return REPO_URL;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getAppDir() {
        return appDir;
    }

    public Path getMarkerFile() {
        return markerFile;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = (port > 1024 && port <= 65535) ? port : DEFAULT_PORT;
    }

    public String getOpenskyClientId() {
        return openskyClientId;
    }

    public void setOpenskyClientId(String openskyClientId) {
        this.openskyClientId = (openskyClientId != null) ? openskyClientId.trim() : "";
    }

    public String getOpenskyClientSecret() {
        return openskyClientSecret;
    }

    public void setOpenskyClientSecret(String openskyClientSecret) {
        this.openskyClientSecret = (openskyClientSecret != null) ? openskyClientSecret.trim() : "";
    }

    public String getN2yoApiKey() {
        return n2yoApiKey;
    }

    public void setN2yoApiKey(String n2yoApiKey) {
        this.n2yoApiKey = (n2yoApiKey != null) ? n2yoApiKey.trim() : "";
    }

    public String getFirmsApiKey() {
        return firmsApiKey;
    }

    public void setFirmsApiKey(String firmsApiKey) {
        this.firmsApiKey = (firmsApiKey != null) ? firmsApiKey.trim() : "";
    }

    public String getAisApiKey() {
        return aisApiKey;
    }

    public void setAisApiKey(String aisApiKey) {
        this.aisApiKey = (aisApiKey != null) ? aisApiKey.trim() : "";
    }

    public String getCustomNodePath() {
        return customNodePath;
    }

    public void setCustomNodePath(String customNodePath) {
        this.customNodePath = (customNodePath != null) ? customNodePath.trim() : "";
    }

    public String getCustomGitPath() {
        return customGitPath;
    }

    public void setCustomGitPath(String customGitPath) {
        this.customGitPath = (customGitPath != null) ? customGitPath.trim() : "";
    }

    // =========================================================
    // LOAD PROPERTIES
    // =========================================================
    public synchronized void load() {
        if (!Files.exists(configFile)) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
            this.port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
            this.openskyClientId = props.getProperty("opensky_client_id", "");
            this.openskyClientSecret = props.getProperty("opensky_client_secret", "");
            this.n2yoApiKey = props.getProperty("n2yo_api_key", "");
            this.firmsApiKey = props.getProperty("firms_api_key", "");
            this.aisApiKey = props.getProperty("ais_api_key", "");
            this.customNodePath = props.getProperty("custom_node_path", "");
            this.customGitPath = props.getProperty("custom_git_path", "");
        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SAVE PROPERTIES
    // =========================================================
    public synchronized void save() {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(port));
        props.setProperty("opensky_client_id", openskyClientId);
        props.setProperty("opensky_client_secret", openskyClientSecret);
        props.setProperty("n2yo_api_key", n2yoApiKey);
        props.setProperty("firms_api_key", firmsApiKey);
        props.setProperty("ais_api_key", aisApiKey);
        props.setProperty("custom_node_path", customNodePath);
        props.setProperty("custom_git_path", customGitPath);

        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "OMEN-X OSIRIS Configuration");
            }
        } catch (IOException e) {
            System.err.println("[OSIRIS Config] Failed to save config: " + e.getMessage());
        }

        syncEnvLocal();
    }

    // =========================================================
    // SYNC .env.local INSIDE APP DIRECTORY
    // =========================================================
    public synchronized void syncEnvLocal() {
        if (!Files.isDirectory(appDir)) {
            return;
        }

        Path envFile = appDir.resolve(".env.local");
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated by OMEN-X for OSIRIS\n");
        sb.append("PORT=").append(port).append("\n");
        if (!openskyClientId.isBlank()) sb.append("OPENSKY_CLIENT_ID=").append(openskyClientId).append("\n");
        if (!openskyClientSecret.isBlank()) sb.append("OPENSKY_CLIENT_SECRET=").append(openskyClientSecret).append("\n");
        if (!n2yoApiKey.isBlank()) sb.append("N2YO_API_KEY=").append(n2yoApiKey).append("\n");
        if (!firmsApiKey.isBlank()) sb.append("FIRMS_API_KEY=").append(firmsApiKey).append("\n");
        if (!aisApiKey.isBlank()) sb.append("AIS_API_KEY=").append(aisApiKey).append("\n");

        try {
            Files.writeString(envFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    // =========================================================
    // GET ENVIRONMENT VARIABLES MAP FOR PROCESSBUILDER
    // =========================================================
    public Map<String, String> getEnvironmentMap(int targetPort) {
        Map<String, String> map = new HashMap<>();
        map.put("PORT", String.valueOf(targetPort));
        map.put("NODE_ENV", "production");
        if (!openskyClientId.isBlank()) map.put("OPENSKY_CLIENT_ID", openskyClientId);
        if (!openskyClientSecret.isBlank()) map.put("OPENSKY_CLIENT_SECRET", openskyClientSecret);
        if (!n2yoApiKey.isBlank()) map.put("N2YO_API_KEY", n2yoApiKey);
        if (!firmsApiKey.isBlank()) map.put("FIRMS_API_KEY", firmsApiKey);
        if (!aisApiKey.isBlank()) map.put("AIS_API_KEY", aisApiKey);
        return map;
    }
}
