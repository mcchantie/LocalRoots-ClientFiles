package com.localroots.clientfiles.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @Size(max = 120) String firstName,
        @Size(max = 120) String lastName,
        @Size(max = 255) String displayName,
        @Size(max = 50) String phone,
        @Email @Size(max = 320) String email,
        @Size(max = 2000) String notes
) {
}
