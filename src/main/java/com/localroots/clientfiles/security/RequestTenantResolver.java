package com.localroots.clientfiles.security;

import com.localroots.clientfiles.common.ApiException;
import com.localroots.clientfiles.common.LoggingContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String claim = jwt.getClaimAsString(JwtService.TENANT_ID_CLAIM);
            if (claim == null || claim.isBlank()) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Tenant claim is missing",
                        "The access token does not contain a tenant identifier."
                );
            }
            try {
                UUID tenantId = UUID.fromString(claim);
                LoggingContext.putTenant(tenantId.toString());
                LoggingContext.putUser(jwt.getSubject());
                return tenantId;
            } catch (IllegalArgumentException exception) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Tenant claim is invalid",
                        "The access token contains an invalid tenant identifier."
                );
            }
        }

        if (properties.isAllowTenantHeader()) {
            return parseDevelopmentTenantHeader(request);
        }

        throw new ApiException(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "Send a valid Bearer token."
        );
    }

    private UUID parseDevelopmentTenantHeader(HttpServletRequest request) {
        String value = request.getHeader(TENANT_HEADER);
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Tenant is required",
                    "Send the X-Tenant-Id header while the local development tenant bridge is enabled."
            );
        }

        try {
            UUID tenantId = UUID.fromString(value.trim());
            LoggingContext.putTenant(tenantId.toString());
            LoggingContext.putUser("development-tenant-header");
            return tenantId;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid tenant header", "X-Tenant-Id must be a UUID.");
        }
    }
}
