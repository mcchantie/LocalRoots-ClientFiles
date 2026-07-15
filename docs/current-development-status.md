# Client Files Current Development Status

**Last updated:** July 14, 2026

## Current Phase

Client Files is in late development integration testing and production-deployment preparation.

The end-to-end development upload path is working:

```text
Base44
  -> Spring Boot upload initialization
  -> browser PUT to private development S3 bucket
  -> Spring Boot completion verification
  -> READY attachment in PostgreSQL
```

Uploaded files can be opened through short-lived S3 URLs, and image thumbnails can be displayed in the dashboard.

The current focus is completing and verifying the Railway production database bootstrap, committing and testing the consolidated backend changes, deploying the backend to Railway, and then completing the production integration smoke test.

## Current Architecture

```text
Base44 dashboard
        |
        | HTTPS JSON API with JWT bearer token
        v
Spring Boot backend
  - Local machine in development
  - Railway in production
        |
        +---- PostgreSQL metadata and relationships
        |
        +---- Presigned upload, view, and download URLs
                     |
                     v
               Private Amazon S3
```

Base44 never receives permanent AWS credentials and does not connect directly to PostgreSQL.

## Environment Placement

| Environment | Frontend | Backend | PostgreSQL | S3 |
|---|---|---|---|---|
| Development | Base44 preview | Local Spring Boot through HTTPS tunnel | Existing AWS RDS development database | Development Client Files bucket |
| Production | Base44 production app | Railway | Railway PostgreSQL | Production Client Files bucket |

Development and production use separate databases, buckets, AWS credentials, JWT secrets, origins, tenant identifiers, and configuration.

## Confirmed Working

- JWT login through the local HTTPS tunnel
- Authenticated current-user and tenant-name lookup
- Tenant-scoped contacts
- Phone-only and email-only contact schema rules
- Upload initialization with the corrected JSON contract
- Browser-to-S3 presigned `PUT`
- S3 CORS for the Base44 development origin
- Backend `HeadObject` completion verification
- READY attachment creation
- Temporary Open/view URLs
- Actual image thumbnail display
- Contact detail pages
- All Files and Unassigned attachment retrieval when frontend-only `ALL` filter values are omitted
- Shared `tenants`, `contacts`, and `contact_attachments` schema
- Soft-delete and restore backend operations
- External Railway PostgreSQL access from DataGrip through the public TCP proxy
- SQL execution reaches the Railway PostgreSQL server

## Backend Updates Prepared During July 13

The backend updates produced during testing include:

- `GET /api/v1/contacts/{contactId}/attachments`
- Safe tenant validation for contact-scoped file lists
- `PATCH /api/v1/attachments/{attachmentId}` for assign, reassign, and unassign
- Proper `405 Method Not Allowed` handling
- Safe download filename generation and `Content-Disposition`
- `deletedOnly=true` list filtering
- Correlation-aware request and operation logging
- S3, database, validation, assignment, deletion, and restoration diagnostics
- Optional detailed debug logging profile

These changes should be consolidated into the active backend branch and verified together before deployment.

## Upload Contract

The initialization request uses:

- `originalFileName`
- `contentType`
- Positive numeric `sizeBytes`
- Backend category enum
- Selected contact UUID or `null`

The frontend then uploads the original JavaScript `File` to the presigned S3 URL and calls the completion endpoint.

Backend headers such as `Authorization` and `ngrok-skip-browser-warning` must never be sent to S3.

## Attachment Category Labels

| Backend value | Base44 label |
|---|---|
| `ESTIMATES` | Estimates |
| `LANDGLIDE` | LandGlide Photos |
| `PROPERTY_PHOTOS` | Photos |
| `VIDEOS` | Videos |
| `DOCUMENTS` | Documents |
| `OTHER` | Other |

The backend enums remain unchanged.

## Download Behavior

The backend should control the download filename.

Example:

```text
Display name: bob's lawn
Original filename: local_roots_logo_facebook_cover.png
Download name: bobs_lawn.png
```

Open uses a temporary URL with `download=false`. Download uses `download=true`.

## Contact Assignment Rules

- Empty client search with no selected contact permits an unassigned upload.
- Typed text does not equal a selected contact.
- Unmatched non-empty text blocks upload and prompts the user to select or create a client.
- A new contact requires a phone number or email address.
- Name fields remain optional.
- Assignment and unassignment use PATCH with a contact UUID or `null`.

## Deleted Files Semantics

- No deletion parameter: active files only
- `includeDeleted=true`: active and deleted files
- `deletedOnly=true`: deleted files only

The Deleted Files page must use:

```http
GET /api/v1/attachments?deletedOnly=true&page=0&size=25
```

Deleting remains recoverable and does not physically remove the S3 object.

## Base44 UI Status

Implemented or visible during testing:

