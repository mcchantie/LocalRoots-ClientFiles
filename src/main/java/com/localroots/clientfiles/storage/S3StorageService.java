package com.localroots.clientfiles.storage;

import com.localroots.clientfiles.attachment.AttachmentCategory;
import com.localroots.clientfiles.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final String bucket;
    private final DataSize maxFileSize;
    private final Duration uploadUrlTtl;
    private final Duration downloadUrlTtl;
    private final Set<String> allowedContentTypes;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${storage.s3.bucket}") String bucket,
            @Value("${storage.s3.max-file-size:100MB}") DataSize maxFileSize,
            @Value("${storage.s3.upload-url-ttl:15m}") Duration uploadUrlTtl,
            @Value("${storage.s3.download-url-ttl:15m}") Duration downloadUrlTtl,
            @Value("${storage.s3.allowed-content-types}") String allowedContentTypes
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = requireValue(bucket, "storage.s3.bucket");
        this.maxFileSize = maxFileSize;
        this.uploadUrlTtl = uploadUrlTtl;
        this.downloadUrlTtl = downloadUrlTtl;

        this.allowedContentTypes = Arrays.stream(allowedContentTypes.split(","))
                .map(this::normalizeContentType)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String bucket() {
        return bucket;
    }

    public long maxFileSizeBytes() {
        return maxFileSize.toBytes();
    }

    public boolean isAllowedContentType(String contentType) {
        return allowedContentTypes.contains(normalizeContentType(contentType));
    }

    public String buildAttachmentKey(
            UUID tenantId,
            UUID contactId,
            UUID attachmentId,
            AttachmentCategory category,
            String fileName
    ) {
        String contactSegment =
                contactId == null ? "unassigned" : contactId.toString();

        String categorySegment = category.name()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');

        return "tenants/%s/contacts/%s/attachments/%s/original/%s/%s".formatted(
                tenantId,
                contactSegment,
                attachmentId,
                categorySegment,
                sanitizeFileName(fileName)
        );
    }

    public PresignedUpload presignUpload(
            String key,
            String contentType,
            String checksumSha256Base64
    ) {
        PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(normalizeContentType(contentType));

        if (checksumSha256Base64 != null
                && !checksumSha256Base64.isBlank()) {
            putObjectRequest.checksumSHA256(checksumSha256Base64);
        }

        PresignedPutObjectRequest presigned =
                s3Presigner.presignPutObject(
                        PutObjectPresignRequest.builder()
                                .signatureDuration(uploadUrlTtl)
                                .putObjectRequest(putObjectRequest.build())
                                .build()
                );

        Map<String, String> requiredHeaders = new LinkedHashMap<>();

        presigned.signedHeaders().forEach((name, values) -> {
            String normalizedName = name.toLowerCase(Locale.ROOT);

            if (!normalizedName.equals("host")
                    && !normalizedName.equals("content-length")) {
                requiredHeaders.put(
                        normalizedName,
                        String.join(",", values)
                );
            }
        });

        return new PresignedUpload(
                URI.create(presigned.url().toString()),
                Instant.now().plus(uploadUrlTtl),
                Map.copyOf(requiredHeaders)
        );
    }

    public UploadedObject headObject(
            String key,
            boolean includeChecksum
    ) {
        HeadObjectRequest.Builder request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (includeChecksum) {
            request.checksumMode(ChecksumMode.ENABLED);
        }

        try {
            HeadObjectResponse response =
                    s3Client.headObject(request.build());

            return new UploadedObject(
                    response.contentLength(),
                    normalizeContentType(response.contentType()),
                    trimQuotes(response.eTag()),
                    response.checksumSHA256()
            );

        } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Upload is not present in S3",
                        "The object was not found. Upload the file with the "
                                + "presigned URL before confirming completion."
                );
            }

            throw exception;
        }
    }

    public PresignedDownload presignDownload(
            String key,
            String fileName,
            boolean download
    ) {
        String disposition = (download ? "attachment" : "inline")
                + "; filename*=UTF-8''"
                + rfc5987(fileName);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition(disposition)
                .build();

        PresignedGetObjectRequest presigned =
                s3Presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(downloadUrlTtl)
                                .getObjectRequest(getObjectRequest)
                                .build()
                );

        return new PresignedDownload(
                URI.create(presigned.url().toString()),
                Instant.now().plus(downloadUrlTtl)
        );
    }

    public void assertKeyBelongsToTenant(
            String key,
            UUID tenantId
    ) {
        String expectedPrefix = "tenants/%s/".formatted(tenantId);

        if (key == null || !key.startsWith(expectedPrefix)) {
            throw new IllegalStateException(
                    "Stored S3 key does not match the attachment tenant."
            );
        }
    }

    public String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }

        int separator = contentType.indexOf(';');

        String value = separator >= 0
                ? contentType.substring(0, separator)
                : contentType;

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        String value = fileName == null
                ? "attachment"
                : fileName.trim();

        if (value.isBlank()) {
            value = "attachment";
        }

        value = value
                .replaceAll("[\\r\\n]", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");

        return value.length() <= 180
                ? value
                : value.substring(value.length() - 180);
    }

    private String trimQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private String rfc5987(String value) {
        String fileName = value == null || value.isBlank()
                ? "attachment"
                : value;

        return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String requireValue(
            String value,
            String propertyName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    propertyName + " must be configured."
            );
        }

        return value.trim();
    }

    public record PresignedUpload(
            URI url,
            Instant expiresAt,
            Map<String, String> requiredHeaders
    ) {
    }

    public record PresignedDownload(
            URI url,
            Instant expiresAt
    ) {
    }

    public record UploadedObject(
            long sizeBytes,
            String contentType,
            String etag,
            String checksumSha256Base64
    ) {
    }
}