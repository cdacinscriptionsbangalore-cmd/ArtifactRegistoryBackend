# Report System LLD Integration Plan

## 1. Brief Description of What This LLD Needs

The provided `ReportSystem.java` is a reference Low-Level Design for a full reporting and moderation workflow. It is not meant to be copied directly into the current backend as-is, because it uses plain Java classes and in-memory repositories, but it gives us the right design direction.

What the LLD is trying to achieve:

- allow a user to report a `Post`, `Comment`, or `User`
- validate the report before creation
- create reports through a factory instead of constructing them directly in controllers
- push every report through a moderation pipeline
- let AI handle obvious cases first
- escalate uncertain cases to human moderation
- maintain a controlled report lifecycle with valid status transitions
- store audit history for traceability
- keep reporting logic centralized in a service/facade

The main patterns in the LLD are:

- `Specification Pattern` for validation
- `Factory Pattern` for report creation
- `Chain of Responsibility` for moderation flow
- `Facade` through `ReportService`

## 2. What Already Exists in This Project

This project is already a Spring Boot + MongoDB backend with a layered structure:

- controllers
- services
- repositories
- Mongo entities/documents
- centralized exception handling
- JWT-based authentication

Existing related pieces already present in the codebase:

- `InscriptionPost` already contains embedded report metadata
- `PublicPostDescription` already contains embedded report metadata
- `User` already contains `reportCount` and `blackListed`
- there is already a content moderation module for post/comment text moderation
- there is already archive/delete support for posts and comments

Important current files:

- `src/main/java/com/cadac/stone_inscription/entity/InscriptionPost.java`
- `src/main/java/com/cadac/stone_inscription/entity/PublicPostDescription.java`
- `src/main/java/com/cadac/stone_inscription/entity/User.java`
- `src/main/java/com/cadac/stone_inscription/entity/model/Report.java`
- `src/main/java/com/cadac/stone_inscription/entity/model/ReportEntry.java`
- `src/main/java/com/cadac/stone_inscription/moderation/service/ContentModerationService.java`

## 3. Gap Between the LLD and the Current Project

The project already has some report-related fields, but not a proper report module yet.

What exists now:

- embedded `report` object on posts/comments
- basic reporter list structure through `ReportEntry`
- per-user `reportCount`
- content moderation for creation of posts/comments

What is missing compared to the LLD:

- a dedicated `Report` document/collection for each report case
- report APIs:
  - `POST /report`
  - `GET /reports`
  - `POST /moderate/:id`
- a `ReportService` facade for orchestration
- a `ReportFactory`
- specification-based validation classes
- a proper moderation pipeline for reports
- a status model matching the reporting workflow
- human moderation flow
- audit log persistence for report actions
- repository support for querying reports by state/target/reporter

## 4. Integration Strategy

We should adapt the LLD into the current codebase, not replace existing post/comment/user modules.

The best integration approach is:

- keep the current `InscriptionPost`, `PublicPostDescription`, and `User` documents as the primary content models
- introduce a new dedicated report module for report tickets
- reuse existing entities as report targets through a Spring-friendly `Reportable` abstraction or resolver
- keep existing embedded `report` metadata only as supporting summary data if needed
- use the new report collection as the source of truth for moderation workflow

This means the system will have:

- existing content models unchanged as the main domain objects
- a new report document to track each report independently
- synchronization logic to update content/user summary fields after moderation decisions

## 5. How the LLD Maps to This Project

### 5.1 Reportable

LLD idea:

- `Post`, `Comment`, and `User` implement `Reportable`

Project adaptation:

- we do not need to heavily modify every existing entity with business logic
- instead, we can use one of these two approaches:

Option A:

- make `InscriptionPost`, `PublicPostDescription`, and `User` implement a simple `Reportable` interface

Option B:

- create adapters/resolvers that expose:
  - target id
  - author id
  - target type

Recommended:

- Option B if we want minimal changes to existing domain classes
- Option A if the user wants a more explicit object-oriented mapping

For this codebase, minimal invasive integration is safer, so adapter/resolver style is likely the better fit.

### 5.2 Report Entity

LLD idea:

- one report object per user-submitted report
- contains target, reporter, reason, lifecycle state, audit log, AI score, action taken

Project adaptation:

- create a new Mongo document such as `moderation/report/entity/ModerationReport.java`
- keep the existing embedded `entity.model.Report` only if needed for summary counters/history inside content documents

Why this is needed:

- the current embedded `Report` model is only a lightweight counter + reporter list
- it cannot represent full lifecycle states, moderation decisions, or an audit trail cleanly

### 5.3 Validation Specifications

