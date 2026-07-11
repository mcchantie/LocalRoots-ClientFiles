# Client Files Architecture Decisions

This document records durable architectural and product decisions for the Local Roots CRM Client Files application. Session-specific discussion belongs in `client-files-development-log.md`.

## ADR-001: Product Name

**Status:** Accepted

The client attachment application will be called **Client Files**.

The pricing calculator will be called **Estimater**.

### Rationale

“Client Files” clearly describes a place for documents, photos, videos, estimates, and property-related media without limiting the tool to a single file type.

---

## ADR-002: Initial Environment Strategy

**Status:** Accepted

The initial environment model will use:

- Development
- Production

UAT will be introduced later when a stable pre-production release-testing environment is needed.

### Rationale

Real business records must be protected from destructive development activity. An environment becomes production because the data is operationally important, not because the software is feature-complete.

### Consequences

- Development and production must have separate databases.
- Development and production must have separate S3 buckets or fully isolated AWS resources.
- Development and production must have separate credentials and configuration.
- Test scripts and database resets must never target production.
- The frontend should clearly identify the current environment.

---

## ADR-003: Production Data Is the Operational Source of Truth

**Status:** Accepted

Real client records and files will be created and maintained in production.

Development data will not be promoted into production through automatic data synchronization.

### Rationale

Development is intentionally destructive and may contain incomplete or invalid test data.

### Consequences

- Recovery will use database backups, point-in-time recovery, S3 versioning, soft deletion, and controlled restore procedures.
- Production data may be copied downward into development only through a controlled and preferably sanitized process.
- Development should also maintain reusable fake seed data.

---

## ADR-004: S3 Stores Files; PostgreSQL Stores Metadata

**Status:** Accepted

Amazon S3 will store file objects. The Local Roots PostgreSQL database will store metadata, search fields, processing state, ownership, tenant relationships, and business relationships.

### Rationale

S3 is better suited for binary objects, large files, durability, and direct browser uploads. PostgreSQL is better suited for relationships, filtering, auditing, and analytics. The database provider may change without changing this storage boundary.

### Consequences

The database should store fields such as:

- Tenant
- Contact
- Estimate
- Upload session
- Parent attachment
- Category
- File kind
- Original and display names
- S3 bucket and key
- MIME type
- File size
- Checksum
- Processing status
- Capture and upload timestamps
- Uploader
- Soft-deletion state
- Flexible metadata JSON

File bytes will not be stored in PostgreSQL.

---

## ADR-005: Database Metadata Controls Organization

**Status:** Accepted

S3 prefixes may reflect tenant, contact, attachment, original, processed, and thumbnail paths, but database metadata will be the primary source of organization and search.

### Rationale

S3 folders are key prefixes rather than a relational filing system. Database categories and relationships are easier to search, change, and secure.

---

## ADR-006: Contact Assignment Supports Three Paths

**Status:** Accepted

An upload may be:

1. Connected to an existing contact
2. Connected while creating a new contact
3. Temporarily left unassigned

### Rationale

Fast intake should not force duplicate contacts or slow the user when contact matching cannot be completed immediately.

### Consequences

An upload-session or unassigned-upload model may be required. Attachments may temporarily have no `contact_id`.

---

## ADR-007: Standard Attachment Categories

**Status:** Accepted

The initial visible categories will include:

- Estimates
- LandGlide
- Property Photos
- Videos
- Documents
- Other

### Rationale

These categories reflect current business workflows while remaining broad enough for future use.

---

## ADR-008: LandGlide Originals and Derivatives Are Both Preserved

**Status:** Accepted

The original LandGlide screenshot will be retained. Timestamp- or status-bar-cropped images will be stored as separate processed derivatives linked to the original.

### Rationale

Automatic cropping can remove useful map details. Preserving the original protects against processing mistakes and allows reprocessing.

### Consequences

- Attachments need parent-child or derivative relationships.
- Processing status must be tracked.
- The interface should distinguish original and processed versions.
- A manual crop adjustment may be added.

---

## ADR-009: No Quo-Specific Attachment Action in the Initial Release

**Status:** Accepted

Client Files will not initially include a custom “Use in message” workflow.

The interface will instead support normal browser interactions such as opening, dragging, copying, pasting, and downloading images.

### Rationale

The browser may already provide the needed workflow, avoiding unnecessary Quo-specific development.

### Validation Needed

The workflow must be tested with the actual Quo web application.

---

## ADR-010: Direct-to-S3 Uploads

**Status:** Accepted

The frontend will request an upload from Spring Boot, receive a presigned URL, upload directly to S3, and then confirm completion with the backend.

### Rationale

This avoids routing large photos and videos through the Spring Boot service and prevents permanent AWS credentials from reaching the browser.

### Consequences

Attachment states should include at least:

- Pending or uploading
- Uploaded
- Processing
- Ready
- Failed

Large videos may later require multipart uploads.

---

## ADR-011: Asynchronous File Processing

**Status:** Accepted

Thumbnail generation and LandGlide cropping will be modeled as processing jobs rather than long-running synchronous upload requests.

### Initial Implementation

Processing may run in the Spring Boot application using a task executor.

### Future Options

- AWS Lambda triggered by S3 events
- SQS-backed worker
- Separate Spring Boot worker service

---

## ADR-012: Estimate Documents and Structured Data Are Both Required

**Status:** Accepted

Estimater will store:

- Structured estimate records in PostgreSQL
- Structured line items
- Calculator input and output snapshots
- Pricing-rule version
- Generated estimate document in S3
- A `contact_attachments` record linked to the estimate

