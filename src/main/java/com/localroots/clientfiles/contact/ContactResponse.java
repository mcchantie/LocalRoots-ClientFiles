package com.localroots.clientfiles.contact;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        String firstName,
        String lastName,
        String displayName,
        String label,
        String phone,
        String email,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long rowVersion
) {
    public static ContactResponse from(ContactEntity entity) {
        return new ContactResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDisplayName(),
                bestDisplayName(entity),
                entity.getPhone(),
                entity.getEmail(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getRowVersion()
        );
    }

    private static String bestDisplayName(ContactEntity entity) {
        if (hasText(entity.getDisplayName())) {
            return entity.getDisplayName();
        }
        String combined = ((entity.getFirstName() == null ? "" : entity.getFirstName()) + " "
                + (entity.getLastName() == null ? "" : entity.getLastName())).trim();
        if (!combined.isBlank()) {
            return combined;
        }
        if (hasText(entity.getPhone())) {
            return entity.getPhone();
        }
        if (hasText(entity.getEmail())) {
            return entity.getEmail();
        }
        return "Unnamed contact";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
