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

## ADR-019: AWS Credentials Use the Default Credential Chain

**Status:** Accepted; clarified July 11, 2026

The Spring Boot application will use the AWS SDK for Java v2 default credential provider chain. Access keys will not be read from custom Spring properties or committed to source control.

### Configuration Boundary

The following values are non-secret and may be stored in environment-specific `application.properties` files or overridden through environment variables:

- `storage.s3.region`
- `storage.s3.bucket`
- Upload and download URL durations
- Maximum file size
- Allowed MIME types

The following credentials must remain outside committed application configuration:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

For local development, the credentials may be supplied through the IDE run configuration or a local AWS credentials profile. Railway production credentials will be stored as Railway environment variables.

### Rationale

Bucket names and regions are configuration, not secrets. Keeping credentials in the AWS default credential chain avoids custom secret handling and lets the same `S3Client` and `S3Presigner` configuration work locally and on Railway.

### Verification

A real S3 operation must be performed to verify credentials. Merely starting Spring Boot may not contact AWS. Initial verification should use a controlled `PutObject` and `HeadObject` smoke test against the development bucket.

---

## ADR-020: Environment Deployment and Database Placement

**Status:** Accepted; clarified July 11, 2026

The initial environment placement is:

| Environment | Application | PostgreSQL database |
|---|---|---|
| Development | Local machine | Existing AWS-hosted development database |
| Production | Railway | Railway PostgreSQL |

Development and production will continue to use separate S3 buckets, AWS credentials, JDBC credentials, and application configuration.

### Consequences

- The development application will connect to the existing AWS development database when database-backed development begins.
- H2 may be used for isolated tests, but it is not the intended development database.
- The production application and production database will be deployed together on Railway.
- Moving or creating production data in Railway is a separate, explicitly planned database task.
- The AWS development database is not assumed to move to Railway.
- No AWS database may be deleted as part of production setup without a reviewed backup, validation, and rollback plan.

---

## ADR-021: Shared AWS SDK v2 Client Configuration

**Status:** Accepted

The backend will use AWS SDK for Java v2. Adding the required Maven or Gradle dependency is the installation step; no separate system-wide SDK installation is required.

`S3Client` and `S3Presigner` will be configured once as Spring beans in:

- File: `src/main/java/com/localroots/clientfiles/config/S3Config.java`
- Package: `com.localroots.clientfiles.config`

### Rationale

Central bean configuration keeps region and credential behavior consistent and allows upload, verification, download, and presigning services to reuse the same clients.



---

## ADR-022: Attachment Schema Integration Is Deferred

**Status:** Superseded July 13, 2026 by ADR-031

The backend contains an attachment metadata entity and repository, but it will not create or modify database tables yet. The existing CRM tenant, contact, estimate, user, and `contact_attachments` schema must be inspected before choosing whether to extend an existing table or create a new one.

### Rationale

The current backend archive does not contain the existing CRM database schema. Creating a parallel table or assuming columns on an operational table could produce an incompatible design.

### Consequences

- No database migration file is included.
- No attachment table is created automatically.
- Contact and estimate IDs remain UUID fields in the Java model until real relationships are designed.
- Database-backed attachment endpoints require a compatible table to be created later through an explicitly reviewed process.

---

## ADR-023: Development Tenant Headers Are Explicitly Non-Production

**Status:** Superseded July 13, 2026 by ADR-030

The local profile may resolve tenant identity from `X-Tenant-Id` so the upload API can be tested before login integration is complete.

Outside the local profile, tenant-header authorization and unverified contact or estimate IDs are disabled by default.

### Rationale

A caller-supplied tenant ID is not authentication. The backend must fail closed rather than accidentally treating a request header as production tenant authorization.

### Consequences

- Production requests remain unavailable until authenticated tenant resolution is connected.
- Contact- and estimate-linked uploads remain unavailable in production until ownership checks are implemented.
- Unassigned uploads can still be modeled without inventing CRM schema.

---

## ADR-024: Upload Completion Requires S3 Verification

**Status:** Accepted

