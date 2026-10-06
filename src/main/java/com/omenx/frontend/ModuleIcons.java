package com.omenx.frontend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry for OMEN-X module icons (emojis).
 * Change an emoji here to update it everywhere across the application
 * (sidebar, module cards, page and module headers).
 */
public final class ModuleIcons {

    public static final Map<String, String> ICONS;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Dashboard",        "🛰️"); // Command Center (was //>)
        map.put("OSIRIS",           "🌍"); // OSIRIS Global OSINT (was [O]>)
        map.put("Username",         "👤"); // Username Recognition (was @>)
        map.put("Email",            "📧"); // Email Exposure (was #>)
        map.put("Domain",           "🌐"); // Domain & DNS (was ::>)
        map.put("IP Address",       "📍"); // IP Geolocation (was []>)
        map.put("Phone",            "📞"); // Phone Intel (was {}>)
        map.put("EXIF Metadata",    "📷"); // EXIF Metadata (was <>)
        map.put("Reverse Image",    "🔍"); // Reverse Image (was >>)
        map.put("Malware Analysis", "🛡️"); // Malware Threat (was !!>)
        ICONS = Collections.unmodifiableMap(map);
    }

    /**
     * Look up module icon by module name or legacy glyph.
     */
    public static String get(String moduleKey) {
        if (moduleKey == null) return "◈";
        String icon = ICONS.get(moduleKey);
        if (icon != null) return icon;
        return switch (moduleKey.trim()) {
            case "//>"  -> ICONS.get("Dashboard");
            case "[O]>" -> ICONS.get("OSIRIS");
            case "@>"   -> ICONS.get("Username");
            case "#>"   -> ICONS.get("Email");
            case "::>"  -> ICONS.get("Domain");
            case "[]>"  -> ICONS.get("IP Address");
            case "{}>"  -> ICONS.get("Phone");
            case "<>"   -> ICONS.get("EXIF Metadata");
            case ">>"   -> ICONS.get("Reverse Image");
            case "!!>"  -> ICONS.get("Malware Analysis");
            default     -> "◈";
        };
    }

    private ModuleIcons() {}
}
