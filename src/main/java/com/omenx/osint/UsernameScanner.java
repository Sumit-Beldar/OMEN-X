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
        // SOCIAL PLATFORMS
        // =========================

        check(
                results,
                "GitHub",
                "https://github.com/" + username
        );

        check(
                results,
                "Reddit",
                "https://www.reddit.com/user/" + username
        );

        check(
                results,
                "GitLab",
                "https://gitlab.com/" + username
        );

        check(
                results,
                "Instagram",
                "https://www.instagram.com/" + username + "/"
        );

        check(
                results,
                "Facebook",
                "https://www.facebook.com/" + username
        );

        check(
                results,
                "Pinterest",
                "https://www.pinterest.com/" + username + "/"
        );

        check(
                results,
                "Twitch",
                "https://www.twitch.tv/" + username
        );

        check(
                results,
                "X",
                "https://x.com/" + username
        );

        return results;
    }

    // =========================
    // CHECK PLATFORM
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
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                    + "AppleWebKit/537.36 "
                                    + "(KHTML, like Gecko) "
                                    + "Chrome/140.0 Safari/537.36"
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int status = response.statusCode();

            String body = response.body();

            // ==================================
            // 404 = PAGE DOES NOT EXIST
            // ==================================

            if (status == 404) {

                results.put(
                        platform,
                        "NOT FOUND"
                );

                return;
            }

            // ==================================
            // BLOCKED / RATE LIMITED
            // ==================================

            if (status == 401 ||
                status == 403 ||
                status == 429) {

                results.put(
                        platform,
                        "UNKNOWN - BLOCKED (" + status + ")"
                );

                return;
            }

            // ==================================
            // PLATFORM-SPECIFIC CHECKS
            // ==================================

            switch (platform) {

                case "Twitch":

                    checkTwitch(
                            results,
                            platform,
                            url,
                            status,
                            body
                    );

                    break;

                case "Facebook":

                    checkFacebook(
                            results,
                            platform,
                            url,
                            status,
                            body
                    );

                    break;

                case "Instagram":

                    checkInstagram(
                            results,
                            platform,
                            url,
                            status,
                            body
                    );

                    break;

                default:

                    checkGeneric(
                            results,
                            platform,
                            url,
                            status,
                            body
                    );

                    break;
            }

        } catch (IOException e) {

            results.put(
                    platform,
                    "UNKNOWN - CONNECTION ERROR"
            );

        } catch (InterruptedException e) {

            results.put(
                    platform,
                    "UNKNOWN - INTERRUPTED"
            );

            Thread.currentThread().interrupt();
        }
    }

    // =========================================
    // TWITCH
    // =========================================

    private void checkTwitch(
            Map<String, String> results,
            String platform,
            String url,
            int status,
            String body) {

        String text = body.toLowerCase();

        /*
         * Twitch can return HTTP 200 even when the
         * requested channel does not exist.
         */

        if (text.contains("unless you've got a time machine")
                || text.contains("channel is unavailable")
                || text.contains("content is unavailable")) {

            results.put(
                    platform,
                    "NOT FOUND"
            );

        } else if (status >= 200 && status < 300) {

            results.put(
                    platform,
                    "FOUND | " + url
            );

        } else {

            results.put(
                    platform,
                    "UNKNOWN (" + status + ")"
            );
        }
    }

    // =========================================
    // FACEBOOK
    // =========================================

    private void checkFacebook(
            Map<String, String> results,
            String platform,
            String url,
            int status,
            String body) {

        String text = body.toLowerCase();

        /*
         * Facebook frequently returns 400/403/login
         * pages for automated requests.
         *
         * Therefore we NEVER call those responses FOUND.
         */

        if (status == 400 ||
            status == 401 ||
            status == 403 ||
            status == 429) {

            results.put(
                    platform,
                    "UNKNOWN - FACEBOOK DID NOT ALLOW VERIFICATION (" +
                    status +
                    ")"
            );

            return;
        }

        if (status >= 200 && status < 300) {

            if (text.contains("page isn't available")
                    || text.contains("this page isn't available")
                    || text.contains("content isn't available")) {

                results.put(
                        platform,
                        "NOT FOUND"
                );

            } else {

                results.put(
                        platform,
                        "FOUND | " + url
                );
            }

        } else {

            results.put(
                    platform,
                    "UNKNOWN (" + status + ")"
            );
        }
    }

    // =========================================
    // INSTAGRAM
    // =========================================

    private void checkInstagram(
            Map<String, String> results,
            String platform,
            String url,
            int status,
            String body) {

        String text = body.toLowerCase();

        /*
         * Instagram may return 200 for login/challenge
         * pages. Therefore HTTP 200 alone is NOT enough.
         */

        if (text.contains("sorry, this page isn't available")
                || text.contains("page isn't available")
                || text.contains("the link you followed may be broken")) {

            results.put(
                    platform,
                    "NOT FOUND"
            );

            return;
        }

        if (text.contains("instagram")) {

            results.put(
                    platform,
                    "FOUND | " + url
            );

        } else {

            results.put(
                    platform,
                    "UNKNOWN - COULD NOT VERIFY"
            );
        }
    }

    // =========================================
    // GENERIC PLATFORMS
    // =========================================

    private void checkGeneric(
            Map<String, String> results,
            String platform,
            String url,
            int status,
            String body) {

        if (status >= 200 && status < 300) {

            results.put(
                    platform,
                    "FOUND | " + url
            );

        } else {

            results.put(
                    platform,
                    "UNKNOWN (" + status + ")"
            );
        }
    }
}