LLD idea:

- `NotSelfReportSpec`
- `NotDuplicateReportSpec`
- `ReporterNotBannedSpec`
- moderator-related constraints

Project adaptation:

- create specification classes under a dedicated report validation package
- use them inside `ReportService`
- translate failures into `StoneInscriptionException` or a dedicated reporting exception mapped by `ExceptionController`

Initial validations should include:

- reporter must exist
- target must exist
- reporter cannot report own content/user profile
- duplicate open report should be blocked
- banned/blacklisted reporter cannot file report
- reason must be valid
- details length limits should be enforced

### 5.4 Factory

LLD idea:

- `ReportFactory` centralizes report construction

Project adaptation:

- create a Spring component/factory that builds the report document with:
  - target type
  - target id
  - target author id
  - reporter id
  - reason
  - details
  - initial state
  - timestamps
  - audit entry

This is a good fit and should be implemented directly.

### 5.5 Moderation Pipeline

LLD idea:

- AI handler runs first
- escalates to human moderator when confidence is low
- human moderator finalizes the report

Project adaptation:

- create a dedicated report moderation chain, separate from content creation moderation
- do not mix it directly into `ContentModerationService`
- optionally reuse some scoring ideas or helper logic from existing moderation services

Pipeline stages should become:

- `PENDING`
- `AI_SCREENING`
- `ESCALATED`
- `RESOLVED`

If you want closer alignment with the LLD, we can internally keep more detailed terminal decisions, but your requested lifecycle can still remain:

- `PENDING -> AI_SCREENING -> RESOLVED / ESCALATED`

Then human resolution can move:

- `ESCALATED -> RESOLVED`

### 5.6 Human Moderation

LLD idea:

- a human moderator takes final action for escalated reports

Project adaptation:

- expose an endpoint to resolve escalated reports manually
- require authenticated moderator/admin access
- support actions such as:
  - dismiss
  - remove content
  - ban author
  - warn

For the current project, we need to verify how moderator/admin roles are represented in JWT authorities before wiring authorization rules.

### 5.7 Audit Logging

LLD idea:

- every report state transition writes to audit log

Project adaptation:

- store audit entries inside the new report document
- each entry should contain:
  - action
  - actor
  - timestamp
  - optional note

This is better than relying only on application logs, because report history must remain queryable.

## 6. Proposed Module Structure

To keep the codebase clean and aligned with the current style, I would introduce a new module like this:

```text
src/main/java/com/cadac/stone_inscription/report/
  controller/
  service/
  repository/
  dto/
  entity/
  enums/
  factory/
  moderation/
  specification/
  resolver/
```

Likely contents:

- `controller/ReportController.java`
- `service/ReportService.java`
- `repository/ModerationReportRepository.java`
- `dto/CreateReportRequest.java`
- `dto/ModerateReportRequest.java`
- `dto/ReportResponse.java`
- `entity/ModerationReport.java`
- `entity/ReportAuditEntry.java`
- `enums/ReportStatus.java`
- `enums/ReportTargetType.java`
- `enums/ReportReason.java`
- `enums/ModerationAction.java`
- `factory/ReportFactory.java`
- `moderation/ModerationHandler.java`
- `moderation/AiModerationHandler.java`
- `moderation/HumanModerationHandler.java`
- `specification/Specification.java`
- `specification/NotSelfReportSpecification.java`
- `specification/NotDuplicateReportSpecification.java`
- `resolver/ReportTargetResolver.java`

## 7. How It Will Use Existing Modules

### Existing Post Module

Used for:

- resolving reported post targets
- removing/rejecting content when moderation decides so
- possibly moving removed posts to archive through existing delete/archive services

### Existing Comment Module

Used for:

- resolving reported comment targets
- removing comments when required
- reusing archive/delete support already present in content delete services

### Existing User Module

Used for:

- resolving reported users
- reading reporter/author information
- updating `reportCount`
- updating `blackListed` if moderation thresholds are hit

### Existing Moderation Module

Used for:

- possibly reusing utility ideas for scoring or threshold-based decisioning

Not used directly for:

- content reporting workflow state management

Reason:

- current moderation service is designed for content creation screening, not user-submitted reporting workflows

## 8. API Plan

The requested API set can be added as a new controller.

### `POST /report`

Purpose:

- create a new report ticket for `POST`, `COMMENT`, or `USER`

Request likely includes:

- `targetType`
- `targetId`
- `reason`
- `details`

Behavior:

- extract reporter from JWT
- resolve target
- run validation specs
- create report via factory
- save report
- optionally leave it in `PENDING` or trigger AI moderation immediately depending on your preferred flow

