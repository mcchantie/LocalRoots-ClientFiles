# Plain-Text Estimate Support

Client Files accepts and displays Estimator-generated UTF-8 `.txt` attachments.

## Backend changes

- `text/plain` is allowed as an upload content type.
- Plain-text objects are stored and served as `text/plain; charset=utf-8`.
- UTF-8 validity is checked before an uploaded text file becomes `READY`.
- `text/plain` files remain classified as `DOCUMENT`.
- Download-extension inference uses `.txt`.
- The authenticated endpoint `GET /api/v1/attachments/{attachmentId}/text-content` supports an in-app text viewer.
- The default text-file limit is 5 MB and can be changed with `CLIENT_FILES_MAX_TEXT_FILE_SIZE`.

## Deployment

No database migration is required. Deploy Client Files before testing the updated Estimator so UTF-8 text uploads and the text viewer endpoint are available.
