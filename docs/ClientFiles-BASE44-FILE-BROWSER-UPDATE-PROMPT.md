# Base44 Prompt: Update the Client Files App

Update the existing **Local Roots Client Files** Base44 app. Keep the current visual style, authentication flow, external Railway API integration, contacts, file cards, deleted-file behavior, and direct-to-S3 upload process. Do not create Base44 database entities for contacts or attachments. The Spring Boot Client Files API remains the source of truth.

Use the existing `CLIENT_FILES_API_BASE_URL` configuration and existing shared authenticated API client. Continue sending the Client Files bearer token in `Authorization: Bearer <token>` only to Railway API calls. Never send the bearer token to a presigned S3 URL.

## 1. Add client-specific upload

On every Contact Details page, add an **Upload files** button in the Files section toolbar. Keep the existing global Upload button too.

When upload is opened from a Contact Details page:

- Preselect that contact for every newly added file.
- Show a clear heading such as `Upload files for Patrick Blunt`.
- Do not make the user search for the contact again.
- Allow the user to intentionally change some or all files to another contact or to Unassigned.
- Preserve the selected contact when more files are added to the same upload session.

## 2. Redesign the multi-file upload queue

The upload screen must support selecting many files and assigning different subsets to different contacts and categories before upload.

For every queued file show:

- A selection checkbox.
- Image thumbnail when the file is an image. Create the thumbnail from the local browser `File` with `URL.createObjectURL(file)` before upload and revoke the URL when the item is removed or the upload panel closes.
- A useful file-type icon for text, PDF, document, video, or unknown files.
- Original filename.
- File size.
- Current contact assignment or `Unassigned`.
- Current category.
- Optional display name.
- Upload state: Queued, Preparing, Uploading, Verifying, Ready, or Failed.
- Remove action.
- Retry action for failed files.

Keep file-specific controls available, but do not require the user to repeat the same contact and category on every file.

## 3. Add defaults and subset bulk actions

At the top of the upload queue add defaults for newly added files:

- Default contact: selected contact or Unassigned.
- Default category.

A default is copied onto a file when it is added. Changing a default must not overwrite files that the user already changed individually unless the user explicitly chooses **Apply to all**.

Add selection controls:

- Select all.
- Deselect all.
- Individual checkboxes.
- Shift-click range selection on desktop.
- Show `<number> files selected`.

When at least one queued file is selected, show a sticky bulk-action toolbar:

- **Assign client**: search existing contacts and allow Unassigned.
- **Set category**.
- **Set display name pattern** only if practical; this is optional.
- **Remove selected**.

Bulk actions apply only to the selected queued files. This must allow one upload batch such as:

- Three files assigned to Todd Williamson / LandGlide Photos.
- Four files assigned to Patrick Blunt / Photos.
- One `.txt` estimate assigned to Unassigned / Estimates.

Use friendly category labels but send these exact API values:

- Estimates -> `ESTIMATES`
- LandGlide Photos -> `LANDGLIDE`
- Photos -> `PROPERTY_PHOTOS`
- Videos -> `VIDEOS`
- Documents -> `DOCUMENTS`
- Other -> `OTHER`

Every file must have a category before upload. A contact is optional.

Before starting, show a compact review summary grouped by contact and category. Example:

```text
8 files ready
Patrick Blunt: 4 Photos
Todd Williamson: 3 LandGlide Photos
Unassigned: 1 Estimate
```

## 4. Keep the existing direct-to-S3 upload contract

For each queued file, use that file's final contact and category values:

1. `POST /api/v1/attachments/uploads`
2. Upload the raw browser `File` to the returned presigned URL with the returned method and every returned required header.
3. Do not attach Railway authentication headers to the S3 request.
4. `POST /api/v1/attachments/{attachmentId}/complete`
5. Mark the item Ready only when completion returns `READY`.

Initialize request example:

```json
{
  "contactId": "contact UUID or null",
  "estimateId": null,
  "parentAttachmentId": null,
  "category": "PROPERTY_PHOTOS",
  "originalFileName": "front-yard.jpg",
  "displayName": "Front yard",
  "contentType": "image/jpeg",
  "sizeBytes": 2458134,
  "checksumSha256Base64": null,
  "sourceSystem": "MANUAL",
  "description": null,
  "metadata": {
    "capturedFrom": "client-files-dashboard"
  }
}
```

Upload files independently with limited concurrency, such as three at a time, so one failure does not cancel the whole batch. Preserve failed items for retry.

The new backend batch endpoint is for changing already-created attachments, not for configuring files that are still only in the local upload queue:

```text
POST /api/v1/attachments/batch-update
```

Request example to assign selected existing attachments and set a category:

```json
{
  "attachmentIds": ["uuid-1", "uuid-2"],
  "updateContact": true,
  "contactId": "contact UUID or null",
  "category": "LANDGLIDE"
}
```

Use `updateContact: true` with `contactId: null` to move selected existing attachments to Unassigned. Use `updateContact: false` when changing only the category.

## 5. Add grid and list file views

Add a Grid/List toggle to All Files, Unassigned, Deleted, and Contact Details file sections.

### Grid view

Retain the current card layout with consistent image ratios and clearly show:

- Thumbnail or file-type icon.
- Display name or filename.
- Category.
- Uploaded date.
- Size where useful.
- Status.
- Existing action menu.

