package com.localroots.clientfiles.security;

import com.localroots.clientfiles.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String encodedPassword;
    private final UUID tenantId;

    public AuthService(
            AuthenticationProperties properties,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.encodedPassword = passwordEncoder.encode(properties.getAdminPassword() == null ? "" : properties.getAdminPassword());
        this.tenantId = parseTenantId(properties.getTenantId());
    }

    public LoginResult login(String username, String password) {
        boolean usernameValid = usernameMatches(username);
        boolean passwordValid = passwordEncoder.matches(password, encodedPassword);
        if (!usernameValid || !passwordValid) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials",
                    "The username or password is incorrect."
            );
        }

        JwtService.IssuedToken token = jwtService.issue(properties.getAdminUsername().trim(), tenantId);
        return new LoginResult(
                token.value(),
                token.expiresAt(),
                properties.getAdminUsername().trim(),
                tenantId
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
            return new UUID(0, 0);
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return new UUID(0, 0);
        }
    }

    public record LoginResult(
            String accessToken,
            java.time.Instant expiresAt,
            String username,
            UUID tenantId
    ) {
    }
}
