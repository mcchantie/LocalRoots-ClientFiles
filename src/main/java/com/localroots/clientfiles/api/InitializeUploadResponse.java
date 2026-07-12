package com.localroots.clientfiles.api;

import com.localroots.clientfiles.attachment.AttachmentStatus;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record InitializeUploadResponse(
        UUID attachmentId,
        AttachmentStatus status,
        String method,
        URI uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
