package com.localroots.clientfiles.attachment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.localroots.clientfiles.api.AttachmentResponse;
import com.localroots.clientfiles.api.DownloadUrlResponse;
import com.localroots.clientfiles.api.InitializeUploadRequest;
import com.localroots.clientfiles.api.InitializeUploadResponse;
import com.localroots.clientfiles.api.PageResponse;
import com.localroots.clientfiles.common.ApiException;
import com.localroots.clientfiles.common.UploadVerificationException;
import com.localroots.clientfiles.contact.ContactService;
import com.localroots.clientfiles.security.ClientFilesSecurityProperties;
import com.localroots.clientfiles.storage.S3StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository repository;
    private final S3StorageService storageService;
    private final ClientFilesSecurityProperties securityProperties;
    private final ContactService contactService;
    private final ObjectMapper objectMapper;

    public AttachmentService(
            AttachmentRepository repository,
            S3StorageService storageService,
            ClientFilesSecurityProperties securityProperties,
            ContactService contactService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.storageService = storageService;
        this.securityProperties = securityProperties;
        this.contactService = contactService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InitializeUploadResponse initializeUpload(UUID tenantId, InitializeUploadRequest request) {
        validateRelatedRecords(tenantId, request.contactId(), request.estimateId(), request.parentAttachmentId());

        String contentType = storageService.normalizeContentType(request.contentType());
        validateContentType(contentType);
        validateFileSize(request.sizeBytes());

        AttachmentFileKind fileKind = inferFileKind(contentType);
        validateCategory(request.category(), fileKind);

        UUID attachmentId = UUID.randomUUID();
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? request.originalFileName().trim()
                : request.displayName().trim();
        String sourceSystem = request.sourceSystem() == null || request.sourceSystem().isBlank()
                ? "manual"
                : request.sourceSystem().trim().toLowerCase(Locale.ROOT);

        String metadataJson = toJson(request.metadata());
        String key = storageService.buildAttachmentKey(
                tenantId,
                request.contactId(),
                attachmentId,
                request.category(),
                request.originalFileName()
        );

        AttachmentEntity entity = AttachmentEntity.pending(
                attachmentId,
                tenantId,
                request.contactId(),
                request.estimateId(),
                request.parentAttachmentId(),
                request.category(),
                fileKind,
                request.originalFileName().trim(),
                displayName,
                contentType,
                request.sizeBytes(),
                blankToNull(request.checksumSha256Base64()),
                storageService.bucket(),
                key,
                sourceSystem,
                blankToNull(request.description()),
                metadataJson,
                null
        );

        repository.save(entity);
        S3StorageService.PresignedUpload upload = storageService.presignUpload(
                entity.getS3Key(),
                entity.getContentType(),
                entity.getChecksumSha256Base64()
        );

        return new InitializeUploadResponse(
                entity.getId(),
                entity.getStatus(),
                "PUT",
                upload.url(),
                upload.expiresAt(),
                upload.requiredHeaders()
        );
    }

    @Transactional(noRollbackFor = UploadVerificationException.class)
    public AttachmentResponse completeUpload(UUID tenantId, UUID attachmentId) {
        AttachmentEntity entity = requireAttachment(tenantId, attachmentId);
        requireNotDeleted(entity);

        if (entity.getStatus() == AttachmentStatus.READY) {
            return AttachmentResponse.from(entity, objectMapper);
        }
        if (entity.getStatus() != AttachmentStatus.PENDING_UPLOAD) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Upload cannot be completed",
                    "Only pending uploads can be completed. Current status: " + entity.getStatus()
            );
        }

        assertStorageLocation(entity, tenantId);
        S3StorageService.UploadedObject uploaded = storageService.headObject(
                entity.getS3Key(),
                entity.getChecksumSha256Base64() != null
        );

        verifyUploadedObject(entity, uploaded);
        entity.markReady(uploaded.sizeBytes(), uploaded.etag());
        return AttachmentResponse.from(entity, objectMapper);
    }

    @Transactional(readOnly = true)
    public AttachmentResponse get(UUID tenantId, UUID attachmentId, boolean includeDeleted) {
        AttachmentEntity entity = requireAttachment(tenantId, attachmentId);
        if (!includeDeleted) {
            requireNotDeleted(entity);
        }
        return AttachmentResponse.from(entity, objectMapper);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttachmentResponse> list(
            UUID tenantId,
            UUID contactId,
            AttachmentCategory category,
            AttachmentStatus status,
            boolean unassigned,
            boolean includeDeleted,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Specification<AttachmentEntity> specification = tenantSpecification(tenantId);
        if (contactId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("contactId"), contactId));
        } else if (unassigned) {
            specification = specification.and((root, query, cb) -> cb.isNull(root.get("contactId")));
        }
        if (category != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (!includeDeleted) {
            specification = specification.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));
        }

        Page<AttachmentResponse> responsePage = repository.findAll(
                        specification,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(entity -> AttachmentResponse.from(entity, objectMapper));
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse createDownloadUrl(UUID tenantId, UUID attachmentId, boolean download) {
        AttachmentEntity entity = requireAttachment(tenantId, attachmentId);
        requireNotDeleted(entity);

        if (entity.getStatus() != AttachmentStatus.READY && entity.getStatus() != AttachmentStatus.UPLOADED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "File is not ready",
                    "A download URL is only available after the upload has been verified."
            );
        }

        assertStorageLocation(entity, tenantId);
        S3StorageService.PresignedDownload presigned = storageService.presignDownload(
                entity.getS3Key(),
                entity.getDisplayName(),
                download
        );
        return new DownloadUrlResponse(entity.getId(), presigned.url(), presigned.expiresAt());
    }

    @Transactional
    public AttachmentResponse softDelete(UUID tenantId, UUID attachmentId) {
        AttachmentEntity entity = requireAttachment(tenantId, attachmentId);
        entity.softDelete();
        return AttachmentResponse.from(entity, objectMapper);
    }

    @Transactional
    public AttachmentResponse restore(UUID tenantId, UUID attachmentId) {
        AttachmentEntity entity = requireAttachment(tenantId, attachmentId);
        entity.restore();
        return AttachmentResponse.from(entity, objectMapper);
    }

    @Transactional
    public AttachmentResponse assignToContact(UUID tenantId, UUID attachmentId, UUID contactId) {
        AttachmentEntity entity = requireAttachment(tenantId, attachmentId);
        requireNotDeleted(entity);
        validateRelatedRecords(tenantId, contactId, null, null);
        entity.assignContact(contactId);
        return AttachmentResponse.from(entity, objectMapper);
    }

    private AttachmentEntity requireAttachment(UUID tenantId, UUID attachmentId) {
        return repository.findByIdAndTenantId(attachmentId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found", "No attachment was found for this tenant."));
    }

    private void requireNotDeleted(AttachmentEntity entity) {
        if (entity.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment not found", "The attachment has been deleted.");
        }
    }

    private Specification<AttachmentEntity> tenantSpecification(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    private void validateRelatedRecords(UUID tenantId, UUID contactId, UUID estimateId, UUID parentAttachmentId) {
        if (contactId != null
                && !contactService.belongsToTenant(tenantId, contactId)
                && !securityProperties.isAllowUnverifiedContactIds()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Contact not found",
                    "The selected contact does not exist for this tenant."
            );
        }
        if (estimateId != null && !securityProperties.isAllowUnverifiedEstimateIds()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Estimate verification is not configured",
                    "Estimate-linked uploads are disabled until estimate ownership can be verified against the CRM database."
            );
        }
        if (parentAttachmentId != null) {
            AttachmentEntity parent = requireAttachment(tenantId, parentAttachmentId);
            requireNotDeleted(parent);
            if (parent.getStatus() != AttachmentStatus.READY && parent.getStatus() != AttachmentStatus.UPLOADED) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Parent attachment is not ready",
                        "A derivative can only be linked to an uploaded parent attachment."
                );
            }
        }
    }

    private void assertStorageLocation(AttachmentEntity entity, UUID tenantId) {
        if (!Objects.equals(entity.getS3Bucket(), storageService.bucket())) {
            throw new IllegalStateException("Stored attachment bucket does not match the configured Client Files bucket.");
        }
        storageService.assertKeyBelongsToTenant(entity.getS3Key(), tenantId);
    }

    private void validateContentType(String contentType) {
        if (!storageService.isAllowedContentType(contentType)) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type",
                    "The content type is not allowed: " + contentType
            );
        }
    }

    private void validateFileSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file size", "File size must be greater than zero.");
        }
        if (sizeBytes > storageService.maxFileSizeBytes()) {
            throw new ApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File is too large",
                    "The file exceeds the configured maximum size of " + storageService.maxFileSizeBytes() + " bytes."
            );
        }
    }

    private AttachmentFileKind inferFileKind(String contentType) {
        if (contentType.startsWith("image/")) {
            return AttachmentFileKind.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return AttachmentFileKind.VIDEO;
        }
        if (contentType.startsWith("application/") || contentType.startsWith("text/")) {
            return AttachmentFileKind.DOCUMENT;
        }
        return AttachmentFileKind.OTHER;
    }

    private void validateCategory(AttachmentCategory category, AttachmentFileKind fileKind) {
        boolean valid = switch (category) {
            case LANDGLIDE, PROPERTY_PHOTOS -> fileKind == AttachmentFileKind.IMAGE;
            case VIDEOS -> fileKind == AttachmentFileKind.VIDEO;
            case ESTIMATES, DOCUMENTS -> fileKind == AttachmentFileKind.DOCUMENT;
            case OTHER -> true;
        };

        if (!valid) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "File does not match category",
                    "The selected category is not compatible with the supplied content type."
            );
        }
    }

    private void verifyUploadedObject(AttachmentEntity entity, S3StorageService.UploadedObject uploaded) {
        if (uploaded.sizeBytes() != entity.getDeclaredSizeBytes()) {
            failVerification(entity, "Uploaded size does not match the declared size.");
        }
        if (uploaded.sizeBytes() > storageService.maxFileSizeBytes()) {
            failVerification(entity, "Uploaded file exceeds the configured size limit.");
        }
        if (!Objects.equals(uploaded.contentType(), entity.getContentType())) {
            failVerification(entity, "Uploaded content type does not match the initialized upload.");
        }
        if (entity.getChecksumSha256Base64() != null
                && !Objects.equals(entity.getChecksumSha256Base64(), uploaded.checksumSha256Base64())) {
            failVerification(entity, "Uploaded SHA-256 checksum does not match.");
        }
    }

    private void failVerification(AttachmentEntity entity, String reason) {
        entity.markFailed(reason);
        repository.save(entity);
        throw new UploadVerificationException(reason);
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid metadata", "Metadata could not be serialized as JSON.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
