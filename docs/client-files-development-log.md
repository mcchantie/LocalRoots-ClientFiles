# Client Files Development Log

## Session: July 9, 2026

### Session Goal

Define the purpose, architecture, storage model, environment strategy, estimate integration, and first implementation steps for the Local Roots CRM client attachment application.

### Questions Discussed

- Should the environment used for real Texas Top Dressing client records be considered UAT or production?
- Is it necessary to maintain development, UAT, and production environments immediately?
- Should data be synchronized from UAT into development to recover from destructive development work?
- Should estimate PDFs be the only stored representation of an estimate?
- Which estimate details should be stored as structured data for future reporting and analytics?
- How should attachments connect to records in the existing `contact_attachments` table?
- How should uploads work when a contact does not exist yet?
- Should the system allow an attachment to remain temporarily unassigned?
- Should photos, videos, documents, estimates, and LandGlide screenshots all use the same attachment model?
- Should LandGlide screenshots have a separate folder or section?
- How should the timestamp or iPhone status bar be removed from LandGlide screenshots?
- Should the original LandGlide screenshot be retained after cropping?
- Does the application need a custom “Use in message” action for Quo?
- Can browser click, drag, copy, and paste behavior be used instead?
- What should the attachment application be called?
- How should development-session notes be preserved and reloaded in future programming sessions?
- What components are required beyond S3, a Spring Boot backend, video upload support, image cropping, and the Local Roots database?

### Design Decisions

- The attachment application will be called **Client Files**.
- The pricing calculator will be renamed **Estimater**.
- The environment containing real client and business records will be treated as **production**, even while the CRM is still under active development.
- The initial environment model will use:
  - Development
  - Production
- UAT will be added later when a separate pre-production release-testing environment becomes useful.
- Development and production will use separate databases, S3 storage, credentials, deployments, and configuration.
- Production data will not be automatically overwritten from development or UAT.
- Recovery from destructive development work will rely on environment separation, backups, seed data, RDS recovery, S3 versioning, and soft deletion.
- Production data may later be copied downward into development only through a controlled, sanitized process.
- Actual files will be stored in Amazon S3.
- RDS will store attachment metadata and relationships, not file bytes.
- The existing `contact_attachments` model will connect files to contacts, tenants, estimates, upload sessions, processed derivatives, and storage metadata.
- Files may be:
  - Assigned to an existing contact
  - Assigned while creating a new contact
  - Temporarily stored as unassigned
- S3 key prefixes will organize objects physically, but database fields will be the primary source of searchable organization.
- Client Files will visibly categorize:
  - Estimates
  - LandGlide screenshots
  - Property photos
  - Videos
  - Documents
  - Other files
- LandGlide screenshots will have their own section or category.
- The original LandGlide screenshot will be preserved.
- A cropped derivative will be stored separately and linked to the original.
- The first LandGlide crop implementation may use a device- or dimension-based fixed crop with a preview or manual adjustment option.
- Intelligent image detection can be added later.
- Client Files does not need a custom Quo “Use in message” action in the first version.
- The interface should make images easy to open, drag, copy, paste, and download using standard browser behavior.
- The generated estimate document will be stored in S3 and represented in `contact_attachments`.
- Estimate data will also be stored in structured relational tables for analytics.
- Structured estimate data should include lawn area, quoted material, service selections, discounts, totals, pricing version, and calculator input/output.
- Estimate revisions should be versioned rather than overwriting previously presented estimates.
- Estimater should treat the structured estimate record as the source of truth and the PDF as the client-facing snapshot.
- Large files and videos should upload directly from the browser to S3 using presigned URLs.
- The frontend will not receive permanent AWS credentials.
- Image cropping and thumbnail generation should be represented as processing work rather than blocking a normal upload request.
- The initial processor may run inside Spring Boot; Lambda, SQS, or a separate worker can be introduced later.
- A living documentation process will use three Markdown documents:
  - `client-files-development-log.md`
  - `architecture-decisions.md`
  - `current-development-status.md`