An attachment is not marked `READY` merely because the frontend reports success. The backend performs `HeadObject` and validates the stored object against the initialized upload.

### Initial Verification

- Expected bucket and tenant key prefix
- Exact declared file size
- Expected content type
- Optional base64 SHA-256 checksum

### Rationale

Presigned URLs can expire, uploads can fail, and the completion call must not create a ready database record for a missing or mismatched S3 object.

---

## ADR-025: Soft Delete Does Not Physically Delete S3 Objects Initially

**Status:** Accepted

The first release only sets `deleted_at` in PostgreSQL. It does not call `DeleteObject` in S3.

### Rationale

This aligns with the recovery requirement and avoids granting runtime delete permission before retention and restore rules are finalized.

### Consequences

- The initial application IAM policy does not require `s3:DeleteObject`.
- A later purge workflow may remove objects after a retention period.
- S3 versioning remains a second recovery layer.

---

## ADR-026: Initial Uploads Use a Single Presigned PUT

**Status:** Accepted

The first release uses a single presigned `PutObject` request with a configurable maximum file size. The initial default is 500 MB.

### Rationale

This is sufficient for the first photo, document, and moderate video workflows while keeping the API small. Multipart upload can be added when real video sizes justify the added state and cleanup logic.

### Consequences

- Allowed MIME types and maximum size are configuration-driven.
- Multipart upload, abandoned-part cleanup, and resume support remain future work.

---

## ADR-027: No Automatic Database Schema Management Yet

**Status:** Amended July 13, 2026

Client Files will not use Flyway, Liquibase, migration files, or Hibernate schema generation at this stage.

### Rationale

The database design has not been reconciled with the existing Local Roots CRM schema, and automatic startup behavior should not make or validate database changes before that design is understood and approved.

### Consequences

- The existing Local Roots schema has now been inspected and a reviewed integration migration was applied manually to the AWS development database.
- Flyway remains disabled for the AWS development database so local startup does not alter the shared schema.
- Hibernate continues to use `ddl-auto=none` and `generate-ddl=false`.
- Future development and production schema changes must be reviewed explicitly before execution.
- Railway production database migration remains a separate controlled deployment step.

---

## ADR-028: Contacts May Be Nameless

**Status:** Accepted

A Local Roots CRM contact may exist without a first name, last name, or display name. Attachments, estimate screenshots, and communication history may be linked to a contact identified only by a phone number or only by an email address.

### Rationale

Some estimate recipients and leads never provide a name or never respond. Their phone number, email address, estimate screenshots, and related files are still operationally useful if they respond later.

### Data Rules

- Contact name fields must remain nullable in the future schema.
- When no name is present, at least one usable identifier must exist: phone number or email address.
- Attachments should reference the contact record by `contact_id`, not store a raw phone number or email as the attachment relationship.
- Phone numbers and email addresses should be normalized for matching and duplicate prevention.
- Adding a name later updates the existing contact rather than creating a second contact.
- Any future migration, entity validation, API validation, and frontend form validation must preserve this behavior.

### Display Fallback

The interface should identify a contact using this order:

1. Name
2. Phone number
3. Email address
4. `Unnamed contact`

---

## ADR-029: Base44 Owns the Dashboard; Spring Boot Is API-Only

**Status:** Accepted July 13, 2026

The Client Files dashboard will be built and hosted in Base44. The Spring Boot application will run as an API-only backend on Railway.

### Deployment Boundary

- Base44 owns login screens, contacts, file galleries, filters, upload controls, and responsive UI behavior.
- Railway hosts the Spring Boot JSON API.
- Railway PostgreSQL will hold production relational data.
- Amazon S3 will hold private file objects.
- Spring Boot will not bundle or serve the production dashboard.

### Rationale

Separating the frontend allows rapid dashboard development while keeping authorization, tenant isolation, database access, S3 signing, and business rules in the backend.

---

## ADR-030: Stateless JWT Authentication Determines Tenant Context

**Status:** Accepted July 13, 2026

