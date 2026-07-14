package com.localroots.clientfiles.security;

import com.localroots.clientfiles.common.ApiException;
import com.localroots.clientfiles.tenant.TenantEntity;
import com.localroots.clientfiles.tenant.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantService tenantService;
    private final String encodedPassword;
    private final UUID tenantId;

    public AuthService(
            AuthenticationProperties properties,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TenantService tenantService
    ) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tenantService = tenantService;
        this.encodedPassword = passwordEncoder.encode(properties.getAdminPassword() == null ? "" : properties.getAdminPassword());
        this.tenantId = parseTenantId(properties.getTenantId());
    }

    public LoginResult login(String username, String password) {
        String attemptedUsername = username == null ? "-" : username.trim();
        log.info("Authentication attempt username={}", attemptedUsername);
        boolean usernameValid = usernameMatches(username);
        boolean passwordValid = passwordEncoder.matches(password, encodedPassword);
        if (!usernameValid || !passwordValid) {
            log.warn("Authentication failed username={} usernameMatched={}", attemptedUsername, usernameValid);
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials",
                    "The username or password is incorrect."
            );
        }

        TenantEntity tenant = tenantService.requireTenant(tenantId);
        JwtService.IssuedToken token = jwtService.issue(properties.getAdminUsername().trim(), tenantId);
        log.info(
                "Authentication succeeded username={} tenantId={} tokenExpiresAt={}",
                properties.getAdminUsername().trim(),
                tenantId,
                token.expiresAt()
        );
        return new LoginResult(
                token.value(),
                token.expiresAt(),
                properties.getAdminUsername().trim(),
                tenantId,
                tenant.getName()
        );
    }

    private boolean usernameMatches(String suppliedUsername) {
        if (suppliedUsername == null || properties.getAdminUsername() == null) {
            return false;
        }
        byte[] supplied = suppliedUsername.trim().getBytes(StandardCharsets.UTF_8);
        byte[] expected = properties.getAdminUsername().trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(supplied, expected);
    }

    private UUID parseTenantId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("CLIENT_FILES_TENANT_ID must be configured with an existing Local Roots tenant UUID.");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("CLIENT_FILES_TENANT_ID must be a valid UUID.", exception);
        }
    }

    public record LoginResult(
            String accessToken,
            java.time.Instant expiresAt,
            String username,
            UUID tenantId,
            String tenantName
    ) {
    }
}
