---
name: Infra/Config Task
about: Infrastructure related and configuration specific issues.
title: 'CONFIG: '
labels: 'enhancement, type: infra'
assignees: ''
type: Task

---

## Task: [Setup Name] — e.g., "Axios Instance + Interceptors"
**Type:** Infrastructure
**Milestone:** Project Setup
**Estimate:** [S / M / L]

### What This Enables
Which features are blocked until this is done?
→ "All API hooks depend on this. Blocks entire auth milestone."

### Deliverables
- [ ] `src/lib/axios.ts` — configured instance with base URL
- [ ] Request interceptor — attaches Bearer token from storage
- [ ] Response interceptor — handles 401 (trigger refresh), 5xx (log + rethrow)
- [ ] Token refresh logic — queues failed requests during refresh

### Acceptance Criteria
- [ ] All requests include Authorization header when token exists
- [ ] 401 triggers token refresh, retries original request once
- [ ] 5xx errors are logged and rethrown as readable Error objects
- [ ] Unit tested: token attachment, 401 refresh, 5xx handling
- [ ] No raw fetch() calls anywhere else in the codebase

### Notes
- Use `axios-auth-refresh` library or implement manually — decide and document
- Refresh endpoint: POST /api/auth/refresh
