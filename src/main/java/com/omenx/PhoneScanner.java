package com.omenx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Omen-X Phone Number Scanner
 *
 * CORE:
 * Google libphonenumber
 *
 * ONLINE ENRICHMENT:
 * Abstract Phone Validation API
 *
 * Provides:
 * - Basic information
 * - Validation
 * - Number type
 * - Country
 * - Region
 * - Formatting
 * - Original carrier
 * - Abstract carrier
 * - Abstract line type
 * - Registered location
 * - Time zones
 *
 * API key is loaded from:
 * src/main/resources/config.properties
 *
 * Property:
 * ABSTRACT_PHONE_API_KEY=YOUR_KEY
 */
public class PhoneScanner {

    private final PhoneNumberUtil phoneUtil;
    private final PhoneNumberOfflineGeocoder geocoder;
    private final PhoneNumberToCarrierMapper carrierMapper;
    private final PhoneNumberToTimeZonesMapper timeZoneMapper;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String abstractApiKey;

    public PhoneScanner() {

        phoneUtil = PhoneNumberUtil.getInstance();

        geocoder =
                PhoneNumberOfflineGeocoder.getInstance();

        carrierMapper =
                PhoneNumberToCarrierMapper.getInstance();

        timeZoneMapper =
                PhoneNumberToTimeZonesMapper.getInstance();

        httpClient =
                HttpClient.newBuilder()
                        .build();

        objectMapper =
                new ObjectMapper();

        abstractApiKey =
                loadAbstractApiKey();
    }

    // =========================================================
    // MAIN SCAN
    // =========================================================

    public PhoneResult scan(
            String input,
            String defaultRegion
    ) throws Exception {

        if (input == null ||
                input.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Phone number cannot be empty."
            );
        }

        String numberInput =
                input.trim();

        String region =
                defaultRegion;

        if (region == null ||
                region.trim().isEmpty()) {

            region = "IN";
        }

        // =====================================================
        // PARSE NUMBER
        // =====================================================

        Phonenumber.PhoneNumber number;

        try {

            number =
                    phoneUtil.parse(
                            numberInput,
                            region
                    );

        } catch (NumberParseException e) {

            throw new IllegalArgumentException(
                    "Unable to parse phone number: "
                            + e.getMessage()
            );
        }

        // =====================================================
        // LIBPHONENUMBER VALIDATION
        // =====================================================

        boolean possible =
                phoneUtil.isPossibleNumber(
                        number
                );

        boolean valid =
                phoneUtil.isValidNumber(
                        number
                );

        // =====================================================
        // COUNTRY
        // =====================================================

        String countryCode =
                "+"
                        + number.getCountryCode();

        String country =
                getCountryName(
                        number.getCountryCode()
                );

        String detectedRegion =
                phoneUtil.getRegionCodeForNumber(
                        number
                );

        if (detectedRegion == null ||
                detectedRegion.isBlank()) {

            detectedRegion = "Unknown";
        }

        boolean regionMatch =
                region.equalsIgnoreCase(
                        detectedRegion
                );

        // =====================================================
        // FORMATTING
        // =====================================================

        String internationalFormat =
                phoneUtil.format(
                        number,
                        PhoneNumberUtil.PhoneNumberFormat
                                .INTERNATIONAL
                );

        String nationalFormat =
                phoneUtil.format(
                        number,
                        PhoneNumberUtil.PhoneNumberFormat
                                .NATIONAL
                );

        String e164Format =
                phoneUtil.format(
                        number,
                        PhoneNumberUtil.PhoneNumberFormat
                                .E164
                );

        String rfc3966Format =
                phoneUtil.format(
                        number,
                        PhoneNumberUtil.PhoneNumberFormat
                                .RFC3966
                );

        String nationalNumber =
                String.valueOf(
                        number.getNationalNumber()
                );

        // =====================================================
        // NUMBER TYPE
        // =====================================================

        PhoneNumberUtil.PhoneNumberType phoneType =
                phoneUtil.getNumberType(
                        number
                );

        String numberType =
                getNumberTypeName(
                        phoneType
                );