- At the end of a programming session, the notes should be grouped into:
  - Questions discussed
  - Design decisions
  - Work completed
  - Unresolved questions
  - Next steps

### Work Completed

- Defined the purpose and scope of Client Files.
- Chosen the product name **Client Files**.
- Chosen the pricing-tool name **Estimater**.
- Decided to treat real business usage as production rather than UAT.
- Selected an initial two-environment strategy: development and production.
- Defined the safe direction for future data copying as production to sanitized development.
- Defined S3 as the file-storage layer and RDS as the metadata and relationship layer.
- Identified the need to preserve both original and processed LandGlide screenshots.
- Decided not to build a Quo-specific message attachment workflow initially.
- Defined the initial attachment categories.
- Defined the upload assignment options for existing, new, and unassigned contacts.
- Identified structured estimate data needed for future analytics.
- Defined the relationship between Estimater, estimate records, generated PDFs, S3, and `contact_attachments`.
- Identified the major frontend, backend, AWS, database, processing, security, and operational components.
- Established a repeatable session-documentation format.
- Created the initial development log, architecture decision record, and current-status handoff documents.

### Unresolved Questions

- What columns already exist in the current `contact_attachments` table?
- Should the existing table be extended or should supporting tables be added around it?
- Does the database already have a property or service-address table separate from contacts?
- What production and development S3 bucket names should be used?
- Which AWS region should hold the Client Files bucket?
- Will the Spring Boot backend run on Railway with AWS access keys, or later move to an AWS role-based deployment?
- What maximum image, document, and video sizes should be allowed?
- Which image and video MIME types should be accepted in the first release?
- Should HEIC files be retained, converted for preview, or both?
- Should video uploads use single-part presigned uploads initially or multipart uploads immediately?
- How will incomplete or abandoned uploads be cleaned up?
- How long should unassigned uploads remain before review or cleanup?
- Should LandGlide processed files be PNG or JPEG?
- Which iPhone screenshot dimensions and status-bar crop profiles must be supported first?
- Should the first crop remove the entire status bar or only mask/crop the timestamp area?
- Should users be able to manually adjust the LandGlide crop before saving?
- Should attachment notes be a plain description field or support tags and structured metadata?
- What estimate statuses are required?
- Which estimate fields should be direct columns versus stored only in calculator JSON?
- How should estimate revisions and client approval be modeled?
- Should generated estimate PDFs be produced by the Spring Boot backend or another service?
- What authentication and tenant-login work must be completed before Client Files can safely use real production data?
- What backup, retention, lifecycle, and deletion policies should be configured for S3?
- What production-readiness checks are required before the first real client upload?

### Next Steps

1. Inspect the current Local Roots database schema, especially `contact_attachments`, contacts, tenants, users, and any property/address tables.
2. Create separate development and production S3 buckets, beginning with the production bucket for real business files.
3. Enable S3 Block Public Access and versioning.
4. Decide the AWS region and naming convention.
5. Create restricted IAM permissions for the Spring Boot backend.
6. Add environment-specific S3 configuration to Spring Boot.
7. Finalize the attachment metadata schema and supporting tables.
8. Implement authentication and tenant authorization requirements for Client Files.
9. Implement the upload-initialization endpoint and presigned image/document upload flow.
10. Add upload completion verification and attachment status transitions.
11. Build the Client Files frontend upload screen with existing-contact search, new-contact creation, and unassigned uploads.
12. Build the contact file gallery and category filters.
13. Test standard browser open, drag, copy, and paste behavior with Quo.
14. Add image thumbnails and the first LandGlide fixed-crop processor.
15. Define the initial `estimates`, `estimate_line_items`, and estimate-versioning schema.
16. Connect Estimater saving to structured estimate records and generated S3 documents.
17. Add soft deletion, restore behavior, error logging, and failed-upload visibility.
18. Add UAT later when release testing requires a stable environment between development and production.
