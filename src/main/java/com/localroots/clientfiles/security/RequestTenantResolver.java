package com.localroots.clientfiles.security;

import com.localroots.clientfiles.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestTenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final ClientFilesSecurityProperties properties;

    public RequestTenantResolver(ClientFilesSecurityProperties properties) {
        this.properties = properties;
    }

    public UUID requireTenantId(HttpServletRequest request) {
        if (!properties.isAllowTenantHeader()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tenant authentication is not configured",
                    "Configure authenticated tenant resolution before enabling Client Files outside development."
            );
        }

        String value = request.getHeader(TENANT_HEADER);
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Tenant is required",
                    "Send the X-Tenant-Id header while the local development tenant bridge is enabled."
            );
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid tenant header", "X-Tenant-Id must be a UUID.");
        }
    }
}