        // =====================================================
        // ORIGINAL CARRIER
        // =====================================================

        String originalCarrier =
                carrierMapper.getNameForNumber(
                        number,
                        Locale.ENGLISH
                );

        if (originalCarrier == null ||
                originalCarrier.isBlank()) {

            originalCarrier =
                    "Not available";
        }

        // =====================================================
        // LIBPHONENUMBER LOCATION
        // =====================================================

        String geographicDescription =
                geocoder.getDescriptionForNumber(
                        number,
                        Locale.ENGLISH
                );

        if (geographicDescription == null ||
                geographicDescription.isBlank()) {

            geographicDescription =
                    "Not available";
        }

        // =====================================================
        // TIME ZONES
        // =====================================================

        List<String> timeZones =
                timeZoneMapper.getTimeZonesForNumber(
                        number
                );

        String timezoneText;

        if (timeZones == null ||
                timeZones.isEmpty()) {

            timezoneText =
                    "Not available";

        } else {

            timezoneText =
                    String.join(
                            ", ",
                            timeZones
                    );
        }

        // =====================================================
        // ABSTRACT API ENRICHMENT
        // =====================================================

        AbstractResult abstractResult =
                queryAbstractApi(
                        e164Format
                );

        // =====================================================
        // RETURN COMPLETE RESULT
        // =====================================================

