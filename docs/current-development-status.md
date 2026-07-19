# Client Files Current Development Status

**Last updated:** July 16, 2026

## Current Phase

Client Files is in late integration development. Its core attachment workflow is functional in development, and the current focus has expanded to:

- Direct Estimater-to-Client Files saving
- UTF-8 plain-text estimate storage and viewing
- Contact-specific uploads
- Mixed-client and mixed-category batch uploads
- Grid/list file browsing, search, sorting, filtering, and pagination
- Structured front, back, and total lawn measurements

Updated backend packages and Base44 implementation prompts have been prepared, but they still need to be merged into the active repositories, fully tested, deployed, and verified end to end.

## Current Architecture

```text
Base44 Client Files dashboard
        |
        | HTTPS JSON API with JWT bearer token
        v
Client Files Spring Boot backend
  - Local machine in development
  - Railway in production
        |
        +---- PostgreSQL contact and attachment metadata
        |
        +---- Presigned upload, view, and download URLs
                     |
                     v
               Private Amazon S3

Base44 Estimater
        |
        | estimate calculation and save request
        v
Estimater Spring Boot backend
        |
        +---- Structured estimate record in PostgreSQL
        |
        +---- UTF-8 .txt estimate artifact
        |
        +---- Client Files API for contact assignment and attachment storage
```

Base44 never receives permanent AWS credentials and does not connect directly to PostgreSQL. Tenant and attachment authorization remain backend responsibilities.

## Environment Placement

| Environment | Frontend | Backend | PostgreSQL | S3 |
|---|---|---|---|---|
| Development | Base44 previews | Local Spring Boot services through HTTPS tunnel or configured API URLs | Existing AWS development database(s) | Development Client Files bucket |
| Production | Base44 production apps | Railway | Railway PostgreSQL | Production Client Files bucket |

Development and production use separate databases, buckets, AWS credentials, JWT secrets, origins, tenant identifiers, and configuration.

## Confirmed Client Files Foundation

- JWT login and authenticated tenant context
- Tenant-scoped contacts
- Phone-only and email-only contact rules
- Shared `tenants`, `contacts`, and `contact_attachments` tables
- Direct browser-to-S3 upload initialization, presigned `PUT`, and completion verification
- READY attachment creation
- Temporary Open/view and Download URLs
- Image thumbnails in the file gallery
- Contact detail pages
- All Files, Unassigned, and Deleted file views
- Assignment, reassignment, and unassignment support
- Soft deletion and restoration
- Safe download filenames with preserved extensions
- Correlation-aware backend logging
- External Railway PostgreSQL administration through the public TCP proxy

## Canonical Estimate Storage

Estimater saves two connected representations:

1. A structured PostgreSQL estimate record as the source of truth.
2. A versioned UTF-8 plain-text `.txt` file in Client Files as the client-facing and copyable snapshot.

The previous PDF direction is superseded.

### Estimate Attachment Values

- Content type: `text/plain; charset=UTF-8`
- Extension: `.txt`
- Category: `ESTIMATES`
- File kind: `DOCUMENT`
- Source: `ESTIMATOR`
- Related estimate UUID
- Existing, newly created, or null contact assignment
- READY status after successful storage and verification

### Formatting Requirements

The generated file and UI must preserve:

- `•` bullets
- `—` em dashes
- `~` tildes
- Curly punctuation and accented characters
- Indentation, spaces, and line breaks

Text must be encoded and decoded as UTF-8. It must not be processed as Latin-1, Base64, Markdown, or HTML.

## Plain-Text Viewing

The prepared Client Files backend includes an authenticated endpoint:

```http
GET /api/v1/attachments/{attachmentId}/text-content
```

The Base44 viewer should:

- Decode the response as UTF-8
- Render literal text
- Use `white-space: pre-wrap`
- Avoid `innerHTML` and Markdown conversion
- Copy with `navigator.clipboard.writeText()`

The default prepared maximum text-file size is 5 MB and should remain configurable.

## Estimater Save Workflow

The Estimater customer-messaging area will display **Save to Client Files** below the generated estimate and above the **Copy for Text** and **Reset** controls.

The save flow supports:

- Existing contact search by name, phone number, or email
- New contact creation without requiring a name
- Unassigned saving

A new contact must contain a usable phone number or email address. The returned contact UUID is used when creating the estimate attachment.

