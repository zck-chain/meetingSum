package com.meetingsum.util;

import java.util.List;

public class FileValidationUtils {

    private FileValidationUtils() {}

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public static boolean isFormatAllowed(String filename, List<String> allowedFormats) {
        String ext = getFileExtension(filename);
        return allowedFormats.contains(ext);
    }

    public static String safeFilename(String filename) {
        if (filename == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : filename.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
