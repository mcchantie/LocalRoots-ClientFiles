package com.localroots.clientfiles.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "contacts")
public class ContactEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(length = 255)
    private String phone;

    @Column(name = "normalized_phone", length = 30)
    private String normalizedPhone;

    @Column(length = 320)
    private String email;

    @Column(name = "normalized_email", length = 320)
    private String normalizedEmail;

    @Column(name = "internal_notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected ContactEntity() {
    }

    public static ContactEntity create(
            UUID id,
            UUID tenantId,
            String firstName,
            String lastName,
            String displayName,
            String phone,
            String normalizedPhone,
            String email,
            String normalizedEmail,
            String notes
    ) {
        ContactEntity contact = new ContactEntity();
        contact.id = id;
        contact.tenantId = tenantId;
        contact.update(firstName, lastName, displayName, phone, normalizedPhone, email, normalizedEmail, notes);
        return contact;
    }

    public void update(
            String firstName,
            String lastName,
            String displayName,
            String phone,
            String normalizedPhone,
            String email,
            String normalizedEmail,
            String notes
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.notes = notes;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public String getNormalizedPhone() { return normalizedPhone; }
    public String getEmail() { return email; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public long getRowVersion() { return rowVersion; }
}