### Rationale

A PDF records what the client received, but structured data is required for analytics, filtering, pricing evaluation, and future automation.

### Important Structured Fields

- Lawn area
- Front, back, and side measurements where available
- Turf type
- Application tier
- Recommended and quoted cubic yards
- Material composition
- Selected services
- Discounts
- Subtotal and total
- Estimate status
- Pricing version

---

## ADR-013: Estimate Records Are the Source of Truth

**Status:** Accepted

The structured estimate record will be the source of truth. The generated PDF will be an immutable client-facing snapshot.

### Consequences

- Revisions should create versions rather than overwrite earlier presentations.
- The approved or sent version must remain identifiable.
- Analytics should query structured fields rather than parse PDFs.

---

## ADR-014: Tenant Isolation Is Required at Every Layer

**Status:** Accepted

Every attachment-related record and action must enforce tenant isolation.

### Required Controls

- Authenticated user belongs to the tenant.
- Contact belongs to the same tenant.
- Estimate belongs to the same tenant.
- Generated S3 keys use the expected tenant prefix.
- Raw S3 keys from the frontend are never trusted as authorization.
- S3 objects remain private.
- File viewing uses authorized backend routes or short-lived signed URLs.

---

## ADR-015: Soft Deletion and Version Protection

**Status:** Accepted

Attachments will use soft deletion in the application. S3 versioning and PostgreSQL backups will protect against accidental loss.

### Rationale

Client files may be operationally important, and destructive actions should be recoverable.

---

## ADR-016: Living Development Documentation

**Status:** Accepted

The project will maintain:

- `client-files-development-log.md`
- `architecture-decisions.md`
- `current-development-status.md`

### Usage

- The development log records each session.
- Architecture decisions records durable choices.
- Current development status provides a concise handoff for the next session.
- At the beginning of a later session, these files can be uploaded or pasted to restore context.

---

## ADR-017: S3 Region and Bucket Security Baseline

**Status:** Accepted

Client Files S3 resources will initially be created in **`us-east-2`** with separate buckets for development and production.

Bucket names will identify the application and environment and include enough account, region, or unique-suffix information to remain globally unique.

### Required Bucket Settings

- Block Public Access enabled
- Object Ownership set to bucket-owner-enforced
- ACLs disabled
- S3 versioning enabled
- Objects private by default

### Rationale

Separate, private, versioned buckets reduce the risk of development activity affecting operational client files and provide recovery options for accidental overwrites or deletions.

---

## ADR-018: Separate Human and Application IAM Access

**Status:** Accepted

Human AWS access and application AWS access will use different IAM identities and policies.

### Application Access

- Use a dedicated application IAM user for each environment while the backend runs locally or on Railway.
- Development and production credentials must remain separate.
- Application policies should be restricted to the matching Client Files bucket and required S3 actions.
- The planned service-user names are:
  - `localroots-client-files-dev-app`
  - `localroots-client-files-prod-app`
- The planned bucket-scoped policy names are:
  - `LocalRootsClientFilesDevS3Policy`
  - `LocalRootsClientFilesProdS3Policy`

### Human Access

The `Developers` IAM group may use `AmazonS3FullAccess` for development-console work. This broad human policy must not be reused as the production application's runtime policy.

### Future Direction

If the backend later runs on AWS infrastructure that supports IAM roles, replace long-lived application access keys with role-based credentials.

---

## ADR-019: AWS Credentials Are Supplied Through the Default Credential Chain

**Status:** Accepted

The Spring Boot application will use the AWS SDK default credential provider chain rather than reading secret keys from custom application properties.

### Local and Railway Configuration

Use environment variables such as:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `CLIENT_FILES_S3_BUCKET`

Bucket and region configuration may be referenced by Spring configuration, but access keys must not be committed to `application.yml`, source control, or the database.

### Verification

Before application testing, verify the active identity and development-bucket access with the AWS CLI, including `aws sts get-caller-identity` and a controlled S3 upload/delete test.

---

## ADR-020: Initial Railway Hosting and Deferred PostgreSQL Migration

**Status:** Accepted; migration deferred

The Local Roots backend will initially continue running on Railway because Texas Top Dressing is currently the only operational tenant.

Amazon S3 can be accessed from the current Railway plan through normal outbound HTTPS using IAM credentials; a static outbound IP is not required for S3 access.

The existing PostgreSQL database is currently hosted in AWS RDS. A migration from AWS RDS to Railway PostgreSQL is planned because connecting a Railway deployment to a tightly restricted external RDS instance is inconvenient without a stable outbound IP.

### Migration Safety Requirements

- Do not delete the AWS RDS database before migration and validation are complete.
- Take a backup before migration.
- Copy data to Railway PostgreSQL.
- Validate schema, row counts, application reads, and writes.
- Keep a rollback path until the Railway database is confirmed stable.

### Deferred Work

The migration is documented but will not be performed during the current S3 upload implementation phase.

---

## ADR-021: Shared AWS SDK v2 Client Configuration

**Status:** Accepted

The backend will use AWS SDK for Java v2. Adding the required Maven or Gradle dependency is the installation step; no separate system-wide SDK installation is required.

`S3Client` and `S3Presigner` will be configured once as Spring beans in:

- File: `src/main/java/com/localroots/crm/config/S3Config.java`
- Package: `com.localroots.crm.config`

### Rationale

Central bean configuration keeps region and credential behavior consistent and allows upload, verification, download, and presigning services to reuse the same clients.