Base44 authenticates through the backend. The backend issues a signed JWT, and authenticated requests use:

```http
Authorization: Bearer <access_token>
```

Tenant identity is derived from the signed token and backend-controlled tenant configuration. The browser must not choose a tenant by sending an arbitrary tenant ID.

### Consequences

- The temporary `X-Tenant-Id` development approach is superseded.
- Contact and attachment operations are tenant-scoped on the server.
- Base44 clears the token and returns to login after `401`.
- `403` is treated as a permission failure.
- JWT secrets exist only in backend environment configuration.
- Development and production use different signing secrets.

---

## ADR-031: Client Files Reuses Shared Local Roots Tables

**Status:** Accepted July 13, 2026

Client Files uses the existing shared tables:

- `tenants`
- `contacts`
- `contact_attachments`

It will not create parallel `client_contacts` or `client_attachments` tables.

### Contact Integration

- First and last names are nullable.
- A contact must have a usable phone number or email address.
- Phone and email are normalized for matching.
- A database trigger maintains normalized identifiers for every application using the table.
- Attachments continue to reference contacts by database ID.

### Attachment Integration

- `contact_attachments.contact_id` may be null for unassigned files.
- The table stores category, file kind, upload status, names, S3 metadata, verification metadata, timestamps, soft-deletion state, and parent relationships.
- Existing CRM source-system constraints remain intact.
- Base44-created files use an allowed shared source-system value.

### Rationale

Contacts and attachments are platform records. Reusing shared tables prevents duplicate clients and keeps Client Files compatible with the rest of Local Roots.

---

## ADR-032: Development RDS Changes Use Reviewed Safe-to-Rerun SQL

**Status:** Accepted July 13, 2026

The locally running backend continues to use the AWS RDS development database. Schema changes are applied through reviewed SQL rather than automatic local startup migration.

### Required Process

1. Confirm the active database and user.
2. Run read-only preflight checks.
3. Run the reviewed safe-to-rerun migration.
4. Run post-migration verification.
5. Export a fresh schema-only `pg_dump` for comparison.

### Tooling Note

PostgreSQL function bodies enclosed in dollar quotes must be executed as complete statements. SQL editors that split at internal semicolons can produce an unterminated dollar-quoted string error; executing the full script or using `psql` avoids it.

---

## ADR-033: The Base44 Dashboard Is Tenant-Neutral

**Status:** Accepted July 13, 2026

The shared dashboard is branded **Local Roots Client Files** and must not be hard-coded to Texas Top Dressing, lawn leveling, or any single service industry.

### Requirements

- Tenant identity may be displayed from `/api/v1/auth/me`.
- Tenant IDs and company names are not hard-coded.
- The browser cannot select or change its tenant.
- Generic terms such as Contacts, Files, Photos, Videos, Documents, Estimates, Unassigned, and Deleted are used.
- Backend category enum values remain unchanged while Base44 may show tenant-neutral labels.
- Branding and category display mapping remain centralized for future tenant configuration.

---

## ADR-034: Base44 CORS and Local Tunnel Rules

**Status:** Accepted July 13, 2026

The backend permits configured Base44 origins and the headers needed by its API client.

During local testing through a free ngrok tunnel, Base44 backend requests include:

```http
ngrok-skip-browser-warning: true
```

The backend development CORS configuration must allow this header.

### Consequences

- The exact Base44 preview or production origin must be configured.
- `Authorization` and `ngrok-skip-browser-warning` are sent only to the backend.
- Neither header is sent to presigned S3 URLs.
- Direct S3 uploads send only headers required by the presigned upload response.

---

## ADR-035: JWT Secret Handling

**Status:** Accepted July 13, 2026

The backend signing key is supplied through:

```text
CLIENT_FILES_JWT_SECRET_BASE64
```

Generate a development value with:

```bash
openssl rand -base64 32
```

The value must decode to at least 32 bytes.

### Security Rules

- The secret is not stored in Base44.
- The secret is not committed to Git.
- Development and production use different values.
- Rotating the secret invalidates existing tokens.

---

