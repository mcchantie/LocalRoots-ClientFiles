package com.localroots.clientfiles.common;

import org.slf4j.MDC;

/**
 * Central names for request-scoped logging context. Values are intentionally
 * limited to identifiers and never include tokens, passwords, or file URLs.
 */
public final class LoggingContext {

    public static final String CORRELATION_ID = "correlationId";
    public static final String TENANT_ID = "tenant";
    public static final String USER = "user";

    private LoggingContext() {
    }

    public static void putTenant(String tenantId) {
        put(TENANT_ID, tenantId);
    }

    public static void putUser(String user) {
        put(USER, user);
    }

    public static void clearRequestValues() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(TENANT_ID);
        MDC.remove(USER);
    }

    private static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
