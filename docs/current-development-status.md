# Client Files Current Development Status

**Last updated:** July 13, 2026

## Current Phase

Client Files is in end-to-end integration testing.

The Base44 dashboard is set up, the Spring Boot backend has been converted into a tenant-scoped API, and the AWS RDS development schema has been migrated and verified against the existing Local Roots CRM tables.

The immediate task is to complete Base44-to-local-backend testing through an HTTPS tunnel, then deploy the backend and a separate production PostgreSQL database to Railway.

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
        +---- Presigned direct uploads to private Amazon S3
```

Base44 does not connect directly to PostgreSQL and does not receive permanent AWS credentials.

## Environment Placement

| Environment | Frontend | Backend | PostgreSQL | S3 |
|---|---|---|---|---|
| Development | Base44 preview | Local Spring Boot through HTTPS tunnel | Existing AWS RDS development database | Development Client Files bucket |
| Production | Base44 production app | Railway | Railway PostgreSQL | Production Client Files bucket |

Development and production use separate credentials, buckets, JWT secrets, origins, configuration, and data.

## Base44 Dashboard Status

The dashboard has been generated and revised to remain tenant-neutral.

Current dashboard scope includes:

- Login and logout
- Current-user and tenant-name lookup
- Contact list and search
- Nameless contact support
- Contact detail and file views
- Drag-and-drop uploads and progress
- Attachment categories
- Image previews
- Open and download
- All Files, Unassigned, and Deleted views
- Assignment, reassignment, and unassignment
- Soft deletion and restoration
- Responsive desktop and mobile layouts

The shared product identity is **Local Roots Client Files**. Texas Top Dressing and lawn-specific wording must not be hard-coded.

## Backend Status

The API now includes:

- Stateless JWT login
- Authenticated current-user lookup
- Tenant-scoped authorization
- Tenant name for frontend branding
- Contact search, creation, reading, and updates
- Phone-only and email-only contacts
- Attachment listing and filtering
- Unassigned attachments
- Assignment, reassignment, and unassignment
- Presigned S3 uploads
- `HeadObject` completion verification
- Short-lived view and download URLs
- Soft deletion and restoration
- Base44-compatible CORS
- Railway deployment packaging

Tenant identity comes from the signed token and backend configuration. Base44 must not supply an arbitrary tenant ID.

## Shared Database Integration

Client Files uses:

- `tenants`
- `contacts`
- `contact_attachments`

It does not use parallel `client_contacts` or `client_attachments` tables.

The development schema now supports nullable contact names, normalized phone and email values, usable-identifier enforcement, unassigned attachments, attachment categories and statuses, S3 metadata, verification fields, timestamps, soft deletion, parent relationships, indexes, checks, foreign keys, functions, and triggers.

A post-migration schema-only `pg_dump` confirmed the expected changes and no duplicate Client Files tables.

## Migration Policy

The AWS RDS development database was updated using:

1. Read-only preflight checks
2. A safe-to-rerun integration migration
3. Post-migration verification
4. A fresh schema-only `pg_dump`

Flyway remains disabled for the shared AWS development database. Local startup must not modify the schema automatically.

Railway production will receive its own reviewed migration during deployment.

## Contact Rules

A contact may be created with:

- Phone only
- Email only
- Name and phone
- Name and email
- Name, phone, and email

At least one usable phone number or email address is required.

Display fallback order:

1. Name
2. Phone number
3. Email address
4. `Unnamed contact`

## Authentication and Secrets

Base44 sends:

```http
Authorization: Bearer <access_token>
```

The signing secret is configured only in the backend:

```text
CLIENT_FILES_JWT_SECRET_BASE64
```

Generate a development value with:

```bash
openssl rand -base64 32
```

Do not commit it, put it in Base44, or reuse it in production.

## Current Local Testing Issue

Base44 is calling the local backend through a free ngrok URL. The current response contains:

```text
Ngrok-Error-Code: ERR_NGROK_6024
Content-Type: text/html
```

Ngrok is returning its browser-warning page before the request reaches Spring Boot.

The shared Base44 backend client must include:

```http
ngrok-skip-browser-warning: true
```

for ngrok backend requests.

Spring CORS must allow the exact Base44 preview origin and this custom header.

Do not send `Authorization` or `ngrok-skip-browser-warning` to presigned S3 PUT URLs.

## Required Development Configuration

The local backend needs:

- AWS development credentials through the default credential chain
- Development S3 bucket and region
- AWS RDS JDBC URL, user, and password
- A real development `tenants.id` value
- `CLIENT_FILES_JWT_SECRET_BASE64`
- The exact Base44 preview origin in allowed origins
- Current development login credentials

The Base44 API base URL currently points to the HTTPS tunnel and will later be replaced by the Railway public URL.

## Immediate Next Steps

1. Add the ngrok warning-skip header to the centralized Base44 API client.
2. Confirm Spring CORS permits the exact Base44 preview origin and custom header.
3. Retest login and verify a JSON token is returned.
4. Verify `/api/v1/auth/me` returns the tenant name.
5. Test phone-only and email-only contacts.
6. Test contact search and updates.
7. Test attachment filters, including Unassigned and Deleted.
8. Test the full presigned S3 upload and completion flow.
9. Test open, download, assignment, reassignment, unassignment, delete, and restore.
10. Run the complete backend test suite.
11. Rebase, commit, and push the backend.
12. Deploy the backend and production PostgreSQL to Railway.
13. Apply the reviewed production schema migration.
14. Replace the ngrok API URL in Base44 with the Railway URL.
15. Configure final Base44 origins in Railway and production S3 CORS.
16. Perform a production smoke test before operational use.

## Deferred Work

- Multiple users and roles per tenant
- Tenant-configurable attachment categories
- Tenant-configurable branding
- Multipart and resumable video uploads
- Abandoned-upload cleanup
- Malware and file-signature inspection
- Thumbnail generation
- LandGlide crop processing
- Physical S3 purge after retention
- Estimate PDF generation and structured Estimater records
- Production monitoring, audit reporting, and backup-restore drills
