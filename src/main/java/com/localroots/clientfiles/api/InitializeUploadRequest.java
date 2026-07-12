package com.localroots.clientfiles.api;

import com.localroots.clientfiles.attachment.AttachmentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record InitializeUploadRequest(
        UUID contactId,
        UUID estimateId,
        UUID parentAttachmentId,
        @NotNull AttachmentCategory category,
        @NotBlank @Size(max = 255) String originalFileName,
        @Size(max = 255) String displayName,
        @NotBlank @Size(max = 255) String contentType,
        @Positive long sizeBytes,
        @Pattern(regexp = "^[A-Za-z0-9+/]{43}=$", message = "must be a base64-encoded SHA-256 checksum")
        String checksumSha256Base64,
        @Size(max = 100) String sourceSystem,
        @Size(max = 2000) String description,
        Map<String, Object> metadata
) {
}
