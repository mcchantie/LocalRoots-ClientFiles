# Client Files Current Development Status

**Last updated:** July 13, 2026

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

The next phase is to consolidate and verify the latest backend changes, finish the remaining Base44 behaviors, run the full test suite, and initialize the Railway production database.

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

## Backend Updates Prepared During This Session

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

The production tenant uses one stable UUID, also supplied as:

```text
CLIENT_FILES_TENANT_ID
```

A new schema-only `pg_dump` is needed only if the AWS development schema changed after the last verified dump. A full development data dump should not be copied into Railway by default.

## Immediate Next Steps

1. Consolidate all backend fixes into the active repository branch.
2. Restart the backend and run the full Maven test suite.
3. Retest assignment, reassignment, and unassignment.
4. Switch Base44 Deleted Files to `deletedOnly=true`.
5. Verify Delete and Restore across all attachment views.
6. Verify sanitized download filenames.
7. Complete and test upload-time new-client creation.
8. Verify Contacts as the default view and tenant name placement.
9. Retest image preview caching and full-size viewing.
10. Fix and retest the mobile navigation drawer theme regression.
11. Review stale `PENDING_UPLOAD` rows and abandoned development objects.
12. Review Hikari connection lifetime settings if warnings continue.
13. Choose the Railway schema-creation path.
14. Initialize Railway PostgreSQL and seed the stable production tenant.
15. Deploy the backend to Railway.
16. Replace the ngrok API URL in Base44 with the Railway URL.
17. Configure final production Base44 and S3 CORS origins.
18. Perform a production smoke test before operational use.

## Deferred Work

- Multiple users and roles per tenant
- Tenant-configurable categories and branding
- Multipart and resumable video uploads
- Automated abandoned-upload cleanup
- Malware and file-signature inspection
- Thumbnail generation service
- LandGlide crop processing
- Physical S3 purge after retention
- Estimate PDF generation and structured Estimater records
- Production monitoring, audit reporting, and backup-restore drills
