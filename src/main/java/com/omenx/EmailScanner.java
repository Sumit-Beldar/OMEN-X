package com.omenx.osint;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

public class EmailScanner {

    private final HttpClient client;

    public EmailScanner() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String scan(String email) {
        email = email.trim();

        if (email.isEmpty()) {
            return "INVALID EMAIL\n\nPlease enter an email address.";
        }

        if (!isValidEmail(email)) {
            return "INVALID EMAIL\n\nThe email format is not valid.";
        }

        try {
            String encodedEmail = URLEncoder.encode(
                    email,
                    StandardCharsets.UTF_8
            );

            String url =
                    "https://api.xposedornot.com/v1/check-email/"
                    + encodedEmail;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "OMEN-X-OSINT/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();
            String body = response.body();

            if (status >= 200 && status < 300) {
                return formatResponse(email, body);
            }

            if (status == 404) {
                return buildResult(
                        email,
                        "UNKNOWN",
                        "The API rejected the request or returned no matching record.\n\n"
                        + "HTTP status: 404"
                );
            }

            if (status == 429) {
                return buildResult(
                        email,
                        "RATE LIMITED",
                        "The free API has temporarily limited requests.\n\n"
                        + "Please wait before trying again."
                );
            }

            return buildResult(
                    email,
                    "UNKNOWN",
                    "No breach conclusion was made.\n\n"
                    + "API response code: " + status
            );

        } catch (IOException e) {
            return buildResult(
                    email,
                    "CONNECTION ERROR",
                    "Unable to connect to the breach intelligence service.\n\n"
                    + "No breach conclusion was made."
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return buildResult(
                    email,
                    "SCAN INTERRUPTED",
                    "The operation was interrupted."
            );
        }
    }

    private String formatResponse(String email, String json) {
        String compact = json.replaceAll("\\s+", "");

        if (compact.contains("\"status\":\"success\"")) {
            String breachSection = extractBreaches(json);

            if (!breachSection.isEmpty()) {
                return buildResult(
                        email,
                        "BREACH DATA FOUND",
                        "Known breaches returned by the API:\n\n"
                        + breachSection
                );
            }

            return buildResult(
                    email,
                    "NO BREACH FOUND",
                    "The API returned a successful response with no breach names."
            );
        }

        if (compact.contains("\"Error\":\"Notfound\"")
                || compact.contains("\"Error\":\"Notfound\"".toLowerCase())) {
            return buildResult(
                    email,
                    "NO BREACH FOUND",
                    "The API reported that the email was not found."
            );
        }

        return buildResult(
                email,
                "UNKNOWN",
                "The API responded successfully, but the response did not contain "
                + "a recognizable breach result.\n\n"
                + "Raw API response:\n"
                + json
        );
    }

    private String extractBreaches(String json) {
        int start = json.indexOf("\"breaches\"");
        if (start == -1) {
            return "";
        }

        int open = json.indexOf('[', start);
        if (open == -1) {
            return "";
        }

        int depth = 0;
        int close = -1;

        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    close = i;
                    break;
                }
            }
        }

        if (close == -1) {
            return "";
        }

        String section = json.substring(open + 1, close);

        Pattern quoted = Pattern.compile("\"([^\"]+)\"");
        java.util.regex.Matcher matcher = quoted.matcher(section);

        StringBuilder result = new StringBuilder();
        int number = 1;

        while (matcher.find()) {
            String value = matcher.group(1).trim();

            if (!value.isEmpty()) {
                result.append(number++)
                        .append(". ")
                        .append(value)
                        .append("\n");
            }
        }

        return result.toString();
    }

    private String buildResult(
            String email,
            String status,
            String details) {

        return "===== OMEN-X EMAIL OSINT =====\n\n"
                + "Target:\n"
                + email
                + "\n\n"
                + "BREACH STATUS:\n"
                + status
                + "\n\n"
                + details
                + "\n\n"
                + "===== END OF SCAN =====";
    }

    private boolean isValidEmail(String email) {
        String regex =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        return Pattern.compile(regex)
                .matcher(email)
                .matches();
    }
}