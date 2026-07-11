# Client Files Current Development Status

**Last updated:** July 10, 2026

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

AWS storage foundation and local Spring Boot S3 integration.

The development and production S3 buckets and initial IAM identities have been created. No presigned upload endpoint, upload-completion endpoint, processing worker, or Client Files frontend has been implemented yet.

## Confirmed Architecture

### Environments

Use two environments initially:

- Development
- Production

Development and production use separate S3 buckets, credentials, database configurations, and deployments. The environment holding real business data is production. UAT remains deferred.

### S3 Foundation

- AWS region: **`us-east-2`**
- Separate development and production Client Files buckets created
- Block Public Access enabled
- Object Ownership set to bucket-owner-enforced
- ACLs disabled
- Versioning enabled
- Objects remain private
- Bucket names identify the application and environment and include globally unique account, region, or suffix information

### IAM and Credentials

- Separate application IAM identities are used for development and production.
- Planned service-user names:
  - `localroots-client-files-dev-app`
  - `localroots-client-files-prod-app`
- Planned bucket-scoped policy names:
  - `LocalRootsClientFilesDevS3Policy`
  - `LocalRootsClientFilesProdS3Policy`
- The human `Developers` group currently has `AmazonS3FullAccess` for development-console work.
- Application runtime credentials should use the restricted environment-specific policies instead of the broad developer-group policy.
- Local and Railway configuration will use:
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`
  - `AWS_REGION`
  - `CLIENT_FILES_S3_BUCKET`
- Secret keys must not be committed to `application.yml`, source control, or the database.
- The AWS SDK default credential provider chain will resolve the credentials.

### Railway and Database Placement

- The backend will initially remain on Railway.
- The current Railway plan can access S3 over outbound HTTPS; no plan upgrade or static outbound IP is required for S3.
- The existing PostgreSQL database remains in AWS RDS for now.
- A migration from AWS RDS to Railway PostgreSQL is planned but deferred while S3 uploads are implemented.
- Do not delete the RDS database until backup, migration, validation, cutover, and rollback checks are complete.

### Spring Boot AWS Configuration

- Use AWS SDK for Java v2.
- Adding the project dependency is sufficient; there is no separate machine-wide SDK installation.
- Create shared `S3Client` and `S3Presigner` beans in:
  - `src/main/java/com/localroots/crm/config/S3Config.java`
  - Package `com.localroots.crm.config`
- Region and bucket may be referenced from Spring configuration; credentials remain in the AWS default provider chain.

### Upload Flow

1. Frontend requests an upload from Spring Boot.
2. Backend validates tenant and contact context.
3. Backend creates a pending attachment record.
4. Backend returns a presigned S3 upload URL.
5. Frontend uploads directly to S3.
6. Frontend confirms completion.
7. Backend verifies the object.
8. Backend marks the file ready or sends it for processing.

### Storage Boundary

- Amazon S3 stores file objects.
- PostgreSQL stores attachment metadata, relationships, search fields, processing state, and analytics data.
- The database provider may move from AWS RDS to Railway without changing the S3/metadata separation.

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
- Structured estimate data in PostgreSQL

Structured data should include lawn size, materials, services, discounts, totals, calculator inputs/outputs, pricing version, status, and revisions.

## S3 Console Clarification

`AmazonS3FullAccess` already includes S3 tagging actions. The **Actions → Edit tags** command on the Objects page is disabled when the bucket has no objects or no object is selected. Bucket-level tags are managed from the bucket **Properties** page.

## Work Completed

- Named the application Client Files.
- Renamed the pricing calculator concept to Estimater.
- Defined the initial development and production environment strategy.
- Defined S3 for file bytes and PostgreSQL for metadata.
- Created separate development and production S3 buckets in `us-east-2`.
- Established private access, versioning, and ACL-disabled bucket settings.
- Created environment-specific IAM policies and application users.
- Attached `AmazonS3FullAccess` to the human `Developers` group.
- Confirmed that no extra tag-editing policy is needed for the developer user.
- Confirmed that Railway can access S3 without upgrading the current plan.
- Deferred and documented the AWS RDS-to-Railway PostgreSQL migration.
- Defined the environment-variable credential strategy and local AWS verification process.
- Chosen the location for the shared Spring `S3Client` and `S3Presigner` beans.
- Defined upload assignment options, categories, LandGlide handling, and Estimater structured-data requirements.

## Immediate Next Step

Add AWS SDK for Java v2 to the Spring Boot backend and create `com.localroots.crm.config.S3Config` with reusable `S3Client` and `S3Presigner` beans. Then configure the development environment variables and run a controlled local S3 access test.

## Next Implementation Checklist

- [x] Choose AWS region.
- [x] Choose the S3 bucket naming approach.
- [x] Create the production S3 bucket.
- [x] Enable Block Public Access.
- [x] Enable S3 versioning.
- [x] Create the development S3 bucket.
- [x] Create environment-specific IAM policies and application users.
- [x] Confirm the human developer group's S3 permissions.
- [x] Decide how local and Railway AWS credentials will be supplied.
- [x] Decide where `S3Client` and `S3Presigner` beans will live.
- [ ] Add the AWS SDK for Java v2 S3 dependency.
- [ ] Create `S3Config` with `S3Client` and `S3Presigner` beans.
- [ ] Configure local development environment variables.
- [ ] Verify credentials with `aws sts get-caller-identity`.
- [ ] Complete a development-bucket upload/read/delete smoke test.
- [ ] Inspect the existing database schema.
- [ ] Confirm the current `contact_attachments` columns and constraints.
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
- [ ] Add soft deletion, restore, logging, lifecycle rules, and backup checks.
- [ ] Plan and execute the AWS RDS-to-Railway PostgreSQL migration after S3 integration is stable.

## Open Questions to Resolve Soon

- Exact AWS SDK v2 dependency/BOM version for the existing backend build
- Whether the development access keys and CLI verification are complete
- Existing database schema and attachment-table design
- Exact first-release S3 permissions required by the application
- Production secret storage and rotation in Railway
- Accepted file types and size limits
- Video multipart-upload timing
- HEIC handling
- LandGlide crop dimensions and output format
- Unassigned-upload cleanup policy
- S3 lifecycle rules for abandoned uploads and noncurrent versions
- Estimate statuses and versioning details
- Timing and runbook for the RDS-to-Railway PostgreSQL migration
- Production authentication readiness
