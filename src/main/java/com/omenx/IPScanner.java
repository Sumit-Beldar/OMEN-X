package com.omenx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// =========================================================
// IP SCANNER
//
// Sources:
// 1. IP-API      -> location + network information
// 2. AbuseIPDB   -> security / reputation information
//
// AbuseIPDB is optional.
// If the API key is missing or unavailable, the normal
// IP-API scan still works.
// =========================================================

public class IPScanner {

    // =========================================================
    // IP-API
    // =========================================================

    private static final String IP_API_URL =
            "http://ip-api.com/json/";

    // =========================================================
    // ABUSEIPDB
    // =========================================================

    private static final String ABUSE_API_URL =
            "https://api.abuseipdb.com/api/v2/check";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String abuseApiKey;

    // =========================================================
    // RESULT
    // =========================================================

    public static class IPResult {

        // -----------------------------------------------------
        // IP-API INFORMATION
        // -----------------------------------------------------

        private final String ip;
        private final String city;
        private final String region;
        private final String regionCode;
        private final String postalCode;
        private final String country;
        private final String countryCode;
        private final String continent;
        private final String continentCode;
        private final String latitude;
        private final String longitude;
        private final String timezone;
        private final String hostname;
        private final String provider;
        private final String organization;
        private final String asn;

        // -----------------------------------------------------
        // ABUSEIPDB INFORMATION
        // -----------------------------------------------------

        private final String ipVersion;
        private final String publicIp;
        private final String whitelisted;
        private final String abuseConfidenceScore;
        private final String usageType;
        private final String abuseIsp;
        private final String abuseDomain;
        private final String abuseHostnames;
        private final String tor;
        private final String totalReports;
        private final String distinctReporters;
        private final String lastReportedAt;
        private final String abuseStatus;

        // =====================================================
        // CONSTRUCTOR
        // =====================================================

        public IPResult(

                String ip,
                String city,
                String region,
                String regionCode,
                String postalCode,
                String country,
                String countryCode,
                String continent,
                String continentCode,
                String latitude,
                String longitude,
                String timezone,
                String hostname,
                String provider,
                String organization,
                String asn,

                String ipVersion,
                String publicIp,
                String whitelisted,
                String abuseConfidenceScore,
                String usageType,
                String abuseIsp,
                String abuseDomain,
                String abuseHostnames,
                String tor,
                String totalReports,
                String distinctReporters,
                String lastReportedAt,
                String abuseStatus
        ) {

            this.ip = ip;
            this.city = city;
            this.region = region;
            this.regionCode = regionCode;
            this.postalCode = postalCode;
            this.country = country;
            this.countryCode = countryCode;
            this.continent = continent;
            this.continentCode = continentCode;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = timezone;
            this.hostname = hostname;
            this.provider = provider;
            this.organization = organization;
            this.asn = asn;

            this.ipVersion = ipVersion;
            this.publicIp = publicIp;
            this.whitelisted = whitelisted;
            this.abuseConfidenceScore = abuseConfidenceScore;
            this.usageType = usageType;
            this.abuseIsp = abuseIsp;
            this.abuseDomain = abuseDomain;
            this.abuseHostnames = abuseHostnames;
            this.tor = tor;
            this.totalReports = totalReports;
            this.distinctReporters = distinctReporters;
            this.lastReportedAt = lastReportedAt;
            this.abuseStatus = abuseStatus;
        }

        // =====================================================
        // BASIC GETTERS
        // =====================================================

        public String getIp() {
            return ip;
        }

        public String getCity() {
            return city;
        }

        public String getRegion() {
            return region;
        }

