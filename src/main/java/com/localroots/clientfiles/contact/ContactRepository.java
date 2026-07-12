package com.localroots.clientfiles.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<ContactEntity, UUID>, JpaSpecificationExecutor<ContactEntity> {
    Optional<ContactEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndNormalizedPhone(UUID tenantId, String normalizedPhone);
    boolean existsByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);
}
