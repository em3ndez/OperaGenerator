package com.kousenit.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for common file operations in the Opera Generator.
 */
public class OperaFileUtils {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Sanitizes a string for use as a filename by converting to lowercase,
     * removing special characters, and replacing spaces with underscores.
     *
     * @param input the string to sanitize
     * @return sanitized filename string
     */
    public static String sanitizeFilename(String input) {
        if (input == null || input.isBlank()) {
            return "untitled";
        }
        return input.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "_")
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Creates an opera directory path with timestamp prefix.
     *
     * @param operaTitle the title of the opera
     * @param basePath the base path for opera storage
     * @return Path to the opera directory
     */
    public static Path getOperaDirectory(String operaTitle, String basePath) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String sanitizedTitle = sanitizeFilename(operaTitle);
        String directoryName = String.format("%s_%s", timestamp, sanitizedTitle);
        return Paths.get(basePath, directoryName);
    }

    /**
     * Creates an opera directory path without timestamp (for testing or specific uses).
     *
     * @param operaTitle the title of the opera
     * @param basePath the base path for opera storage
     * @param includeTimestamp whether to include timestamp prefix
     * @return Path to the opera directory
     */
    public static Path getOperaDirectory(String operaTitle, String basePath, boolean includeTimestamp) {
        if (includeTimestamp) {
            return getOperaDirectory(operaTitle, basePath);
        }
        String sanitizedTitle = sanitizeFilename(operaTitle);
        return Paths.get(basePath, sanitizedTitle);
    }

    /**
     * Extracts the opera title from a directory path.
     *
     * @param directoryPath the path to extract title from
     * @return the extracted title or "unknown" if unable to parse
     */
    public static String extractTitleFromPath(Path directoryPath) {
        String dirName = directoryPath.getFileName().toString();
        // Remove timestamp prefix if present (format: YYYYMMDD-HHMMSS_title)
        if (dirName.matches("\\d{8}-\\d{6}_.*")) {
            return dirName.substring(16).replace("_", " ");
        }
        return dirName.replace("_", " ");
    }
}