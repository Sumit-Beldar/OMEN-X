package com.omenx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// =========================================================
// REVERSE IMAGE SCANNER
//
// Flow:
//
// Local Image
//      ↓
// imgdb.io temporary upload
//      ↓
// Public image URL
//      ↓
// QuanticData Google Lens API
//      ↓
// Real Internet results
// =========================================================

public class ReverseImageScanner {

    private static final String UPLOAD_URL =
            "https://imgdb.io/api/v1/upload?ttl=3600";

    private static final String QUANTICDATA_URL =
            "https://api.quanticdata.io/v1/scraper/collectors/google_lens/run";

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private String apiKey;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReverseImageScanner() {

        httpClient =
                HttpClient.newBuilder()
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();

        objectMapper =
                new ObjectMapper();

        loadApiKey();
    }

    // =========================================================
    // LOAD API KEY
    // =========================================================

    private void loadApiKey() {

        try {

            Properties properties =
                    new Properties();

            var stream =
                    getClass()
                            .getClassLoader()
                            .getResourceAsStream(
                                    "config.properties"
                            );

            if (stream == null) {

                throw new IOException(
                        "config.properties not found"
                );
            }

            properties.load(stream);

            apiKey =
                    properties.getProperty(
                            "QUANTICDATA_API_KEY"
                    );

            if (apiKey == null ||
                    apiKey.isBlank()) {

                throw new IOException(
                        "QUANTICDATA_API_KEY is missing"
                );
            }

        } catch (Exception e) {

            apiKey = null;

            System.err.println(
                    "Unable to load QuanticData API key: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // RESULT
    // =========================================================

    public static class Match {

        private final String title;
        private final String source;
        private final String domain;
        private final String link;
        private final String image;
        private final String thumbnail;

        public Match(
                String title,
                String source,
                String domain,
                String link,
                String image,
                String thumbnail
        ) {

            this.title = title;
            this.source = source;
            this.domain = domain;
            this.link = link;
            this.image = image;
            this.thumbnail = thumbnail;
        }

        public String getTitle() {
            return title;
        }

        public String getSource() {
            return source;
        }

        public String getDomain() {
            return domain;
        }

        public String getLink() {
            return link;
        }

        public String getImage() {
            return image;
        }

        public String getThumbnail() {
            return thumbnail;
        }
    }

    // =========================================================
    // SCAN
    // =========================================================

    public List<Match> scan(File imageFile)
            throws Exception {

        if (imageFile == null ||
                !imageFile.exists() ||
                !imageFile.isFile()) {

            throw new IOException(
                    "Invalid image file."
            );
        }

        if (apiKey == null ||
                apiKey.isBlank()) {

            throw new IOException(
                    "QuanticData API key is not configured."
            );
        }

        System.out.println(
                "Uploading image..."
        );

        String publicImageUrl =
                uploadImage(imageFile);

        System.out.println(
                "Image URL: "
                        + publicImageUrl
        );

        System.out.println(
                "Searching Internet..."
        );

        return searchGoogleLens(
                publicImageUrl
        );
    }

    // =========================================================
    // UPLOAD TO IMGDB
    // =========================================================

    private String uploadImage(
            File imageFile
    ) throws Exception {

        String boundary =
                "----OmenXBoundary"
                        + System.currentTimeMillis();

        byte[] fileBytes =
                Files.readAllBytes(
                        imageFile.toPath()
                );

        String fileName =
                imageFile.getName();

        String contentType =
                Files.probeContentType(
                        imageFile.toPath()
                );

        if (contentType == null) {

            contentType =
                    "application/octet-stream";
        }

        byte[] header =
                (
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; " +
                        "name=\"file\"; " +
                        "filename=\"" + fileName + "\"\r\n" +
                        "Content-Type: " + contentType + "\r\n\r\n"
                ).getBytes();

        byte[] footer =
                (
                        "\r\n--" +
                        boundary +
                        "--\r\n"
                ).getBytes();

        byte[] body =
                new byte[
                        header.length +
                        fileBytes.length +
                        footer.length
                ];

        System.arraycopy(
                header,
                0,
                body,
                0,
                header.length
        );

        System.arraycopy(
                fileBytes,
                0,
                body,
                header.length,
                fileBytes.length
        );

        System.arraycopy(
                footer,
                0,
                body,
                header.length +
                        fileBytes.length,
                footer.length
        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        UPLOAD_URL
                                )
                        )
                        .header(
                                "Content-Type",
                                "multipart/form-data; boundary="
                                        + boundary
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofByteArray(
                                        body
                                )
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 201) {

            throw new IOException(
                    "Image upload failed. HTTP "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        JsonNode json =
                objectMapper.readTree(
                        response.body()
                );

        JsonNode urlNode =
                json.get("url");

        if (urlNode == null ||
                urlNode.asText().isBlank()) {

            throw new IOException(
                    "imgdb.io did not return an image URL."
            );
        }

        return urlNode.asText();
    }

    // =========================================================
    // QUANTICDATA GOOGLE LENS
    // =========================================================

    private List<Match> searchGoogleLens(
            String imageUrl
    ) throws Exception {

        String jsonBody =
                objectMapper.createObjectNode()
                        .put(
                                "image_url",
                                imageUrl
                        )
                        .put(
                                "max_results",
                                20
                        )
                        .toString();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        QUANTICDATA_URL
                                )
                        )
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        jsonBody
                                )
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {

            throw new IOException(
                    "Reverse image search failed. HTTP "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        return parseResults(
                response.body()
        );
    }

    // =========================================================
    // PARSE RESULTS
    // =========================================================

    private List<Match> parseResults(
            String response
    ) throws Exception {

        List<Match> matches =
                new ArrayList<>();

        JsonNode root =
                objectMapper.readTree(
                        response
                );

        JsonNode payload =
                root.path("payload");

        JsonNode results =
                payload.path("results");

        /*
         * QuanticData may return the result rows
         * under the payload depending on collector
         * response version.
         */

        if (!results.isArray()) {

            results =
                    payload.path("rows");
        }

        if (!results.isArray()) {

            results =
                    root.path("results");
        }

        if (results.isArray()) {

            for (JsonNode item : results) {

                String title =
                        text(
                                item,
                                "title"
                        );

                String source =
                        text(
                                item,
                                "source"
                        );

                String domain =
                        text(
                                item,
                                "domain"
                        );

                String link =
                        text(
                                item,
                                "link"
                        );

                String image =
                        text(
                                item,
                                "image"
                        );

                String thumbnail =
                        text(
                                item,
                                "thumbnail"
                        );

                matches.add(
                        new Match(
                                title,
                                source,
                                domain,
                                link,
                                image,
                                thumbnail
                        )
                );
            }
        }

        return matches;
    }

    // =========================================================
    // SAFE JSON TEXT
    // =========================================================

    private String text(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return "";
        }

        return value.asText("");
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public void shutdown() {
        // HttpClient does not require explicit shutdown.
    }
}