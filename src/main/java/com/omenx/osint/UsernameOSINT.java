package com.omenx.osint;

public class UsernameOSINT {

    public static String investigate(String username) {

        StringBuilder result = new StringBuilder();

        result.append("USERNAME OSINT INVESTIGATION\n");
        result.append("============================\n\n");

        result.append("Target: ").append(username).append("\n\n");

        result.append("PUBLIC PROFILE CHECKS\n");
        result.append("---------------------\n");

        result.append("GitHub: https://github.com/")
              .append(username).append("\n");

        result.append("Reddit: https://www.reddit.com/user/")
              .append(username).append("\n");

        result.append("Instagram: https://www.instagram.com/")
              .append(username).append("/\n");

        result.append("X: https://x.com/")
              .append(username).append("\n");

        result.append("\nSTATUS\n");
        result.append("------\n");
        result.append("Investigation completed.\n");

        return result.toString();
    }
}