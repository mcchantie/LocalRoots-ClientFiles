package com.localroots.clientfiles.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Logs one concise completion line for every request and an optional DEBUG
 * start line. Request bodies, authorization headers, cookies, and presigned
 * URLs are deliberately never logged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final int MAX_QUERY_LENGTH = 500;
    private static final Set<String> SAFE_QUERY_KEYS = Set.of(
            "page",
            "size",
            "status",
            "category",
            "unassigned",
            "includeDeleted",
            "deletedOnly",
            "download"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedNanos = System.nanoTime();
        Throwable failure = null;

        if (log.isDebugEnabled()) {
            log.debug(
                    "HTTP request started method={} path={} query={} contentType={} contentLength={} origin={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    safeQuery(request),
                    valueOrDash(request.getContentType()),
                    request.getContentLengthLong(),
                    valueOrDash(request.getHeader("Origin"))
            );
        }

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            long durationMs = (System.nanoTime() - startedNanos) / 1_000_000;
            int status = failure == null ? response.getStatus() : Math.max(response.getStatus(), 500);
            String message = "HTTP request completed method={} path={} query={} status={} durationMs={} responseType={} failure={}";
            Object[] arguments = {
                    request.getMethod(),
                    request.getRequestURI(),
                    safeQuery(request),
                    status,
                    durationMs,
                    valueOrDash(response.getContentType()),
                    failure == null ? "-" : failure.getClass().getSimpleName()
            };

            if (status >= 500) {
                log.error(message, arguments);
            } else if (status >= 400) {
                log.warn(message, arguments);
            } else {
                log.info(message, arguments);
            }
        }
    }

    private String safeQuery(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        if (parameters == null || parameters.isEmpty()) {
            return "-";
        }

        StringJoiner joiner = new StringJoiner("&");
        new TreeMap<>(parameters).forEach((name, values) -> {
            if (SAFE_QUERY_KEYS.contains(name)) {
                String value = values == null
                        ? ""
                        : String.join(",", Arrays.stream(values)
                                .map(this::truncateValue)
                                .toList());
                joiner.add(name + "=" + value);
            } else {
                joiner.add(name + "=[REDACTED]");
            }
        });

        String result = joiner.toString();
        return result.length() <= MAX_QUERY_LENGTH
                ? result
                : result.substring(0, MAX_QUERY_LENGTH) + "...";
    }

    private String truncateValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("[\r\n]", "_");
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
