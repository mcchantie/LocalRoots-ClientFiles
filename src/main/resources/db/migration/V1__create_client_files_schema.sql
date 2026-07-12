CREATE TABLE IF NOT EXISTS client_contacts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    first_name VARCHAR(120),
    last_name VARCHAR(120),
    display_name VARCHAR(255),
    phone VARCHAR(50),
    normalized_phone VARCHAR(30),
    email VARCHAR(320),
    normalized_email VARCHAR(320),
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_client_contacts_identifier
        CHECK (normalized_phone IS NOT NULL OR normalized_email IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_client_contacts_tenant_phone
    ON client_contacts (tenant_id, normalized_phone)
    WHERE normalized_phone IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_client_contacts_tenant_email
    ON client_contacts (tenant_id, normalized_email)
    WHERE normalized_email IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_client_contacts_tenant_created
    ON client_contacts (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_client_contacts_tenant_display_name
    ON client_contacts (tenant_id, LOWER(display_name));

CREATE TABLE IF NOT EXISTS client_attachments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    contact_id UUID,
    estimate_id UUID,
    parent_attachment_id UUID,
    category VARCHAR(32) NOT NULL,
    file_kind VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    declared_size_bytes BIGINT NOT NULL,
    actual_size_bytes BIGINT,
    checksum_sha256_base64 VARCHAR(128),
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    etag VARCHAR(255),
    source_system VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    metadata_json TEXT,
    failure_reason VARCHAR(1000),
    created_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    uploaded_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_client_attachments_bucket_key UNIQUE (s3_bucket, s3_key),
    CONSTRAINT fk_client_attachments_contact
        FOREIGN KEY (contact_id) REFERENCES client_contacts (id),
    CONSTRAINT fk_client_attachments_parent
        FOREIGN KEY (parent_attachment_id) REFERENCES client_attachments (id)
);

CREATE INDEX IF NOT EXISTS ix_client_attachments_tenant_created
    ON client_attachments (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_client_attachments_tenant_contact_created
    ON client_attachments (tenant_id, contact_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_client_attachments_tenant_category
    ON client_attachments (tenant_id, category);

CREATE INDEX IF NOT EXISTS ix_client_attachments_tenant_status
    ON client_attachments (tenant_id, status);

CREATE INDEX IF NOT EXISTS ix_client_attachments_active
    ON client_attachments (tenant_id, deleted_at)
    WHERE deleted_at IS NULL;