### List view

Use a compact table or file-list layout with:

- Name.
- Category.
- Client on All Files and Deleted views.
- Type.
- Size.
- Uploaded/created date.
- Status.
- Actions.

Do not show a redundant Client column on a Contact Details page.

Persist the view preference in local storage. Use a separate preference for mobile only if necessary.


## 6. Open UTF-8 plain-text files inside Client Files

Treat attachments whose `contentType` starts with `text/plain` as viewable plain-text documents. This includes Estimator estimates saved as `.txt` files.

When the user chooses **Open** on a plain-text attachment, open an in-app text viewer modal or full-page viewer. Do not send the user to a raw S3 page for the primary viewing experience.

Fetch the authenticated text endpoint:

```text
GET /api/v1/attachments/{attachmentId}/text-content
Authorization: Bearer <Client Files token>
Accept: text/plain
```

The backend returns `text/plain;charset=UTF-8`. Read it as bytes and decode it explicitly as UTF-8:

```javascript
const response = await authenticatedFetch(
  `${CLIENT_FILES_API_BASE_URL}/api/v1/attachments/${attachment.id}/text-content`,
  { headers: { Accept: "text/plain" } }
);

if (!response.ok) {
  throw await parseProblemJson(response);
}

const bytes = await response.arrayBuffer();
const text = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
```

Do not use any of the following for text attachments:

- `atob`
- `String.fromCharCode`
- `FileReader.readAsBinaryString`
- Latin-1 or Windows-1252 decoding
- JSON stringification of the file body
- Markdown parsing
- `innerHTML`

Render the decoded value as literal text, not HTML and not Markdown. Use a `<pre>` or equivalent component with:

```css
white-space: pre-wrap;
overflow-wrap: anywhere;
word-break: normal;
font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
```

Preserve the exact line breaks, spaces, punctuation, and Unicode characters. A file containing the following must display exactly this way:

```text
Estimated Lawn Size: ~5,420 sq ft

• Standard option — 5 cubic yards
• Moderate option — 6 cubic yards
```

The bullet `•`, em dash `—`, tilde `~`, apostrophes, dollar signs, and accented names must not be replaced with sequences such as `â€¢`, `â€”`, `Ã`, `Â`, question-mark diamonds, or HTML entities.

Add these actions to the text viewer:

- **Copy text**, using `navigator.clipboard.writeText(text)` so the exact Unicode text and spacing are copied.
- **Download**, using the existing download URL flow.
- **Close**.

Do not trim, normalize, reformat, or reconstruct the text in the frontend. Display the exact string returned by the text-content endpoint.

If UTF-8 decoding fails, show a clear error that the file is not valid UTF-8 rather than displaying corrupted characters.

## 7. Add file search, filters, sorting, and pagination

Add a file search field above the file results. Search should call the backend rather than filtering only the currently loaded page.

Use these list query parameters:

- `search`: matches display name, original filename, content type, source, and description.
- `category`: existing category enum.
- `fileKind`: `IMAGE`, `VIDEO`, `DOCUMENT`, or `OTHER`.
- `status`: existing status enum.
- `sortBy`: `NAME`, `CREATED_AT`, `UPDATED_AT`, or `SIZE`.
- `sortDirection`: `ASC` or `DESC`.
- `page` and `size`.

Examples:

```text
GET /api/v1/attachments?search=lawn&sortBy=NAME&sortDirection=ASC&page=0&size=25
GET /api/v1/attachments?unassigned=true&fileKind=IMAGE&sortBy=CREATED_AT&sortDirection=DESC&page=0&size=25
GET /api/v1/contacts/{contactId}/attachments?search=landglide&category=LANDGLIDE&sortBy=UPDATED_AT&sortDirection=DESC&page=0&size=25
```

Sorting choices shown to the user:

- Newest first -> `CREATED_AT`, `DESC`
- Oldest first -> `CREATED_AT`, `ASC`
- Name A-Z -> `NAME`, `ASC`
- Name Z-A -> `NAME`, `DESC`
- Recently updated -> `UPDATED_AT`, `DESC`
- Largest first -> `SIZE`, `DESC`
- Smallest first -> `SIZE`, `ASC`

Debounce file search by roughly 300 ms. Reset to page 0 whenever search, filters, or sorting changes.

Store the current file browser state in URL query parameters when practical, including search, view, filters, sort, and page, so Back and Forward navigation works correctly.

## 8. File toolbar layout

Use a practical toolbar like:

```text
Files                                                   8 files
[ Search files...                         ] [ Upload files ]
[ All categories ] [ All types ] [ All statuses ] [ Sort: Newest ] [Grid][List]
```

Allow wrapping on smaller screens. Keep large touch targets on iPhone.

## 9. Contact rules

Do not change the existing contact rule:

- A contact name is optional.
- A contact must have at least a usable phone number or email address.
- Phone-only and email-only contacts must display correctly.

## 10. Error and session handling

Continue parsing `application/problem+json` and showing `title`, `detail`, field errors, and the correlation ID when available.

On `401`, clear the Client Files session and return to login. Do not silently retry expired authentication.

Do not rebuild or replace working pages. Update the existing components and API wrapper incrementally while preserving current delete, restore, open, download, assignment, contact editing, and authentication behavior.