## ADR-036: Direct-to-S3 Uploads Remain a Three-Step Workflow

**Status:** Accepted July 13, 2026

Client Files will keep the direct-to-S3 upload architecture.

One frontend upload function will orchestrate these three operations:

1. `POST /api/v1/attachments/uploads` to create a pending attachment and receive a presigned upload URL.
2. `PUT` the original browser `File` directly to the presigned S3 URL.
3. `POST /api/v1/attachments/{attachmentId}/complete` so the backend can verify the S3 object and mark the attachment ready.

The two backend endpoints will not be collapsed into a multipart upload endpoint at this stage.

### Required Frontend Contract

The upload-initialization request must include the backend field names and values, including:

- `originalFileName`
- `contentType`
- Positive numeric `sizeBytes`
- Backend category enum
- Selected contact UUID or `null`

The frontend must retain the original JavaScript `File` object until the S3 `PUT` completes.

### Security Boundary

- Backend API requests use the JWT and, during local tunnel testing, the ngrok warning-skip header.
- Presigned S3 requests use only the headers returned by the backend.
- JWT, ngrok, and JSON API headers are never sent to S3.
- The completion endpoint remains responsible for `HeadObject` verification.

---

## ADR-037: Browser Uploads Require Separate S3 CORS Configuration

**Status:** Accepted July 13, 2026

Spring API CORS and S3 bucket CORS are separate controls.

The Client Files S3 bucket must allow the exact Base44 browser origin to perform the browser operations required by the application.

### Development Baseline

- Allowed origins: exact Base44 preview origin or other explicitly approved frontend origins
- Allowed methods: `PUT`, `GET`, and `HEAD`
- Allowed headers: the headers required by the presigned operation; `*` is acceptable during development
- `OPTIONS` is not added as an S3 allowed method because S3 handles preflight automatically
- Exposing `ETag` is optional unless frontend JavaScript needs to read it

CORS does not make the bucket public. IAM, bucket policy, object privacy, and the presigned signature continue to authorize access.

---

## ADR-038: Download Filenames Use Display Names with Safe Extensions

**Status:** Accepted July 13, 2026

The backend will generate the download filename and set it in the presigned S3 response `Content-Disposition`.

### Filename Rules

- Use `display_name` as the base when present.
- Otherwise use the original filename base.
- Preserve the extension from `original_file_name`.
- Infer an extension from the content type only when the original name has no extension.
- Remove apostrophes.
- Convert whitespace and unsafe filename characters to underscores.
- Collapse repeated underscores.
- Avoid duplicating the extension.
- Use a safe fallback such as `file`.

Example:

```text
Display name: bob's lawn
Original filename: local_roots_logo_facebook_cover.png
Download filename: bobs_lawn.png
```

The Open action continues to request a view URL with `download=false`. The Download action requests a URL with `download=true`.

---

## ADR-039: Attachment Category Labels Are Centralized

**Status:** Accepted July 13, 2026

Backend category enum values remain unchanged:

- `ESTIMATES`
- `LANDGLIDE`
- `PROPERTY_PHOTOS`
- `VIDEOS`
- `DOCUMENTS`
- `OTHER`

The Base44 dashboard displays them as:

| Backend value | Display label |
|---|---|
| `ESTIMATES` | Estimates |
| `LANDGLIDE` | LandGlide Photos |
| `PROPERTY_PHOTOS` | Photos |
| `VIDEOS` | Videos |
| `DOCUMENTS` | Documents |
| `OTHER` | Other |

The label **Property Maps** is removed. Display labels remain a frontend concern and must not rename backend enum values.

---

## ADR-040: Contacts Is the Default Dashboard View

**Status:** Accepted July 13, 2026

After login and when the application opens at its root route, the dashboard defaults to **Contacts** rather than **All Files**.

The tenant business name returned by `/api/v1/auth/me` is displayed in the sidebar beneath **Local Roots Client Files**.

### Requirements

- The tenant name is never hard-coded.
- Explicit deep links to files, contacts, Unassigned, or Deleted are preserved.
- The same tenant name should not be redundantly repeated in multiple prominent page headings.

