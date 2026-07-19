package com.localroots.clientfiles.attachment;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

final class DownloadFileNameBuilder {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/heic", "heic"),
            Map.entry("image/heif", "heif"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("text/plain", "txt"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/quicktime", "mov")
    );

    private DownloadFileNameBuilder() {
    }

    static String build(String displayName, String originalFileName, String contentType) {
        String original = hasText(originalFileName) ? originalFileName.trim() : "file";
        String extension = extractExtension(original);
        if (extension == null) {
            extension = CONTENT_TYPE_EXTENSIONS.get(normalizeContentType(contentType));
        }

        String baseName = hasText(displayName)
                ? displayName.trim()
                : stripExtension(original);

        if (extension != null && baseName.toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            baseName = baseName.substring(0, baseName.length() - extension.length() - 1);
        }

        baseName = sanitizeBaseName(baseName);
        if (baseName.isBlank()) {
            baseName = "file";
        }

        return extension == null ? baseName : baseName + "." + extension;
    }

    private static String sanitizeBaseName(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replace("'", "")
                .replace("’", "")
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String stripExtension(String fileName) {
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String leafName = separator >= 0 ? fileName.substring(separator + 1) : fileName;
        int dot = leafName.lastIndexOf('.');
        return dot > 0 ? leafName.substring(0, dot) : leafName;
    }

    private static String extractExtension(String fileName) {
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String leafName = separator >= 0 ? fileName.substring(separator + 1) : fileName;
        int dot = leafName.lastIndexOf('.');
        if (dot <= 0 || dot == leafName.length() - 1) {
            return null;
        }

        String extension = leafName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? extension : null;
    }

    private static String normalizeContentType(String contentType) {
        if (!hasText(contentType)) {
            return "";
        }
        int separator = contentType.indexOf(';');
        String value = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
