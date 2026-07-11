package com.localroots.clientfiles.storage;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class S3StorageService {
    private final S3StorageProperties properties;

    public S3StorageService(S3StorageProperties properties) {
        this.properties = properties;
    }

    public String bucket() {
        return properties.getBucket();
    }

    public String buildAttachmentKey(UUID tenantId, UUID contactId, String sourceSystem, String fileName) {
        String safeFileName = fileName == null || fileName.isBlank()
                ? "attachment"
                : fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return "tenants/%s/contacts/%s/%s/%s-%s".formatted(
                tenantId,
                contactId,
                sourceSystem.toLowerCase(),
                UUID.randomUUID(),
                safeFileName
        );
    }

    public String buildViewUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank() || properties.getPublicBaseUrl() == null || properties.getPublicBaseUrl().isBlank()) {
            return null;
        }
        String base = properties.getPublicBaseUrl().endsWith("/")
                ? properties.getPublicBaseUrl().substring(0, properties.getPublicBaseUrl().length() - 1)
                : properties.getPublicBaseUrl();
        return base + "/" + URLEncoder.encode(s3Key, StandardCharsets.UTF_8).replace("%2F", "/");
    }
}
