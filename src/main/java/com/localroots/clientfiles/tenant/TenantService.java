package com.localroots.clientfiles.tenant;

import com.localroots.clientfiles.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository repository;

    public TenantService(TenantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TenantEntity requireTenant(UUID tenantId) {
        log.debug("Loading tenant tenantId={}", tenantId);
        return repository.findById(tenantId)
                .orElseThrow(() -> {
                    log.error("Configured tenant was not found tenantId={}", tenantId);
                    return new ApiException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Tenant is not configured",
                            "CLIENT_FILES_TENANT_ID does not match a tenant in the Local Roots database."
                    );
                });
    }
}
