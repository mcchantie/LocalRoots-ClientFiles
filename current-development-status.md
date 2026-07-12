# Client Files Current Development Status

**Last updated:** July 11, 2026

## Current Phase

The first Client Files backend structure is implemented. The immediate focus is validating local access to the development S3 bucket, inspecting the existing AWS development database, and designing database integration without automatic migrations.

## Environment Placement

| Environment | Application | PostgreSQL database | S3 |
|---|---|---|---|
| Development | Local machine | Existing AWS development database | Development Client Files bucket |
| Production | Railway | Railway PostgreSQL | Production Client Files bucket |

Development and production must use separate AWS credentials, JDBC credentials, buckets, configuration, and data.

The AWS development database is not currently planned to move to Railway. Railway production database setup and any required data transfer will be planned separately.

## Backend Implemented

- AWS SDK for Java v2 S3 integration
- Shared `S3Client` and `S3Presigner` Spring beans
- AWS default credential provider chain
- Tenant-scoped S3 object keys
- Presigned direct `PutObject` uploads
- Required signed upload headers
- `HeadObject` upload-completion verification
- Size, MIME type, ETag, and optional SHA-256 metadata handling
- Short-lived presigned view and download URLs
- Attachment categories and processing/status model
- Tenant-scoped retrieval and filtering structure
- Soft deletion and restoration behavior
- Fail-closed production authorization boundary until login/JWT integration is connected
- API error-handling structure
- Railway-compatible production packaging

## Database Safety Status

No database migration tool or migration file is included.

- No Flyway
- No Liquibase
- No automatic SQL migration
- `spring.jpa.generate-ddl=false`
- `spring.jpa.hibernate.ddl-auto=none`

Application startup will not create, update, migrate, or validate tables. The existing AWS development database must be inspected before attachment or contact schema changes are designed.

Future database changes will be explicitly reviewed before they run.

## Contact Model Requirement

Contacts must be allowed to exist without names.

- First name, last name, and display name must be nullable where applicable.
- A nameless contact may be created with only a phone number or only an email address.
- Attachments and estimate screenshots should link to the contact's database ID.
- A name added later should update the same contact.
- Phone numbers and email addresses should be normalized for matching and duplicate prevention.
- Display fallback: name, then phone number, then email address, then `Unnamed contact`.

The existing contacts schema and backend validation must be checked for name requirements before the future production database work begins.

## S3 Configuration

Non-secret S3 settings may remain in `application.properties`:

```properties
storage.s3.region=us-east-2
storage.s3.bucket=<development-bucket-name>
storage.s3.max-file-size=100MB
storage.s3.upload-url-ttl=15m
storage.s3.download-url-ttl=15m
storage.s3.allowed-content-types=image/jpeg,image/png,image/webp,image/heic,image/heif,application/pdf,video/mp4,video/quicktime
```

AWS credentials must remain outside committed application properties:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

No standalone AWS SDK installation is required. The AWS SDK for Java v2 is supplied through the Maven or Gradle dependency.

## IntelliJ Credential Configuration

The active Spring Boot or JUnit run configuration should use IntelliJ's parent-variable syntax:

```text
AWS_ACCESS_KEY_ID=$LOCALROOTS_CLIENT_FILES_ACCESS_KEY_DEV$
AWS_SECRET_ACCESS_KEY=$LOCALROOTS_CLIENT_FILES_SECRET_ACCESS_KEY_DEV$
```

`${VARIABLE_NAME}` is shell/property-placeholder syntax and was being passed literally by IntelliJ in this context.

Spring Boot and JUnit run configurations are separate. Credentials added to one configuration are not automatically guaranteed to exist in another. IntelliJ must also inherit the parent `LOCALROOTS_CLIENT_FILES_*` variables.

## Immediate Test

Run an S3-only smoke test that performs:

1. `PutObject` to the development bucket
2. `HeadObject` for the uploaded key
3. Confirmation in the AWS S3 console

This test does not require the attachment database table. Successful Spring Boot startup alone does not prove that the AWS credentials work because the SDK may not contact S3 until an operation is invoked.

## Important Security Boundary

Authentication is not yet integrated.

Any temporary tenant header or unverified contact/estimate relationship must remain development-only. Production must resolve tenant and uploader identity from authenticated claims and verify that related contacts and estimates belong to that tenant.

## Immediate Next Steps

1. Correct and verify IntelliJ credential substitution.
2. Run the development-bucket S3 smoke test.
3. Inspect the existing AWS development database schema.
4. Identify current database and Java validation that requires contact names.
5. Design nullable-name and attachment schema changes for explicit review.
6. Connect the local app to the AWS development database only after the expected schema is understood.
7. Plan Railway production PostgreSQL setup separately.
8. Add authenticated tenant/contact/estimate authorization.
9. Build the Client Files frontend upload screen and gallery.

## Deferred Work

- Automatic or reviewed database migration implementation
- Multipart video uploads
- Abandoned-upload cleanup
- Malware and file-signature inspection
- Thumbnail generation
- LandGlide crop processing
- Physical S3 purge after retention
- Estimate PDF generation and structured Estimater tables
- Production login and admin/tenant dashboard integration