- Tenant business name in the application chrome
- Contacts and contact search
- Contact detail pages
- All Files, Unassigned, and Deleted navigation
- Attachment cards
- Image thumbnails
- Open and Download controls
- Assignment menus
- Upload dialog
- Responsive/mobile navigation structure

Still requiring final verification or completion:

- Contacts as the default root and post-login view
- Create-new-client fields in the upload dialog
- Client-not-found validation
- Delete action in every active attachment dropdown
- Restore-only behavior in Deleted Files
- Deleted Files using `deletedOnly=true`
- Correct contact labels on file cards
- Mobile drawer styling after the theme-color change
- Safe download filename after the latest backend restart

## Logging and Debugging

The updated backend logging design includes:

- Correlation IDs
- Tenant and authenticated-user context
- Request method, path, status, and duration
- Upload, S3 verification, completion, assignment, deletion, and restore events
- Structured error categories
- Rotating local logs
- Optional `local,debug` profile

Logs must not include JWTs, AWS secrets, complete presigned URLs, passwords, or raw contact identifiers.

A separate warning was observed from Hikari validating closed PostgreSQL connections. This did not cause the unsupported PATCH failure, but connection-pool lifetime settings should be reviewed if the warning continues.

## Railway Database Preparation

A Railway PostgreSQL setup bundle has been prepared with:

- Preflight checks
- Schema creation
- Production tenant seed
- Post-setup verification
- Transactional smoke test and cleanup

Use one creation path:

- Production Flyway, followed by tenant seed and verification; or
- Manual schema script with Flyway disabled

Do not run both schema-creation paths.

### Current Railway Database State

- DataGrip can connect from the local Mac through Railway's public TCP proxy using SSL.
- The reviewed schema script was attempted against Railway PostgreSQL.
- Execution stopped inside the first PL/pgSQL function with an `unterminated dollar-quoted string` error because DataGrip submitted only part of the dollar-quoted block.
- The SQL file contains the closing delimiter; the fix is to use **SQL Scripts → Run SQL Script…** or execute the entire selected function or `DO` block.
- The Railway migration must not be treated as complete until the verification queries pass.

### Production Tenant Bootstrap

The `tenants` table should generate its UUID and timestamp in PostgreSQL:

```sql
ALTER TABLE tenants
ALTER COLUMN id SET DEFAULT gen_random_uuid();
```

Normal production tenant insertion omits `id` and `created_at`, uses `RETURNING`, and saves the returned UUID as:

```text
CLIENT_FILES_TENANT_ID
```

The production JWT signing key is generated separately with:

```bash
openssl rand -base64 32
```

and stored as:

```text
CLIENT_FILES_JWT_SECRET_BASE64
```

A new schema-only `pg_dump` is needed only if the AWS development schema changed after the last verified dump. A full development data dump should not be copied into Railway by default.


## Estimater Integration Decision

Estimater will not use Google Docs or Word files as the canonical saved estimate. It will save:

1. Structured estimate and line-item data in PostgreSQL as the editable source of truth.
2. A generated, versioned PDF in S3 with a linked Client Files attachment record.

The PDF attachment will use `application/pdf`, category `ESTIMATES`, file kind `DOCUMENT`, the selected contact, and the related estimate ID. Revisions create new PDF snapshots rather than replacing an estimate that may already have been sent.

## Immediate Next Steps

1. Rerun the complete Railway schema script through DataGrip using **Run SQL Script**.
2. Verify the Railway tables, columns, constraints, indexes, functions, and triggers.
3. Apply the database-generated UUID default to `tenants.id` if needed.
4. Insert the production tenant and capture the generated UUID.
5. Set `CLIENT_FILES_TENANT_ID` and `CLIENT_FILES_JWT_SECRET_BASE64` in Railway.
6. Create the attachment, observability, and documentation Git commits; verify `logs/` is ignored; push to `origin/main`.
7. Run the full Maven test suite.
8. Retest assignment, reassignment, unassignment, delete, restore, and safe download filenames.
9. Complete and test upload-time new-client creation.
10. Verify Contacts as the default view, tenant name placement, and the mobile navigation drawer.
11. Deploy the backend to Railway and verify startup against Railway PostgreSQL.
12. Replace the ngrok API URL in Base44 with the Railway backend URL.
13. Configure final production Base44 and S3 CORS origins.
14. Perform a production login, contact, upload, open, download, assignment, deletion, and restoration smoke test.
15. Design the initial structured estimate tables and implement PDF generation for direct Estimater-to-Client Files saving.

## Deferred Work

- Multiple users and roles per tenant
- Tenant-configurable categories and branding
- Multipart and resumable video uploads
- Automated abandoned-upload cleanup
- Malware and file-signature inspection
- Thumbnail generation service
- LandGlide crop processing
- Physical S3 purge after retention
- Implementation of estimate PDF generation and structured Estimater records
- Production monitoring, audit reporting, and backup-restore drills