        public String getRegionCode() {
            return regionCode;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public String getCountry() {
            return country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getContinent() {
            return continent;
        }

        public String getContinentCode() {
            return continentCode;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public String getTimezone() {
            return timezone;
        }

        public String getHostname() {
            return hostname;
        }

        public String getProvider() {
            return provider;
        }

        public String getOrganization() {
            return organization;
        }

        public String getAsn() {
            return asn;
        }

        public String getCoordinates() {
            return latitude + " / " + longitude;
        }

        // =====================================================
        // SECURITY GETTERS
        // =====================================================

        public String getIpVersion() {
            return ipVersion;
        }

        public String getPublicIp() {
            return publicIp;
        }

        public String getWhitelisted() {
            return whitelisted;
        }

        public String getAbuseConfidenceScore() {
            return abuseConfidenceScore;
        }

        public String getUsageType() {
            return usageType;
        }

        public String getAbuseIsp() {
            return abuseIsp;
        }

        public String getAbuseDomain() {
            return abuseDomain;
        }

        public String getAbuseHostnames() {
            return abuseHostnames;
        }

        public String getTor() {
            return tor;
        }

        public String getTotalReports() {
            return totalReports;
        }

        public String getDistinctReporters() {
            return distinctReporters;
        }

        public String getLastReportedAt() {
            return lastReportedAt;
        }

        public String getAbuseStatus() {
            return abuseStatus;
        }
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public IPScanner() {

        httpClient =
                HttpClient.newBuilder()
                        .followRedirects(
                                HttpClient.Redirect.NORMAL
                        )
                        .build();

        objectMapper =
                new ObjectMapper();

        abuseApiKey =
                loadAbuseApiKey();
    }

    // =========================================================
    // MAIN SCAN
    // =========================================================

    public IPResult scan(
            String ip
    ) throws Exception {

        if (ip == null ||
                ip.isBlank()) {

            throw new IllegalArgumentException(
                    "IP address cannot be empty."
            );
        }

        ip = ip.trim();

        // =====================================================
        // 1. IP-API
        // =====================================================

        String encodedIp =
                URLEncoder.encode(
                        ip,
                        StandardCharsets.UTF_8
                );

        String url =
                IP_API_URL
                        + encodedIp
                        + "?fields="
                        + "status,message,"
                        + "query,"
                        + "country,countryCode,"
                        + "region,regionName,"
                        + "city,zip,"
                        + "lat,lon,"
                        + "timezone,"
                        + "isp,org,as,"
                        + "reverse,"
                        + "continent,continentCode";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(url)
                        )
                        .header(
                                "User-Agent",
                                "OMEN-X/1.0"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "IP API returned HTTP "
                            + response.statusCode()
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );

        String status =
                getText(
                        root,
                        "status"
                );

        if (!"success".equalsIgnoreCase(status)) {

            String message =
                    getText(
                            root,
                            "message"
                    );

            throw new RuntimeException(
                    "IP lookup failed"
                            + (
                            message.equals("Unknown")
                                    ? ""
                                    : ": " + message
                    )
            );
        }

        // =====================================================
        // 2. ABUSEIPDB
        // =====================================================

        AbuseResult abuse =
                lookupAbuseIPDB(
                        ip
                );

        // =====================================================
        // 3. COMBINE RESULTS
        // =====================================================

        return new IPResult(

                getText(
                        root,
                        "query"
                ),

                getText(
                        root,
                        "city"
                ),

                getText(
                        root,
                        "regionName"
                ),

                getText(
                        root,
                        "region"
                ),

                getText(
                        root,
                        "zip"
                ),

                getText(
                        root,
                        "country"
                ),

                getText(
                        root,
                        "countryCode"
                ),

                getText(
                        root,
                        "continent"
                ),

                getText(
                        root,
                        "continentCode"
                ),

                getNumber(
                        root,
                        "lat"
                ),

                getNumber(
                        root,
                        "lon"
                ),

                getText(
                        root,
                        "timezone"
                ),

                getText(
                        root,
                        "reverse"
                ),

                getText(
                        root,
                        "isp"
                ),

                getText(
                        root,
                        "org"
                ),

                getASN(
                        root
                ),

                // AbuseIPDB
                abuse.ipVersion,
                abuse.publicIp,
                abuse.whitelisted,
                abuse.abuseConfidenceScore,
                abuse.usageType,
                abuse.isp,
                abuse.domain,
                abuse.hostnames,
                abuse.tor,
                abuse.totalReports,
                abuse.distinctReporters,
                abuse.lastReportedAt,
                abuse.status
        );
    }

    // =========================================================
    // ABUSE RESULT
    // =========================================================

    private static class AbuseResult {

        String ipVersion = "Not available";
        String publicIp = "Not available";
        String whitelisted = "Not available";
        String abuseConfidenceScore = "Not available";
        String usageType = "Not available";
        String isp = "Not available";
        String domain = "Not available";
        String hostnames = "Not available";
        String tor = "Not available";
        String totalReports = "Not available";
        String distinctReporters = "Not available";
        String lastReportedAt = "Not available";
        String status = "Not checked";
    }

    // =========================================================
    // ABUSEIPDB LOOKUP
    // =========================================================

    private AbuseResult lookupAbuseIPDB(
            String ip
    ) {

        AbuseResult result =
                new AbuseResult();

        // -----------------------------------------------------
        // No API key
        // -----------------------------------------------------

        if (abuseApiKey == null ||
                abuseApiKey.isBlank()) {

            result.status =
                    "API key not configured";

            return result;
        }

        try {

            String encodedIp =
                    URLEncoder.encode(
                            ip,
                            StandardCharsets.UTF_8
                    );

            String url =
                    ABUSE_API_URL
                            + "?ipAddress="
                            + encodedIp
                            + "&maxAgeInDays=90";

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .header(
                                    "Key",
                                    abuseApiKey
                            )
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {

                result.status =
                        "AbuseIPDB HTTP "
                                + response.statusCode();

                return result;
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode data =
                    root.get("data");

            if (data == null ||
                    data.isNull()) {

                result.status =
                        "No AbuseIPDB data";

                return result;
            }

            // -------------------------------------------------
            // IP VERSION
            // -------------------------------------------------

            result.ipVersion =
                    getNodeText(
                            data,
                            "ipVersion"
                    );

            // -------------------------------------------------
            // PUBLIC IP
            // -------------------------------------------------

            result.publicIp =
                    getBooleanText(
                            data,
                            "isPublic"
                    );

            // -------------------------------------------------
            // WHITELIST
            // -------------------------------------------------

            result.whitelisted =
                    getBooleanText(
                            data,
                            "isWhitelisted"
                    );

            // -------------------------------------------------
            // ABUSE SCORE
            // -------------------------------------------------

            result.abuseConfidenceScore =
                    getNodeText(
                            data,
                            "abuseConfidenceScore"
                    ) + "%";

            // -------------------------------------------------
            // USAGE TYPE
            // -------------------------------------------------

            result.usageType =
                    getNodeText(
                            data,
                            "usageType"
                    );

            // -------------------------------------------------
            // ISP
            // -------------------------------------------------

            result.isp =
                    getNodeText(
                            data,
                            "isp"
                    );

            // -------------------------------------------------
            // DOMAIN
            // -------------------------------------------------

            result.domain =
                    getNodeText(
                            data,
                            "domain"
                    );

            // -------------------------------------------------
            // HOSTNAMES
            // -------------------------------------------------

            JsonNode hostnamesNode =
                    data.get("hostnames");

            if (hostnamesNode != null &&
                    hostnamesNode.isArray() &&
                    hostnamesNode.size() > 0) {

                StringBuilder hosts =
                        new StringBuilder();

                for (JsonNode host :
                        hostnamesNode) {

                    if (hosts.length() > 0) {
                        hosts.append(", ");
                    }

                    hosts.append(
                            host.asText()
                    );
                }

                result.hostnames =
                        hosts.toString();

            } else {

                result.hostnames =
                        "None found";
            }

            // -------------------------------------------------
            // TOR
            // -------------------------------------------------

            result.tor =
                    getBooleanText(
                            data,
                            "isTor"
                    );

            // -------------------------------------------------
            // REPORT COUNT
            // -------------------------------------------------

            result.totalReports =
                    getNodeText(
                            data,
                            "totalReports"
                    );

            // -------------------------------------------------
            // DISTINCT REPORTERS
            // -------------------------------------------------

            result.distinctReporters =
                    getNodeText(
                            data,
                            "numDistinctUsers"
                    );

            // -------------------------------------------------
            // LAST REPORTED
            // -----------------------------------------------------

            result.lastReportedAt =
                    getNodeText(
                            data,
                            "lastReportedAt"
                    );

            if ("Unknown".equals(
                    result.lastReportedAt
            )) {

                result.lastReportedAt =
                        "Never";
            }

            result.status =
                    "Security lookup complete";

        } catch (Exception ex) {

            result.status =
                    "Security lookup failed: "
                            + ex.getMessage();
        }

        return result;
    }

    // =========================================================
    // LOAD ABUSE API KEY
    // =========================================================

    private String loadAbuseApiKey() {

        Properties properties =
                new Properties();

        try {

            InputStream input =
                    IPScanner.class
                            .getClassLoader()
                            .getResourceAsStream(
                                    "config.properties"
                            );

            if (input == null) {
                return null;
            }

            try (input) {

                properties.load(
                        input
                );
            }

            String key =
                    properties.getProperty(
                            "abuseipdb_api_key"
                    );

            if (key == null) {

                key =
                        properties.getProperty(
                                "ABUSEIPDB_API_KEY"
                        );
            }

            return key == null
                    ? null
                    : key.trim();

        } catch (Exception ex) {

            return null;
        }
    }

    // =========================================================
    // GET JSON TEXT
    // =========================================================

    private String getNodeText(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return "Unknown";
        }

        String text =
                value.asText();

        if (text == null ||
                text.isBlank()) {

            return "Unknown";
        }

        return text;
    }

    // =========================================================
    // GET BOOLEAN
    // =========================================================

    private String getBooleanText(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return "Unknown";
        }

        if (value.isBoolean()) {

            return value.asBoolean()
                    ? "Yes"
                    : "No";
        }

        return value.asText();
    }

    // =========================================================
    // GET TEXT
    // =========================================================

    private String getText(
            JsonNode root,
            String field
    ) {

        JsonNode value =
                root.get(field);

        if (value == null ||
                value.isNull()) {

            return "Unknown";
        }

        String text =
                value.asText();

        if (text == null ||
                text.isBlank()) {

            return "Unknown";
        }

        return text;
    }

    // =========================================================
    // GET NUMBER
    // =========================================================

    private String getNumber(
            JsonNode root,
            String field
    ) {

        JsonNode value =
                root.get(field);

        if (value == null ||
                value.isNull()) {

            return "Unknown";
        }

        if (!value.isNumber()) {

            return getText(
                    root,
                    field
            );
        }

        return value.asText();
    }

    // =========================================================
    // GET ASN
    // =========================================================

    private String getASN(
            JsonNode root
    ) {

        String as =
                getText(
                        root,
                        "as"
                );

        if (as.equals("Unknown")) {

            return "Unknown";
        }

        int space =
                as.indexOf(" ");

        if (space > 0) {

            return as.substring(
                    0,
                    space
            );
        }

        return as;
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public void shutdown() {

        // HttpClient does not require
        // explicit shutdown.
    }
}