# Client Files Current Development Status

**Last updated:** July 9, 2026

## Product Overview

**Client Files** is the planned Local Roots CRM application for storing, uploading, viewing, searching, organizing, and retrieving client-related files.

It will support:

- Photos
- Videos
- Estimate documents
- LandGlide screenshots
- General documents
- Files connected to existing contacts
- Files connected while creating a new contact
- Temporarily unassigned uploads

The pricing calculator is being renamed **Estimater**.

## Current Phase

Architecture and scope definition.

No S3 bucket, upload endpoint, processing worker, or Client Files frontend has been implemented during this documented session.

## Confirmed Architecture

### Environments

Use two environments initially:

- Development
- Production

The environment holding real business data is production.

UAT is deferred until a stable pre-production release-testing environment is needed.

### Storage

- Amazon S3 stores file objects.
- Local Roots RDS stores attachment metadata, relationships, search fields, processing state, and analytics data.
- Development and production use separate storage and databases.
- Production resources must have backups, restricted access, and recoverable deletion.

### Upload Flow

1. Frontend requests an upload from Spring Boot.
2. Backend validates tenant and contact context.
3. Backend creates a pending attachment record.
4. Backend returns a presigned S3 upload URL.
5. Frontend uploads directly to S3.
6. Frontend confirms completion.
7. Backend verifies the object.
8. Backend marks the file ready or sends it for processing.

### Contact Assignment

Uploads may be:

- Assigned to an existing contact
- Assigned while creating a new contact
- Left temporarily unassigned

### Attachment Categories

- Estimates
- LandGlide
- Property Photos
- Videos
- Documents
- Other

### LandGlide Processing

- Preserve the original screenshot.
- Create a separate cropped derivative.
- Link processed output to the original.
- Start with a fixed crop profile based on screenshot dimensions.
- Add preview or manual adjustment if needed.
- Test click, drag, copy, and paste into Quo before building any Quo-specific integration.

### Estimater Integration

Estimater should save both:

- A generated estimate document in S3
- Structured estimate data in RDS

Structured data should include lawn size, materials, services, discounts, totals, calculator inputs/outputs, pricing version, status, and revisions.

## Planned Main Components

### Frontend

- Client Files page
- Contact file gallery
- Drag-and-drop uploader
- Multi-file upload
- Upload progress
- Contact search
- New-contact creation
- Unassigned-upload queue
- Category filters
- Image preview
- Video preview
- Copy, drag, open, and download behavior
- Rename, notes, soft delete, and restore

### Spring Boot Backend

- Authentication and tenant authorization
- Attachment metadata APIs
- Contact lookup and creation
- Upload initialization
- Presigned URLs
- Upload completion verification
- Search and filtering
- Soft deletion and restore
- Processing orchestration
- Estimate relationships
- Audit and error logging

### AWS

- Production S3 bucket
- Development S3 bucket
- Block Public Access
- Versioning
- IAM policy for the backend
- Presigned uploads and downloads
- Lifecycle rules
- Optional later use of Lambda and SQS

### Database

Likely required or updated models:

- `contact_attachments`
- `attachment_upload_sessions`
- `attachment_processing_jobs`
- `estimates`
- `estimate_line_items`
- `estimate_versions`
- Possibly `properties` or service addresses

### Processing

- Image thumbnails
- LandGlide crop
- File metadata extraction
- Video metadata
- Future HEIC preview conversion
- Future video thumbnailing or transcoding

### Operations and Security

- Private S3 objects
- Short-lived signed URLs
- File-type validation
- File-size limits
- Tenant isolation
- Soft deletion
- RDS backups and point-in-time recovery
- S3 versioning
- Failed-upload visibility
- Orphaned-upload cleanup
- Malware scanning before external client uploads are enabled

## Work Completed

- Named the application Client Files.
- Renamed the pricing calculator concept to Estimater.
- Defined the initial two-environment strategy.
- Defined the file and metadata storage split.
- Defined upload assignment options.
- Defined attachment categories.
- Defined the first LandGlide processing strategy.
- Removed Quo-specific message integration from the first-release scope.
- Defined structured estimate storage requirements.
- Defined the major application components.
- Created the project’s initial Markdown documentation files.

## Immediate Next Step

Create the production S3 bucket for real Client Files data, followed by the development bucket.

Before implementation, inspect the current database schema so the S3 and API design match the existing tenant, contact, attachment, user, and address models.

## Next Implementation Checklist

- [ ] Inspect the existing database schema.
- [ ] Confirm the current `contact_attachments` columns and constraints.
- [ ] Choose AWS region.
- [ ] Choose development and production bucket names.
- [ ] Create the production S3 bucket.
- [ ] Enable Block Public Access.
- [ ] Enable S3 versioning.
- [ ] Create the development S3 bucket.
- [ ] Create restricted IAM permissions.
- [ ] Add environment-specific Spring Boot S3 configuration.
- [ ] Finalize attachment and upload-session schema.
- [ ] Implement authentication and tenant authorization needed for production files.
- [ ] Implement presigned upload initialization.
- [ ] Implement upload completion verification.
- [ ] Build the first Client Files upload interface.
- [ ] Build the contact file gallery.
- [ ] Test LandGlide copy, drag, and paste behavior with Quo.
- [ ] Implement the first LandGlide crop profile.
- [ ] Define and create Estimater relational tables.
- [ ] Generate and store estimate documents.
- [ ] Add soft deletion, restore, logging, and backup checks.

## Open Questions to Resolve Soon

- Existing database schema and attachment-table design
- AWS region and bucket naming
- Backend AWS authentication method
- Accepted file types and size limits
- Video multipart-upload timing
- HEIC handling
- LandGlide crop dimensions and output format
- Unassigned-upload cleanup policy
- Estimate statuses and versioning details
- Production authentication readiness