### `GET /reports`

Purpose:

- list reports

Behavior:

- likely moderator/admin only
- support optional filtering:
  - by status
  - by target type
  - by reporter

### `POST /moderate/{id}`

Purpose:

- trigger or continue moderation

Possible behavior:

- if report is `PENDING`, run AI screening
- if report is `ESCALATED`, allow human moderator decision through request body

Because your requirement includes both AI and human moderation, this endpoint may either:

- act as a trigger endpoint for AI/human depending on state

or

- be split later into:
  - trigger moderation
  - resolve escalated report

I would keep your requested endpoint first and implement state-aware behavior inside the service.

## 9. Report Status Plan

Your requested status lifecycle is:

- `PENDING`
- `AI_SCREENING`
- `RESOLVED`
- `ESCALATED`

Recommended final state model for this project:

- `PENDING`
- `AI_SCREENING`
- `ESCALATED`
- `RESOLVED`
- optional `DISMISSED`

Why include `DISMISSED`:

- it is useful when a report is reviewed and found invalid
- otherwise `RESOLVED` becomes too broad

If you want strict adherence to your simplified lifecycle, we can keep only:

- `PENDING`
- `AI_SCREENING`
- `ESCALATED`
- `RESOLVED`

and encode the final action separately.

## 10. Data Model Recommendation

Because this backend uses MongoDB, the best fit is a dedicated collection for reports.

Recommended report document fields:

- `_id`
- `reporterId`
- `targetId`
- `targetType`
- `targetAuthorId`
- `reason`
- `details`
- `status`
- `actionTaken`
- `aiConfidenceScore`
- `createdAt`
- `updatedAt`
- `resolvedAt`
- `resolvedBy`
- `auditEntries`

Recommended indexes:

- `status`
- `targetType + targetId`
- `reporterId`
- `createdAt`
- optional uniqueness/index rule to prevent duplicate active reports from same reporter for same target

## 11. Minimal Working AI Moderation Logic

The first version should stay intentionally simple.

Suggested approach:

- assign base score by reason
- boost score if report details or content contains flagged keywords
- if score >= threshold:
  - auto-resolve with action
- else:
  - escalate

This matches the LLD and is enough for a first production-safe version if we keep actions conservative.

Safer initial auto-actions:

- for very clear spam/explicit cases:
  - mark resolved
  - optionally soft-remove content
- for uncertain cases:
  - escalate

## 12. Implementation Plan I Would Follow

This is the plan I would execute when you allow implementation.

### Phase 1: Design the new report module

- create report enums, DTOs, document models, and repository
- define report target types and status model
- add audit entry model

### Phase 2: Add target resolution

- create a resolver that can fetch:
  - `InscriptionPost`
  - `PublicPostDescription`
  - `User`
- expose a uniform report-target view for validation and moderation

### Phase 3: Add validation layer

- create specification interfaces and concrete validation rules
- centralize validation inside `ReportService`

### Phase 4: Add report factory

- build report creation through factory
- initialize first audit log entry and default status

### Phase 5: Add moderation pipeline

- implement AI moderation handler
- implement human moderation handler
- wire them using chain-of-responsibility style

### Phase 6: Add controller endpoints

- `POST /report`
- `GET /reports`
- `POST /moderate/{id}`

### Phase 7: Integrate with existing content/user modules

- apply moderation action to post/comment/user
- update existing summary report fields if still required
- increment user report counters where appropriate

### Phase 8: Add error handling and audit safety

- ensure invalid transitions are blocked
- ensure target-not-found cases are handled cleanly
- return consistent error messages

### Phase 9: Verify with tests

- create basic service/unit tests for:
  - duplicate prevention
  - self-report prevention
  - AI auto-resolution
  - escalation path
  - invalid state transitions

## 13. What I Would Not Do

To avoid unnecessary churn, I would not:

- rewrite existing post/comment/user modules
- remove current moderation features
- force every existing entity into a heavy inheritance model
- replace existing response/error style unless necessary
- overengineer the first AI moderator

## 14. Final Recommendation

The LLD is good as a behavioral blueprint, but it should be translated into Spring Boot + Mongo idioms instead of copied literally.

Recommended implementation direction:

- create a new report module
- keep existing content/user modules intact
- use a dedicated Mongo report collection as the source of truth
- reuse existing moderation and archive/delete modules where they already fit
- preserve the LLD patterns in Spring-friendly form:
  - specifications for validation
  - factory for report creation
  - service facade for orchestration
  - chain of responsibility for moderation

This gives us a clean implementation that matches your LLD while staying natural to the current codebase.
