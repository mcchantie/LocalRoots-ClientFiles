package com.localroots.clientfiles.api;

import com.localroots.clientfiles.security.AuthService;
import com.localroots.clientfiles.security.JwtService;
import com.localroots.clientfiles.tenant.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TenantService tenantService;

    public AuthController(AuthService authService, TenantService tenantService) {
        this.authService = authService;
        this.tenantService = tenantService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return new LoginResponse(
                "Bearer",
                result.accessToken(),
                result.expiresAt(),
                result.username(),
                result.tenantId(),
                result.tenantName()
        );
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = UUID.fromString(jwt.getClaimAsString(JwtService.TENANT_ID_CLAIM));
        String tenantName = tenantService.requireTenant(tenantId).getName();
        return new CurrentUserResponse(
                jwt.getSubject(),
                tenantId,
                tenantName,
                jwt.getExpiresAt()
        );
    }
}
