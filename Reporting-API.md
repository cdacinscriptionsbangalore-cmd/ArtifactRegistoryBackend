# Reporting API

## Overview

| Property | Value |
|---|---|
| **Base URL (Production)** | `https://inscriptions.cdacb.in/api` |
| **Authentication** | `Authorization: Bearer <jwt-token>` — required on all endpoints |

---

## Endpoints at a Glance

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/report` | Submit a new report |
| `GET` | `/reports` | Fetch all reports |
| `POST` | `/moderate/{id}` | Moderate a specific report |

---

## 1. `POST /report`

> Submit a report against a Post, Comment, or User for moderation review.

**Auth:** `Required`  
**Roles:** `user` · `admin`

### Request

```http
POST https://inscriptions.cdacb.in/api/report
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

### Request Body

```json
{
  "targetType": "POST",
  "targetId": "6817a9d9f2b7b12c34d56789",
  "reason": "SPAM",
  "details": "This post is repeatedly promoting unrelated links."
}
```

### Fields

| Field | Type | Required | Validation |
|---|---|---|---|
| `targetType` | `enum` | Yes | `POST` \| `COMMENT` \| `USER` |
| `targetId` | `string` | Yes | Must be a valid existing resource ID |
| `reason` | `enum` | Yes | See Reason Values below |
| `details` | `string` | Yes | Max 1000 characters |

### `reason` Values

| Value | Description |
|---|---|
| `SPAM` | Unsolicited or repetitive content |
| `HATE_SPEECH` | Content promoting hatred or discrimination |
| `MISINFORMATION` | False or misleading information |
| `HARASSMENT` | Targeting or bullying another user |
| `EXPLICIT_CONTENT` | Inappropriate or adult content |
| `OTHER` | Any other violation not listed above |

### Examples

**Report a Post for Spam**

```json
{
  "targetType": "POST",
  "targetId": "6817a9d9f2b7b12c34d56789",
  "reason": "SPAM",
  "details": "This post is repeatedly promoting unrelated links."
}
```

**Report a User for Harassment**

```json
{
  "targetType": "USER",
  "targetId": "6817a9d9f2b7b12c34d56000",
  "reason": "HARASSMENT",
  "details": "This user has been sending threatening messages to multiple members."
}
```

---

## 2. `GET /reports`

> Fetch all moderation reports. Supports optional filtering by report status.

**Auth:** `Required`  
**Roles:** `admin` · `moderator` · `human_moderator` · `ai_moderator`

### Request

```http
GET https://inscriptions.cdacb.in/api/reports
Authorization: Bearer <jwt-token>
```

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `status` | `enum` | No | Filter reports by status. Omit to return all reports. |

### `status` Allowed Values

| Value | Description |
|---|---|
| `PENDING` | Report filed but not yet picked up by AI screening |
| `AI_SCREENING` | AI is currently evaluating the report |
| `ESCALATED` | AI flagged it — waiting for a human moderator |
| `RESOLVED` | Final decision made, report is closed |

### Example Requests

```http
GET /reports
Authorization: Bearer <jwt-token>
```
Returns **all reports** regardless of status.

```http
GET /reports?status=ESCALATED
Authorization: Bearer <jwt-token>
```
Returns only reports that are **waiting for human review**.

```http
GET /reports?status=PENDING
Authorization: Bearer <jwt-token>
```
Returns reports that are **queued for AI screening**.

---

## 3. `POST /moderate/{id}`

> Moderate an escalated report by taking a moderation action on the reported content or user.

**Auth:** `Required`  
**Roles:** `admin` · `moderator` · `human_moderator` · `ai_moderator`

### Request

```http
POST https://inscriptions.cdacb.in/api/moderate/{id}
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

### Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | Yes | The `_id` of the report to moderate (MongoDB ObjectId) |

### Request Body

```json
{
  "action": "REMOVE_CONTENT",
  "note": "Content clearly violates community spam guidelines."
}
```

### Body Fields

| Field | Type | Required | Validation |
|---|---|---|---|
| `action` | `enum` | **Yes** (for `ESCALATED` reports) | See Allowed Actions below |
| `note` | `string` | No | Moderator's reason or remarks. Max 1000 chars. |

> **Important:** When the report status is `ESCALATED`, the `action` field is **required**. Omitting it or sending an invalid value will return `400 Bad Request`.

### Allowed Actions

> These are the only actions a human moderator can submit. The AI uses `ESCALATE` and `NONE` internally — do not pass those.

| Action | What it does |
|---|---|
| `WARN` | Issues a warning to the content author. Content remains visible. |
| `REMOVE_CONTENT` | Deletes the reported post or comment. Increments author report count. |
| `BAN_AUTHOR` | Deletes the content and permanently bans the content author. |
| `BAN_REPORTER` | Blacklists the reporter (used when the report is false/abusive). Restores the reported content. |
| `DISMISS` | Dismisses the report as invalid. Restores the reported content. |

### Notes

- This endpoint is for **human moderation only**. It is called when a report has `status: ESCALATED` (i.e., the AI could not auto-resolve it).
- A moderator **cannot moderate their own content**. If the moderator's ID matches the content author's ID, the request will be rejected with `400`.
- The `action` field is **required for `ESCALATED` reports**. Only these values are accepted: `WARN`, `REMOVE_CONTENT`, `BAN_AUTHOR`, `BAN_REPORTER`, `DISMISS`.
- Every action is permanently recorded in `auditEntries` — this is the full audit trail for the report.
- **Side effects by action:**
  - `REMOVE_CONTENT` — deletes the post or comment from the platform.
  - `BAN_AUTHOR` — deletes the content and marks the author as blacklisted.
  - `BAN_REPORTER` — blacklists the reporter and restores the reported content to `ACCEPTED` status.
  - `DISMISS` — restores the reported content to `ACCEPTED` status, no penalty applied.
  - `WARN` — restores the content and increments the author's report count.
- Once a report is `RESOLVED`, calling this endpoint again will return `400 Bad Request`.
