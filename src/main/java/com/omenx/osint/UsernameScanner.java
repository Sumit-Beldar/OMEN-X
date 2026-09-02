package com.omenx.osint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class UsernameScanner {

    private final HttpClient client;

    public UsernameScanner() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Map<String, String> scan(String username) {

        Map<String, String> results = new LinkedHashMap<>();

        username = username.trim();

        if (username.isEmpty()) {
            return results;
        }

        // =========================
        // SOCIAL / PUBLIC PLATFORMS
        // =========================

        check(results,
                "GitHub",
                "https://github.com/" + username);

        check(results,
                "Reddit",
                "https://www.reddit.com/user/" + username);

        check(results,
                "GitLab",
                "https://gitlab.com/" + username);

        check(results,
                "Instagram",
                "https://www.instagram.com/" + username + "/");

        check(results,
                "Facebook",
                "https://www.facebook.com/" + username);

        check(results,
                "Pinterest",
                "https://www.pinterest.com/" + username + "/");

        check(results,
                "Twitch",
                "https://www.twitch.tv/" + username);

        check(results,
                "X",
                "https://x.com/" + username);

        return results;
    }

    // =========================
    // CHECK PUBLIC URL
    // =========================

    private void check(
            Map<String, String> results,
            String platform,
            String url) {

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .header(
                                    "User-Agent",
                                    "Mozilla/5.0"
                            )
                            .GET()
                            .build();

            HttpResponse<Void> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .discarding()
                    );

            int status = response.statusCode();

            // Page exists
            if (status >= 200 && status < 300) {

                results.put(
                        platform,
                        "FOUND | " + url
                );
            }

            // Page definitely doesn't exist
            else if (status == 404) {

                results.put(
                        platform,
                        "NOT FOUND"
                );
            }

            // Website blocked automated request
            else if (
                    status == 401 ||
                    status == 403 ||
                    status == 429
            ) {

                results.put(
                        platform,
                        "BLOCKED / UNKNOWN (" +
                        status +
                        ")"
                );
            }

            // Other response
            else {

                results.put(
                        platform,
                        "UNKNOWN (" +
                        status +
                        ")"
                );
            }

        } catch (IOException e) {

            results.put(
                    platform,
                    "CONNECTION ERROR"
            );

        } catch (InterruptedException e) {

            results.put(
                    platform,
                    "INTERRUPTED"
            );

            Thread.currentThread().interrupt();
        }
    }
}