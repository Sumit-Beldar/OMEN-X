package com.omenx.osint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

/**
 * ReverseImageScanner
 *
 * Day 3 foundation for OMEN-X's Reverse Image OSINT module.
 *
 * Current responsibilities:
 *  - Validate the supplied image
 *  - Read basic file information
 *  - Calculate SHA-256 hash
 *  - Read EXIF/GPS information
 *  - Prepare a structured Result object for the future
 *    reverse-image/web-search layer
 *
 * IMPORTANT:
 * This class does NOT pretend to perform a reverse-image
 * search yet. The actual internet search provider will be
 * added separately.
 */
public class ReverseImageScanner {

    // =========================================================
    // RESULT
    // =========================================================

    public static class Result {

        private final Map<String, String> information =
                new LinkedHashMap<>();

        private boolean validImage = false;
        private boolean hasGps = false;

        private Double latitude = null;
        private Double longitude = null;

        public Map<String, String> getInformation() {
            return information;
        }

        public boolean isValidImage() {
            return validImage;
        }

        public boolean hasGps() {
            return hasGps;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }
    }

    // =========================================================
    // MAIN SCAN METHOD
    // =========================================================

    public Result scan(File imageFile) {

        Result result = new Result();

        // -----------------------------------------------------
        // Validate file
        // -----------------------------------------------------

        if (imageFile == null) {
            result.information.put(
                    "Status",
                    "No image was selected."
            );

            return result;
        }

        if (!imageFile.exists()) {
            result.information.put(
                    "Status",
                    "Selected file does not exist."
            );

            return result;
        }

        if (!imageFile.isFile()) {
            result.information.put(
                    "Status",
                    "Selected path is not a file."
            );

            return result;
        }

        if (!isSupportedImage(imageFile)) {
            result.information.put(
                    "Status",
                    "Unsupported image format."
            );

            return result;
        }

        result.validImage = true;

        // -----------------------------------------------------
        // Basic file information
        // -----------------------------------------------------

        result.information.put(
                "Status",
                "Image loaded successfully."
        );

        result.information.put(
                "File Name",
                imageFile.getName()
        );

        result.information.put(
                "File Path",
                imageFile.getAbsolutePath()
        );

        result.information.put(
                "File Size",
                formatFileSize(imageFile.length())
        );

        result.information.put(
                "Extension",
                getExtension(imageFile)
        );

        // -----------------------------------------------------
        // SHA-256
        // -----------------------------------------------------

        String hash = calculateSha256(imageFile);

        if (hash != null) {

            result.information.put(
                    "SHA-256",
                    hash
            );

        } else {

            result.information.put(
                    "SHA-256",
                    "Unable to calculate hash"
            );
        }

        // -----------------------------------------------------
        // EXIF / GPS
        // -----------------------------------------------------

        readGpsMetadata(imageFile, result);

        // -----------------------------------------------------
        // Future reverse-image search status
        // -----------------------------------------------------

        result.information.put(
                "Reverse Image Search",
                "Not connected yet"
        );

        result.information.put(
                "Location Analysis",
                result.hasGps
                        ? "GPS coordinates found"
                        : "GPS coordinates not found"
        );

        return result;
    }

    // =========================================================
    // SUPPORTED IMAGE CHECK
    // =========================================================

    private boolean isSupportedImage(File file) {

        String extension = getExtension(file)
                .toLowerCase();

        return extension.equals(".jpg")
                || extension.equals(".jpeg")
                || extension.equals(".png")
                || extension.equals(".gif")
                || extension.equals(".tif")
                || extension.equals(".tiff")
                || extension.equals(".webp");
    }

    // =========================================================
    // EXTENSION
    // =========================================================

    private String getExtension(File file) {

        String name = file.getName();

        int dot = name.lastIndexOf('.');

        if (dot < 0) {
            return "";
        }

        return name.substring(dot);
    }

    // =========================================================
    // FILE SIZE
    // =========================================================

    private String formatFileSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb = bytes / 1024.0;

        if (kb < 1024) {
            return String.format(
                    "%.2f KB",
                    kb
            );
        }

        double mb = kb / 1024.0;

        if (mb < 1024) {
            return String.format(
                    "%.2f MB",
                    mb
            );
        }

        double gb = mb / 1024.0;

        return String.format(
                "%.2f GB",
                gb
        );
    }

    // =========================================================
    // SHA-256
    // =========================================================

    private String calculateSha256(File file) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (FileInputStream input =
                         new FileInputStream(file)) {

                byte[] buffer = new byte[8192];

                int bytesRead;

                while ((bytesRead =
                        input.read(buffer)) != -1) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            byte[] hashBytes =
                    digest.digest();

            StringBuilder hash =
                    new StringBuilder();

            for (byte b : hashBytes) {

                hash.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return hash.toString();

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // GPS / EXIF
    // =========================================================

    private void readGpsMetadata(
            File imageFile,
            Result result
    ) {

        try {

            Metadata metadata =
                    ImageMetadataReader
                            .readMetadata(imageFile);

            GpsDirectory gps =
                    metadata.getFirstDirectoryOfType(
                            GpsDirectory.class
                    );

            if (gps == null) {

                result.information.put(
                        "GPS",
                        "Not available"
                );

                return;
            }

            if (gps.getGeoLocation() == null) {

                result.information.put(
                        "GPS",
                        "GPS directory found, but coordinates are unavailable"
                );

                return;
            }

            double latitude =
                    gps.getGeoLocation().getLatitude();

            double longitude =
                    gps.getGeoLocation().getLongitude();

            result.latitude = latitude;
            result.longitude = longitude;
            result.hasGps = true;

            result.information.put(
                    "GPS",
                    "Available"
            );

            result.information.put(
                    "Latitude",
                    String.format(
                            "%.6f",
                            latitude
                    )
            );

            result.information.put(
                    "Longitude",
                    String.format(
                            "%.6f",
                            longitude
                    )
            );

        } catch (Exception e) {

            result.information.put(
                    "GPS",
                    "Unable to read EXIF data"
            );
        }
    }
}