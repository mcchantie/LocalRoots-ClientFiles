# UTF-8 Plain-Text Estimate Viewer

Client Files now treats Estimator estimates as UTF-8 plain-text attachments and provides an authenticated in-app text-content endpoint.

## Backend behavior

- Plain-text attachments are stored with the canonical content type `text/plain; charset=utf-8`.
- New plain-text uploads are limited by `CLIENT_FILES_MAX_TEXT_FILE_SIZE` (default `5MB`).
- Upload completion validates that every `text/plain` object contains valid UTF-8 bytes before marking it `READY`.
- Inline and download URLs override the S3 response content type with the UTF-8 charset.
- `GET /api/v1/attachments/{attachmentId}/text-content` returns the exact file bytes as `text/plain;charset=UTF-8` with private, no-store caching.
- A UTF-8 byte-order mark is removed from the in-app response so it cannot appear as a stray character at the beginning of the file.
- Invalid UTF-8 returns a clear `422 Unprocessable Entity` problem response instead of displaying corrupted punctuation.

No database migration is required.

## Frontend behavior

The Base44 app should fetch the authenticated text-content endpoint, decode `arrayBuffer()` with `new TextDecoder("utf-8", { fatal: true })`, and render the result as literal text in a `<pre>` element using `white-space: pre-wrap`.

The UI must preserve characters such as:

```text
Estimated Lawn Size: ~5,420 sq ft

• Standard option — 5 cubic yards
• Moderate option — 6 cubic yards
```
