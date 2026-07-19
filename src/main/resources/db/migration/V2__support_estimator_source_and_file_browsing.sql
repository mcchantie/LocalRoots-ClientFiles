-- Adds the Estimator as a trusted attachment source and indexes used by file search/sorting.

ALTER TABLE contact_attachments
    DROP CONSTRAINT IF EXISTS contact_attachments_source_system_check;

ALTER TABLE contact_attachments
    ADD CONSTRAINT contact_attachments_source_system_check
    CHECK (source_system IN (
        'QUO', 'FACEBOOK', 'JOBBER', 'WEBSITE', 'GMAIL', 'MANUAL', 'ESTIMATOR'
    ));

CREATE INDEX IF NOT EXISTS ix_contact_attachments_tenant_display_name
    ON contact_attachments (tenant_id, lower(display_name));

CREATE INDEX IF NOT EXISTS ix_contact_attachments_tenant_file_name
    ON contact_attachments (tenant_id, lower(file_name));

CREATE INDEX IF NOT EXISTS ix_contact_attachments_tenant_updated
    ON contact_attachments (tenant_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS ix_contact_attachments_tenant_size
    ON contact_attachments (tenant_id, file_size);
