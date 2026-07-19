# File Browser and Estimator Backend Update

## Added Client Files API capabilities

### Search, file type filtering, and sorting

Both attachment list routes now accept:

- `search`
- `category`
- `fileKind`
- `status`
- `sortBy`: `NAME`, `CREATED_AT`, `UPDATED_AT`, or `SIZE`
- `sortDirection`: `ASC` or `DESC`
- `page`
- `size`

Routes:

```text
GET /api/v1/attachments
GET /api/v1/contacts/{contactId}/attachments
```

`search` matches display name, original filename, content type, source system, and description.

### Batch assignment and categorization

```text
POST /api/v1/attachments/batch-update
```

```json
{
  "attachmentIds": ["uuid-1", "uuid-2"],
  "updateContact": true,
  "contactId": "contact UUID or null",
  "category": "PROPERTY_PHOTOS"
}
```

- Set `updateContact` to `true` to assign, reassign, or unassign selected attachments.
- Use `contactId: null` with `updateContact: true` to move selected files to Unassigned.
- Set `updateContact` to `false` when changing only the category.
- Category compatibility is still validated against the stored file kind.
- Maximum batch size is 100 attachments.

### Plain-text estimate support

Client Files now allows `text/plain` uploads and maps plain-text downloads to the `.txt` extension. Estimator-generated attachments use UTF-8 text, category `ESTIMATES`, file kind `DOCUMENT`, and source `ESTIMATOR`.

### Estimator source support

Authenticated uploads may now use `sourceSystem: ESTIMATOR` and supply their Estimator UUID in `estimateId`. The attachment is still tenant-scoped through the authenticated Client Files JWT.

## Required production SQL

Client Files production uses `ddl-auto: none` and Flyway is disabled. Run the following reviewed migration against the production Client Files PostgreSQL database before deploying the updated Estimator integration:

```text
src/main/resources/db/migration/V2__support_estimator_source_and_file_browsing.sql
```

This updates the `source_system` check constraint to allow `ESTIMATOR` and adds file-browser indexes.

## Validation performed

All Client Files main Java sources were compiled with Java 17 against the dependencies packaged in the existing application JAR.
