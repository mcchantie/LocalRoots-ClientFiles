package com.localroots.clientfiles.contact;

import com.localroots.clientfiles.api.PageResponse;
import com.localroots.clientfiles.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class ContactService {

    private final ContactRepository repository;

    public ContactService(ContactRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ContactResponse create(UUID tenantId, ContactRequest request) {
        Values values = validateAndNormalize(request);
        rejectDuplicates(tenantId, values, null);

        ContactEntity entity = ContactEntity.create(
                UUID.randomUUID(),
                tenantId,
                values.firstName(),
                values.lastName(),
                values.displayName(),
                values.phone(),
                values.normalizedPhone(),
                values.email(),
                values.normalizedEmail(),
                values.notes()
        );
        return ContactResponse.from(repository.save(entity));
    }

    @Transactional
    public ContactResponse update(UUID tenantId, UUID contactId, ContactRequest request) {
        ContactEntity entity = requireContact(tenantId, contactId);
        Values values = validateAndNormalize(request);
        rejectDuplicates(tenantId, values, entity);
        entity.update(
                values.firstName(),
                values.lastName(),
                values.displayName(),
                values.phone(),
                values.normalizedPhone(),
                values.email(),
                values.normalizedEmail(),
                values.notes()
        );
        return ContactResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public ContactResponse get(UUID tenantId, UUID contactId) {
        return ContactResponse.from(requireContact(tenantId, contactId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> list(UUID tenantId, String search, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Specification<ContactEntity> specification = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        String term = blankToNull(search);
        if (term != null) {
            String like = "%" + term.toLowerCase(Locale.ROOT) + "%";
            String phoneDigits = normalizePhone(term);
            specification = specification.and((root, query, cb) -> {
                var textMatch = cb.or(
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phone")), like)
                );
                if (phoneDigits == null) {
                    return textMatch;
                }
                return cb.or(textMatch, cb.like(root.get("normalizedPhone"), "%" + phoneDigits + "%"));
            });
        }

        Page<ContactResponse> response = repository.findAll(
                        specification,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "firstName")
                                .and(Sort.by(Sort.Direction.ASC, "lastName"))
                                .and(Sort.by(Sort.Direction.ASC, "phone"))
                                .and(Sort.by(Sort.Direction.ASC, "email"))
                                .and(Sort.by(Sort.Direction.DESC, "createdAt")))
                )
                .map(ContactResponse::from);
        return PageResponse.from(response);
    }

    public boolean belongsToTenant(UUID tenantId, UUID contactId) {
        return repository.existsByIdAndTenantId(contactId, tenantId);
    }

    private ContactEntity requireContact(UUID tenantId, UUID contactId) {
        return repository.findByIdAndTenantId(contactId, tenantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Contact not found",
                        "No contact was found for this tenant."
                ));
    }

    private Values validateAndNormalize(ContactRequest request) {
        String phone = blankToNull(request.phone());
        String email = blankToNull(request.email());
        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = email == null ? null : email.toLowerCase(Locale.ROOT);

        if (normalizedPhone == null && normalizedEmail == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Contact needs an identifier",
                    "Enter at least a phone number or email address. A contact name is optional."
            );
        }

        String firstName = blankToNull(request.firstName());
        String lastName = blankToNull(request.lastName());
        String displayName = blankToNull(request.displayName());
        if (displayName == null) {
            String combinedName = ((firstName == null ? "" : firstName) + " "
                    + (lastName == null ? "" : lastName)).trim();
            displayName = combinedName.isBlank() ? null : combinedName;
        }

        return new Values(
                firstName,
                lastName,
                displayName,
                phone,
                normalizedPhone,
                email,
                normalizedEmail,
                blankToNull(request.notes())
        );
    }

    private void rejectDuplicates(UUID tenantId, Values values, ContactEntity current) {
        if (values.normalizedPhone() != null
                && repository.existsByTenantIdAndNormalizedPhone(tenantId, values.normalizedPhone())
                && (current == null || !values.normalizedPhone().equals(current.getNormalizedPhone()))) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number already exists", "A contact already uses this phone number.");
        }
        if (values.normalizedEmail() != null
                && repository.existsByTenantIdAndNormalizedEmail(tenantId, values.normalizedEmail())
                && (current == null || !values.normalizedEmail().equals(current.getNormalizedEmail()))) {
            throw new ApiException(HttpStatus.CONFLICT, "Email address already exists", "A contact already uses this email address.");
        }
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Values(
            String firstName,
            String lastName,
            String displayName,
            String phone,
            String normalizedPhone,
            String email,
            String normalizedEmail,
            String notes
    ) {
    }
}
