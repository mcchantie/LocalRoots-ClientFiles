package com.localroots.clientfiles.config;

import com.localroots.clientfiles.security.AuthenticationProperties;
import com.localroots.clientfiles.security.ClientFilesSecurityProperties;
import com.localroots.clientfiles.storage.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Emits a safe startup summary. Secret values and credentials are never
 * included, but the settings most likely to cause integration failures are.
 */
@Component
public class StartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);
    private static final Pattern PASSWORD_PARAMETER = Pattern.compile("(?i)(password=)[^&;]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern JDBC_USER_INFO = Pattern.compile("(//)[^/@:]+:[^/@]+@");

    private final Environment environment;
    private final S3StorageService storageService;
    private final CorsProperties corsProperties;
    private final AuthenticationProperties authenticationProperties;
    private final ClientFilesSecurityProperties securityProperties;

    public StartupDiagnostics(
            Environment environment,
            S3StorageService storageService,
            CorsProperties corsProperties,
            AuthenticationProperties authenticationProperties,
            ClientFilesSecurityProperties securityProperties
    ) {
        this.environment = environment;
        this.storageService = storageService;
        this.corsProperties = corsProperties;
        this.authenticationProperties = authenticationProperties;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Client Files started profiles={} port={} datasource={} s3Region={} s3Bucket={} maxFileSizeBytes={} uploadUrlTtl={} downloadUrlTtl={} tenantId={} jwtIssuer={} allowedOrigins={} allowedOriginPatterns={}",
                activeProfiles(),
                environment.getProperty("server.port", "8080"),
                safeDatasourceUrl(environment.getProperty("spring.datasource.url")),
                environment.getProperty("storage.s3.region", "-"),
                storageService.bucket(),
                storageService.maxFileSizeBytes(),
                storageService.uploadUrlTtl(),
                storageService.downloadUrlTtl(),
                valueOrDash(authenticationProperties.getTenantId()),
                valueOrDash(authenticationProperties.getJwtIssuer()),
                corsProperties.getAllowedOrigins(),
                corsProperties.getAllowedOriginPatterns()
        );

        log.info(
                "Client Files security bridges allowTenantHeader={} allowUnverifiedContactIds={} allowUnverifiedEstimateIds={}",
                securityProperties.isAllowTenantHeader(),
                securityProperties.isAllowUnverifiedContactIds(),
                securityProperties.isAllowUnverifiedEstimateIds()
        );

        if (securityProperties.isAllowTenantHeader()
                || securityProperties.isAllowUnverifiedContactIds()
                || securityProperties.isAllowUnverifiedEstimateIds()) {
            log.warn("One or more development-only security bridges are enabled. Confirm they are disabled before production use.");
        }
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "default" : Arrays.toString(profiles);
    }

    private String safeDatasourceUrl(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String withoutUserInfo = JDBC_USER_INFO.matcher(value).replaceAll("$1[REDACTED]@");
        return PASSWORD_PARAMETER.matcher(withoutUserInfo).replaceAll("$1[REDACTED]");
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
