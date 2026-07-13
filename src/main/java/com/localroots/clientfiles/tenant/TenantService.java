package com.localroots.clientfiles.tenant;

import com.localroots.clientfiles.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository repository;

    public TenantService(TenantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TenantEntity requireTenant(UUID tenantId) {
        return repository.findById(tenantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Tenant is not configured",
                        "CLIENT_FILES_TENANT_ID does not match a tenant in the Local Roots database."
                ));
    }
}
