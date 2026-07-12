package com.localroots.clientfiles.api;

import com.localroots.clientfiles.attachment.AttachmentCategory;
import com.localroots.clientfiles.attachment.AttachmentEntity;
import com.localroots.clientfiles.attachment.AttachmentFileKind;
import com.localroots.clientfiles.attachment.AttachmentStatus;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID tenantId,
        UUID contactId,
        UUID estimateId,
        UUID parentAttachmentId,
        AttachmentCategory category,
        AttachmentFileKind fileKind,
        AttachmentStatus status,
        String originalFileName,
        String displayName,
        String contentType,
        long declaredSizeBytes,
        Long actualSizeBytes,
        String checksumSha256Base64,
        String etag,
        String sourceSystem,
        String description,
        Map<String, Object> metadata,
        String failureReason,
        UUID createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime uploadedAt,
        OffsetDateTime deletedAt,
        long rowVersion
) {
    public static AttachmentResponse from(AttachmentEntity entity, ObjectMapper objectMapper) {
        Map<String, Object> metadata = Collections.emptyMap();
        if (entity.getMetadataJson() != null && !entity.getMetadataJson().isBlank()) {
            try {
                metadata = objectMapper.readValue(entity.getMetadataJson(), new TypeReference<>() { });
            } catch (Exception ignored) {
                metadata = Map.of("_raw", entity.getMetadataJson());
            }
        }

        return new AttachmentResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getContactId(),
                entity.getEstimateId(),
                entity.getParentAttachmentId(),
                entity.getCategory(),
                entity.getFileKind(),
                entity.getStatus(),
                entity.getOriginalFileName(),
                entity.getDisplayName(),
                entity.getContentType(),
                entity.getDeclaredSizeBytes(),
                entity.getActualSizeBytes(),
                entity.getChecksumSha256Base64(),
                entity.getEtag(),
                entity.getSourceSystem(),
                entity.getDescription(),
                metadata,
                entity.getFailureReason(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUploadedAt(),
                entity.getDeletedAt(),
                entity.getRowVersion()
        );
    }
}
