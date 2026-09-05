package com.omenx.osint;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.file.FileTypeDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageMetadataScanner {

    public static class Result {
        public final Map<String, String> metadata = new LinkedHashMap<>();
        public Double latitude = null;
        public Double longitude = null;
        public boolean hasGps = false;
    }

    public Result scan(File imageFile) {
        Result result = new Result();

        try {
            // ========== FILE FACTS ==========
            result.metadata.put("File Name", imageFile.getName());
            result.metadata.put("Source", "Local file");

            String name = imageFile.getName();
            String extension = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                extension = name.substring(dot).toLowerCase();
            }
            result.metadata.put("Extension", extension.isEmpty() ? "—" : extension);

            // Size
            double sizeKb = imageFile.length() / 1024.0;
            if (sizeKb > 1024) {
                result.metadata.put("Size", String.format("%.2f MB", sizeKb / 1024.0));
            } else {
                result.metadata.put("Size", String.format("%.2f KB", sizeKb));
            }

            // MIME + Signature
            String mime = "—";
            String signature = "—";
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                byte[] header = new byte[12];
                int read = fis.read(header);
                if (read >= 3) {
                    // JPEG
                    if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                        mime = "image/jpeg";
                        signature = "JPEG (FF D8 FF ...)";
                    }
                    // PNG
                    else if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                        mime = "image/png";
                        signature = "PNG (89 50 4E 47 ...)";
                    }
                    // GIF
                    else if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) {
                        mime = "image/gif";
                        signature = "GIF (47 49 46 ...)";
                    }
                    // WEBP
                    else if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46) {
                        mime = "image/webp";
                        signature = "WEBP (RIFF ...)";
                    }
                    // TIFF
                    else if ((header[0] == 0x49 && header[1] == 0x49) || (header[0] == 0x4D && header[1] == 0x4D)) {
                        mime = "image/tiff";
                        signature = "TIFF";
                    }
                }
            }
            result.metadata.put("Detected MIME", mime);
            result.metadata.put("Signature", signature);

            // ========== EXIF / IMAGE DATA ==========
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            JpegDirectory jpeg = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpeg != null) {
                if (jpeg.containsTag(JpegDirectory.TAG_IMAGE_WIDTH))
                    result.metadata.put("Width", jpeg.getString(JpegDirectory.TAG_IMAGE_WIDTH) + " px");
                if (jpeg.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT))
                    result.metadata.put("Height", jpeg.getString(JpegDirectory.TAG_IMAGE_HEIGHT) + " px");
            }

            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                if (ifd0.containsTag(ExifIFD0Directory.TAG_MAKE))
                    result.metadata.put("Camera Make", ifd0.getString(ExifIFD0Directory.TAG_MAKE));
                if (ifd0.containsTag(ExifIFD0Directory.TAG_MODEL))
                    result.metadata.put("Camera Model", ifd0.getString(ExifIFD0Directory.TAG_MODEL));
                if (ifd0.containsTag(ExifIFD0Directory.TAG_SOFTWARE))
                    result.metadata.put("Software", ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE));
                if (ifd0.containsTag(ExifIFD0Directory.TAG_DATETIME))
                    result.metadata.put("Date Taken", ifd0.getString(ExifIFD0Directory.TAG_DATETIME));
            }

            ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfd != null) {
                if (subIfd.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
                    result.metadata.put("Original Date", subIfd.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
                if (subIfd.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME))
                    result.metadata.put("Exposure", subIfd.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) + " s");
                if (subIfd.containsTag(ExifSubIFDDirectory.TAG_FNUMBER))
                    result.metadata.put("F-Number", "f/" + subIfd.getString(ExifSubIFDDirectory.TAG_FNUMBER));
                if (subIfd.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT))
                    result.metadata.put("ISO", subIfd.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
            }

            // GPS
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null && gps.getGeoLocation() != null) {
                result.latitude = gps.getGeoLocation().getLatitude();
                result.longitude = gps.getGeoLocation().getLongitude();
                result.hasGps = true;

                result.metadata.put("GPS Latitude", String.format("%.6f", result.latitude));
                result.metadata.put("GPS Longitude", String.format("%.6f", result.longitude));
            }

        } catch (Exception e) {
            result.metadata.put("Error", "Failed to read metadata: " + e.getMessage());
        }

        return result;
    }
}