---

## ADR-041: Upload-Time Contact Selection Is Explicit

**Status:** Accepted July 13, 2026

Typing text into the upload dialog's client search field does not assign a contact. A contact is assigned only when the user selects an actual backend search result.

### Behavior

- Empty search text and no selected contact means the upload may remain unassigned.
- Non-empty search text without a selected result blocks upload.
- The UI displays: `Client not found. Select an existing client or create a new client.`
- The new-client fields are then shown.
- A newly created client must contain a usable phone number or email address; a name alone is insufficient.
- The new contact is created before upload initialization.
- The returned contact UUID is used for every applicable file in that upload batch.
- Typed client text is never sent as `contactId`.

---

## ADR-042: Base44-Compatible Attachment Routes Are Part of the API Contract

**Status:** Accepted July 13, 2026

The backend supports the dashboard routes needed for attachment navigation and assignment.

### Contact Files

```http
GET /api/v1/contacts/{contactId}/attachments
```

This route verifies that the contact belongs to the authenticated tenant before listing files.

### Assignment and Unassignment

```http
PATCH /api/v1/attachments/{attachmentId}
Content-Type: application/json
```

Assign or reassign:

```json
{
  "contactId": "contact-uuid"
}
```

Return to Unassigned:

```json
{
  "contactId": null
}
```

The earlier assignment endpoint may remain for compatibility. Unsupported HTTP methods must return `405 Method Not Allowed`, not an unexpected `500`.

---

## ADR-043: Deleted-File Queries Distinguish Included from Deleted-Only

**Status:** Accepted July 13, 2026

Attachment-list deletion filters have distinct meanings:

- No deletion parameter: active attachments only
- `includeDeleted=true`: active and deleted attachments
- `deletedOnly=true`: deleted attachments only

The Deleted Files page must use `deletedOnly=true`.

Soft deletion remains the application behavior. Deleting an attachment sets its deletion timestamp and does not physically remove its S3 object.

---

## ADR-044: Backend Observability Uses Correlation-Aware Safe Logging

**Status:** Accepted July 13, 2026

The backend will provide debugging-focused logging for API, database, authentication, and S3 workflows.

### Logging Scope

- Correlation ID on request and error logs
- Tenant and authenticated-user context where available
- Request method, safe path/query information, response status, and duration
- Upload initialization, S3 verification, and completion
- Assignment, unassignment, deletion, and restoration
- Contact creation, update, and search events
- Clear validation, authorization, database, S3, and unexpected-error categories
- Rotating local log files
- Optional detailed `debug` profile

### Sensitive Data Rules

Logs must not contain:

- JWTs
- AWS credentials
- Complete presigned URLs
- Raw passwords
- Full phone numbers or email addresses unless explicitly and safely masked

API problem responses continue to include a correlation ID so a frontend error can be traced to backend logs.

---

## ADR-045: Railway Database Bootstrap Is Reviewed and Reproducible

**Status:** Accepted July 13, 2026

The Railway production PostgreSQL database will be initialized through a reviewed, reproducible setup rather than by copying uncontrolled development data.

The prepared setup includes:

1. Preflight checks
2. Schema creation or application migration
3. Stable production-tenant seed
4. Post-setup verification
5. Transactional smoke test and cleanup

### Migration Path Rule

Use one schema-creation path:

- If production Flyway is enabled, allow Flyway to create the schema and run only the reviewed seed and verification steps afterward.
- If setup is manual, disable Flyway for that deployment and run the reviewed schema script.

Do not run both schema-creation paths against the same fresh database.

### Tenant Identity

The production tenant uses one stable UUID. The same UUID must be used in:

- The production `tenants` row
- `CLIENT_FILES_TENANT_ID`

### Dump Policy

A new schema-only `pg_dump` is required before production setup only if the AWS development schema changed after the last verified dump.

A full development data dump is not copied into Railway by default because it may contain test contacts, pending attachment rows, or references to the development S3 bucket.

