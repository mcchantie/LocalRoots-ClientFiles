package com.localroots.clientfiles.api;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        String username,
        UUID tenantId
) {
}
