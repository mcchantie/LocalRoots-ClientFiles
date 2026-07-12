package com.localroots.clientfiles.api;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record DownloadUrlResponse(
        UUID attachmentId,
        URI url,
        Instant expiresAt
) {
}