        return new PhoneResult(

                // Core
                numberInput,
                internationalFormat,
                nationalFormat,
                e164Format,
                rfc3966Format,
                nationalNumber,

                // Country
                country,
                countryCode,
                detectedRegion,
                regionMatch,

                // Validation
                possible,
                valid,
                numberType,

                // libphonenumber
                originalCarrier,
                geographicDescription,
                detectedRegion,
                timezoneText,

                // Abstract
                abstractResult
        );
    }

    // =========================================================
    // ABSTRACT API
    // =========================================================

    private AbstractResult queryAbstractApi(
            String e164Number
    ) {

        if (abstractApiKey == null ||
                abstractApiKey.isBlank() ||
                abstractApiKey.equals("YOUR_API_KEY")) {

            return AbstractResult.notAvailable(
                    "API key not configured"
            );
        }

        try {

            String encodedKey =
                    URLEncoder.encode(
                            abstractApiKey,
                            StandardCharsets.UTF_8
                    );

            String encodedPhone =
                    URLEncoder.encode(
                            e164Number,
                            StandardCharsets.UTF_8
                    );

            String url =
                    "https://phonevalidation.abstractapi.com/v1/"
                            + "?api_key="
                            + encodedKey
                            + "&phone="
                            + encodedPhone;

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
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

            int status =
                    response.statusCode();

            if (status != 200) {

                return AbstractResult.notAvailable(
                        "Abstract API HTTP "
                                + status
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            return parseAbstractResponse(
                    root
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return AbstractResult.notAvailable(
                    "Request interrupted"
            );

        } catch (Exception e) {

            return AbstractResult.notAvailable(
                    "Abstract API unavailable"
            );
        }
    }

    // =========================================================
    // PARSE ABSTRACT RESPONSE
    // =========================================================

    private AbstractResult parseAbstractResponse(
            JsonNode root
    ) {

        boolean valid =
                getBoolean(
                        root,
                        "valid",
                        false
                );

        String phone =
                getText(
                        root,
                        "phone",
                        "Not available"
                );

        String location =
                getText(
                        root,
                        "location",
                        "Not available"
                );

        String carrier =
                getText(
                        root,
                        "carrier",
                        "Not available"
                );

        String type =
                getText(
                        root,
                        "type",
                        "Not available"
                );

        String internationalFormat =
                getNestedText(
                        root,
                        "format",
                        "international",
                        "Not available"
                );

        String localFormat =
                getNestedText(
                        root,
                        "format",
                        "local",
                        "Not available"
                );

        String countryCode =
                getNestedText(
                        root,
                        "country",
                        "code",
                        "Not available"
                );

        String countryName =
                getNestedText(
                        root,
                        "country",
                        "name",
                        "Not available"
                );

        String countryPrefix =
                getNestedText(
                        root,
                        "country",
                        "prefix",
                        "Not available"
                );

        // Some Abstract responses use the newer
        // field names. Support those too.

        if (location.equals("Not available")) {

            location =
                    getText(
                            root,
                            "registered_location",
                            "Not available"
                    );
        }

        if (internationalFormat.equals("Not available")) {

            internationalFormat =
                    getText(
                            root,
                            "international_format",
                            "Not available"
                    );
        }

        if (localFormat.equals("Not available")) {

            localFormat =
                    getText(
                            root,
                            "local_format",
                            "Not available"
                    );
        }

        if (countryCode.equals("Not available")) {

            countryCode =
                    getText(
                            root,
                            "country_code",
                            "Not available"
                    );
        }

        if (countryName.equals("Not available")) {

            countryName =
                    getText(
                            root,
                            "country_name",
                            "Not available"
                    );
        }

        if (countryPrefix.equals("Not available")) {

            countryPrefix =
                    getText(
                            root,
                            "country_prefix",
                            "Not available"
                    );
        }

        if (carrier.equals("Not available")) {

            carrier =
                    getText(
                            root,
                            "carrier",
                            "Not available"
                    );
        }

        if (type.equals("Not available")) {

            type =
                    getText(
                            root,
                            "line_type",
                            "Not available"
                    );
        }

        return new AbstractResult(

                true,

                "",

                phone,

                valid,

                internationalFormat,

                localFormat,

                countryName,

                countryCode,

                countryPrefix,

                location,

                carrier,

                type
        );
    }

    // =========================================================
    // JSON HELPERS
    // =========================================================

    private String getText(
            JsonNode root,
            String field,
            String defaultValue
    ) {

        JsonNode node =
                root.get(field);

        if (node == null ||
                node.isNull()) {

            return defaultValue;
        }

        String value =
                node.asText();

        if (value == null ||
                value.isBlank()) {

            return defaultValue;
        }

        return value;
    }

    private String getNestedText(
            JsonNode root,
            String parent,
            String child,
            String defaultValue
    ) {

        JsonNode parentNode =
                root.get(parent);

        if (parentNode == null ||
                parentNode.isNull()) {

            return defaultValue;
        }

        JsonNode childNode =
                parentNode.get(child);

        if (childNode == null ||
                childNode.isNull()) {

            return defaultValue;
        }

        String value =
                childNode.asText();

        if (value == null ||
                value.isBlank()) {

            return defaultValue;
        }

        return value;
    }

    private boolean getBoolean(
            JsonNode root,
            String field,
            boolean defaultValue
    ) {

        JsonNode node =
                root.get(field);

        if (node == null ||
                node.isNull()) {

            return defaultValue;
        }

        return node.asBoolean(
                defaultValue
        );
    }

    // =========================================================
    // CONFIG
    // =========================================================

    private String loadAbstractApiKey() {

        Properties properties =
                new Properties();

        try {

            InputStream input =
                    PhoneScanner.class
                            .getClassLoader()
                            .getResourceAsStream(
                                    "config.properties"
                            );

            if (input == null) {

                return "";
            }

            try (input) {

                properties.load(
                        input
                );
            }

            String key =
                    properties.getProperty(
                            "ABSTRACT_PHONE_API_KEY",
                            ""
                    );

            if (key == null) {

                return "";
            }

            return key.trim();

        } catch (IOException e) {

            return "";
        }
    }

    // =========================================================
    // COUNTRY NAME
    // =========================================================

    private String getCountryName(
            int countryCallingCode
    ) {

        String regionCode =
                phoneUtil.getRegionCodeForCountryCode(
                        countryCallingCode
                );

        if (regionCode == null ||
                regionCode.isBlank()) {

            return "Unknown";
        }

        String countryName =
                new Locale(
                        "",
                        regionCode
                ).getDisplayCountry(
                        Locale.ENGLISH
                );

        if (countryName == null ||
                countryName.isBlank()) {

            return regionCode;
        }

        return countryName;
    }

    // =========================================================
    // NUMBER TYPE
    // =========================================================

    private String getNumberTypeName(
            PhoneNumberUtil.PhoneNumberType type
    ) {

        if (type == null) {

            return "Unknown";
        }

        return switch (type) {

            case MOBILE ->
                    "Mobile";

            case FIXED_LINE ->
                    "Fixed Line";

            case FIXED_LINE_OR_MOBILE ->
                    "Fixed Line / Mobile";

            case TOLL_FREE ->
                    "Toll Free";

            case PREMIUM_RATE ->
                    "Premium Rate";

            case SHARED_COST ->
                    "Shared Cost";

            case VOIP ->
                    "VoIP";

            case PERSONAL_NUMBER ->
                    "Personal Number";

            case PAGER ->
                    "Pager";

            case UAN ->
                    "UAN";

            case VOICEMAIL ->
                    "Voicemail";

            case UNKNOWN ->
                    "Unknown";
        };
    }

    // =========================================================
    // ABSTRACT RESULT
    // =========================================================

    public static class AbstractResult {

        private final boolean available;
        private final String error;
        private final String phone;
        private final boolean valid;
        private final String internationalFormat;
        private final String localFormat;
        private final String countryName;
        private final String countryCode;
        private final String countryPrefix;
        private final String location;
        private final String carrier;
        private final String lineType;

        public AbstractResult(
                boolean available,
                String error,
                String phone,
                boolean valid,
                String internationalFormat,
                String localFormat,
                String countryName,
                String countryCode,
                String countryPrefix,
                String location,
                String carrier,
                String lineType
        ) {

            this.available =
                    available;

            this.error =
                    error;

            this.phone =
                    phone;

            this.valid =
                    valid;

            this.internationalFormat =
                    internationalFormat;

            this.localFormat =
                    localFormat;

            this.countryName =
                    countryName;

            this.countryCode =
                    countryCode;

            this.countryPrefix =
                    countryPrefix;

            this.location =
                    location;

            this.carrier =
                    carrier;

            this.lineType =
                    lineType;
        }

        public static AbstractResult notAvailable(
                String reason
        ) {

            return new AbstractResult(

                    false,
                    reason,
                    "Not available",
                    false,
                    "Not available",
                    "Not available",
                    "Not available",
                    "Not available",
                    "Not available",
                    "Not available",
                    "Not available",
                    "Not available"
            );
        }

        public boolean isAvailable() {
            return available;
        }

        public String getError() {
            return error;
        }

        public String getPhone() {
            return phone;
        }

        public boolean isValid() {
            return valid;
        }

        public String getInternationalFormat() {
            return internationalFormat;
        }

        public String getLocalFormat() {
            return localFormat;
        }

        public String getCountryName() {
            return countryName;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getCountryPrefix() {
            return countryPrefix;
        }

        public String getLocation() {
            return location;
        }

        public String getCarrier() {
            return carrier;
        }

        public String getLineType() {
            return lineType;
        }
    }

    // =========================================================
    // PHONE RESULT
    // =========================================================

    public static class PhoneResult {

        private final String inputNumber;
        private final String internationalFormat;
        private final String nationalFormat;
        private final String e164Format;
        private final String rfc3966Format;
        private final String nationalNumber;

        private final String country;
        private final String countryCode;
        private final String region;
        private final boolean regionMatch;

        private final boolean possible;
        private final boolean valid;
        private final String numberType;

        private final String originalCarrier;
        private final String geographicDescription;
        private final String regionDescription;
        private final String timezones;

        private final AbstractResult abstractResult;

        public PhoneResult(
                String inputNumber,
                String internationalFormat,
                String nationalFormat,
                String e164Format,
                String rfc3966Format,
                String nationalNumber,
                String country,
                String countryCode,
                String region,
                boolean regionMatch,
                boolean possible,
                boolean valid,
                String numberType,
                String originalCarrier,
                String geographicDescription,
                String regionDescription,
                String timezones,
                AbstractResult abstractResult
        ) {

            this.inputNumber =
                    inputNumber;

            this.internationalFormat =
                    internationalFormat;

            this.nationalFormat =
                    nationalFormat;

            this.e164Format =
                    e164Format;

            this.rfc3966Format =
                    rfc3966Format;

            this.nationalNumber =
                    nationalNumber;

            this.country =
                    country;

            this.countryCode =
                    countryCode;

            this.region =
                    region;

            this.regionMatch =
                    regionMatch;

            this.possible =
                    possible;

            this.valid =
                    valid;

            this.numberType =
                    numberType;

            this.originalCarrier =
                    originalCarrier;

            this.geographicDescription =
                    geographicDescription;

            this.regionDescription =
                    regionDescription;

            this.timezones =
                    timezones;

            this.abstractResult =
                    abstractResult;
        }

        // =====================================================
        // CORE GETTERS
        // =====================================================

        public String getInputNumber() {
            return inputNumber;
        }

        public String getInternationalFormat() {
            return internationalFormat;
        }

        public String getNationalFormat() {
            return nationalFormat;
        }

        public String getE164Format() {
            return e164Format;
        }

        public String getRfc3966Format() {
            return rfc3966Format;
        }

        public String getNationalNumber() {
            return nationalNumber;
        }

        public String getCountry() {
            return country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getRegion() {
            return region;
        }

        public boolean isRegionMatch() {
            return regionMatch;
        }

        public boolean isPossible() {
            return possible;
        }

        public boolean isValid() {
            return valid;
        }

        public String getNumberType() {
            return numberType;
        }

        public String getOriginalCarrier() {
            return originalCarrier;
        }

        public String getGeographicDescription() {
            return geographicDescription;
        }

        public String getRegionDescription() {
            return regionDescription;
        }

        public String getTimezones() {
            return timezones;
        }

        // =====================================================
        // ABSTRACT GETTERS
        // =====================================================

        public boolean isAbstractAvailable() {

            return abstractResult != null &&
                    abstractResult.isAvailable();
        }

        public String getAbstractError() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult.getError();
        }

        public boolean isAbstractValid() {

            return abstractResult != null &&
                    abstractResult.isValid();
        }

        public String getAbstractPhone() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult.getPhone();
        }

        public String getAbstractInternationalFormat() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getInternationalFormat();
        }

        public String getAbstractLocalFormat() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getLocalFormat();
        }

        public String getAbstractCountry() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getCountryName();
        }

        public String getAbstractCountryCode() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getCountryCode();
        }

        public String getAbstractCountryPrefix() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getCountryPrefix();
        }

        public String getAbstractLocation() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getLocation();
        }

        public String getAbstractCarrier() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getCarrier();
        }

        public String getAbstractLineType() {

            if (abstractResult == null) {

                return "Not available";
            }

            return abstractResult
                    .getLineType();
        }

        // =====================================================
        // FINDINGS
        // =====================================================

        public String[] getFindings() {

            java.util.ArrayList<String> findings =
                    new java.util.ArrayList<>();

            if (valid) {

                findings.add(
                        "Valid phone number"
                );

            } else {

                findings.add(
                        "Phone number failed validation"
                );
            }

            if (possible) {

                findings.add(
                        "Possible phone number"
                );
            }

            findings.add(
                    "Number type: "
                            + numberType
            );

            if (!country.equals("Unknown")) {

                findings.add(
                        "Country identified: "
                                + country
                );
            }

            if (!originalCarrier.equals(
                    "Not available")) {

                findings.add(
                        "Original carrier identified: "
                                + originalCarrier
                );
            }

            if (!timezones.equals(
                    "Not available")) {

                findings.add(
                        "Timezone identified: "
                                + timezones
                );
            }

            if (isAbstractAvailable()) {

                findings.add(
                        "Abstract API enrichment completed"
                );

                if (!getAbstractCarrier().equals(
                        "Not available")) {

                    findings.add(
                            "Online carrier: "
                                    + getAbstractCarrier()
                    );
                }

                if (!getAbstractLocation().equals(
                        "Not available")) {

                    findings.add(
                            "Registered location available"
                    );
                }

            } else {

                findings.add(
                        "Abstract enrichment unavailable"
                );
            }

            return findings.toArray(
                    new String[0]
            );
        }
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public void shutdown() {
        // No shutdown required.
    }
}