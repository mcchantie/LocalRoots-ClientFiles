# Base44 Dashboard Build Prompt

Build a responsive production dashboard named **Client Files** for Texas Top Dressing and Lawn Leveling.

The dashboard is hosted entirely in Base44. Do not create or use Base44 database entities for contacts or attachments. The source of truth is an external Spring Boot REST API hosted on Railway. All application data operations must use that API.

Use an environment/config value named `CLIENT_FILES_API_BASE_URL` for the Railway origin. Do not hard-code AWS credentials, a tenant ID, or production passwords in frontend source code.

## Authentication

Create a login page with username and password fields. Submit JSON to:

```text
POST {CLIENT_FILES_API_BASE_URL}/api/v1/auth/login
```

On success, store only `accessToken`, `expiresAt`, and username in session storage. Send the token on protected API calls as:

```text
Authorization: Bearer <token>
```

Call `/api/v1/auth/me` when restoring a session. On any `401`, clear the session and return to login. Include a visible Sign Out action that clears the session locally. Do not use cookies or `X-Tenant-Id`.

## Main layout

Create a desktop-first responsive layout that works well on an iPhone:

- Left sidebar on desktop, collapsible drawer on mobile
- Header with Client Files title, current user, upload button, and sign out
- Navigation items: All Files, Unassigned, Deleted, and Contacts
- Contact search in the sidebar
- Main file gallery/list area

Keep the visual design clean and practical for daily office work. Favor compact spacing, large click targets, legible filenames, and clear upload status. Avoid decorative animations.

## Contacts

Use the Railway endpoints under `/api/v1/contacts`.

Support:

- Contact list and search
- Create contact
- Edit contact
- Contact details
- Viewing files assigned to a contact

A contact may have no name. Require at least a phone number or an email address. When a name is missing, display the API-provided `label`, phone number, or email rather than showing a blank row.

Contact form fields:

- First name, optional
- Last name, optional
- Display name, optional
- Phone, optional when email is present
- Email, optional when phone is present
- Notes, optional

Show duplicate phone/email conflicts returned by the API.

## Files

Use the Railway endpoints under `/api/v1/attachments`.

Support:

- All files
- Files for a selected contact
- Unassigned files
- Deleted files
- Category filter
- Status filter
- Pagination or Load More
- Image thumbnail cards
- Useful generic cards for PDFs, videos, and other documents
- Filename, display name, category, size, status, upload date, and assigned contact
- Open, Download, Assign/Reassign, Move to Unassigned, Delete, and Restore actions

Categories are:

- ESTIMATES
- LANDGLIDE
- PROPERTY_PHOTOS
- VIDEOS
- DOCUMENTS
- OTHER

Use friendly labels in the UI.

## Upload workflow

Allow file picker, drag and drop, and mobile camera/photo selection.

For each file:

1. Collect category, optional contact, and optional display name.
2. POST metadata to `/api/v1/attachments/uploads`.
3. Upload the raw file directly to the returned S3 `uploadUrl` using the returned HTTP method and every entry in `requiredHeaders`.
4. Do not send the Railway bearer token to S3.
5. After S3 succeeds, POST `/api/v1/attachments/{attachmentId}/complete` to Railway.
6. Only show success when the completion response reports `READY`.
7. Show upload progress when the Base44 runtime supports it; otherwise show clear per-file phases: Preparing, Uploading, Verifying, Ready, or Failed.
8. Preserve failed items in the upload panel with a Retry action.

Support batch uploads. Files may be uploaded without a contact and later assigned.

## Opening files

Never construct S3 URLs. Request a temporary URL from:

```text
POST /api/v1/attachments/{attachmentId}/download-url?download=false
```

For downloads use `download=true`. Open the returned URL immediately because it expires.

For images, make it easy to open the full image in a new browser tab so the user can use normal browser copy, drag, and paste behavior with Quo.

## Error handling

Create one shared API client. Parse `application/problem+json` responses and show the API `title`, `detail`, and field errors. Preserve and expose `X-Correlation-Id` in an expandable technical-details area for support.

Do not silently retry authentication failures. Do not expose stack traces or secrets.

## Initial production scope

This is a single-admin, single-tenant first release. Do not add Base44 user roles, Base44 data storage, Quo integration, LandGlide auto-cropping, or estimate creation yet. Structure components so those can be added later without replacing the contacts and file gallery screens.
