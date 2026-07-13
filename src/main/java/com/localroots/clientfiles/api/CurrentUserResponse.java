package com.localroots.clientfiles.api;

import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
        String username,
        UUID tenantId,
        String tenantName,
        Instant expiresAt
) {
}