## Structured Estimate Fields

The prepared estimate model stores:

- `front_sqft`
- `back_sqft`
- `total_sqft`

New estimates populate the front and back values when supplied. Historical records may leave the split null. The total records the area used for pricing.

## Upload Experience Target

### Contact-Specific Uploads

- Upload files directly from a contact-detail page
- Preselect the current contact
- Allow explicit reassignment or Unassigned status

### Mixed Batch Assignment

- Select multiple files with checkboxes
- Select all, individual files, or ranges
- Apply a contact or category to only the selected subset
- Assign different subsets to different contacts and categories
- Retain per-file overrides when defaults change
- Require a category for each file

### Queue Previews

- Local thumbnails for images before upload
- File-type icons for text, PDF, document, video, and unknown files
- Filename, size, client, category, display name, and upload status

## File Browser Target

Client Files will support:

- Grid/icon view
- List view
- Search by display name and original filename
- Category and status filters
- File-kind filtering where useful
- Sort by name, created/uploaded time, updated time, and size
- Server-side pagination, search, filtering, and sorting
- Persistent view preference

On a contact page, the list view may omit the client column because the contact context is already known.

## Prepared Backend and Database Changes

### Client Files

- File search and sorting support
- File-kind filtering
- Batch assignment and categorization support
- `ESTIMATOR` attachment source support
- UTF-8 plain-text validation
- Authenticated text-content endpoint
- Correct `.txt` download behavior
- Indexes supporting file browsing

### Estimater

- Structured `estimates` records
- Existing-contact, new-contact, and Unassigned save flows
- UTF-8 `.txt` generation
- Client Files upload integration
- Front, back, and total lawn measurements
- Estimate UUID attachment metadata

### SQL Requiring Review or Application

- Client Files source-system and file-browser index changes
- Estimater `estimates` table creation when not already present
- `front_sqft` and `back_sqft` column additions

The Estimater production profile may currently use Hibernate schema updates, but explicit reviewed migrations remain preferable for controlled production changes.

## Base44 Work Prepared

Two implementation prompts have been prepared:

- Client Files upload and file-browser update
- Estimater Save to Client Files update

They cover contact-specific uploads, subset assignment, image previews, grid/list views, search, sorting, UTF-8 text viewing, and the Estimater save modal.

These prompts still need to be applied and verified against the current Base44 applications.

## Current Verification Status

Confirmed in the preparation environment:

- UTF-8 sample estimate displays readable bullets, em dashes, tildes, and line breaks
- Prepared main Java sources compile with Java 17 compatibility

Still unconfirmed:

- Full Maven test suites
- Active-repository merge quality
- Database migration success
- End-to-end save through Estimater, Client Files, PostgreSQL, and S3
- Copy and paste from Client Files into Quo
- Mixed-client and mixed-category upload behavior
- File browser performance with a larger collection
- Idempotent duplicate-save protection
- Production deployment and smoke testing

## Immediate Next Steps

1. Merge the latest prepared Client Files changes into the active Client Files repository.
2. Merge the latest prepared Estimater changes into the active Estimater repository.
3. Review the diffs and remove superseded PDF-generation code and documentation.
4. Run both full Maven test suites.
5. Apply and verify Client Files database constraints and indexes.
6. Apply and verify the Estimater estimate table and front/back measurement columns.
7. Configure `CLIENT_FILES_API_BASE_URL` for Estimater.
8. Apply the Client Files Base44 prompt and test contact-specific and mixed-batch uploads.
9. Apply the Estimater Base44 prompt and verify button placement and all three save destinations.
10. Test saved estimate text with bullets, em dashes, tildes, spacing, and line breaks.
11. Paste the copied estimate into Quo and verify formatting on desktop and mobile.
12. Add or verify estimate revisioning and duplicate-save idempotency.
13. Deploy verified changes and run a production smoke test with non-client test records.

## Deferred Work

- Multiple users and roles per tenant
- Tenant-configurable categories and branding
- Multipart and resumable video uploads
- Automated abandoned-upload cleanup
- Malware and file-signature inspection
- Automated thumbnail-generation service
- LandGlide crop processing
- Physical S3 purge after retention
- Advanced estimate revision approval workflow
- Production monitoring, audit reporting, and backup-restore drills
