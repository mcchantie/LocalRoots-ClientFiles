package com.localroots.clientfiles.common;

import com.localroots.clientfiles.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Adds authenticated user and tenant identifiers to SLF4J MDC so every log
 * line produced while handling the request can be correlated safely.
 */
@Component
public class LoggingContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            LoggingContext.putUser(jwt.getSubject());
            LoggingContext.putTenant(jwt.getClaimAsString(JwtService.TENANT_ID_CLAIM));
        } else {
            LoggingContext.putUser(authentication.getName());
        }

        return true;
    }
}
