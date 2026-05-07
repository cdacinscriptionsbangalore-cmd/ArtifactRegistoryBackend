---
name: Feature Ticket Template
about: Feature implementation Ticket
title: 'Feature: '
labels: enhancement, feature
assignees: ''
type: Feature

---

## Feature: [Name] — e.g., "Login Flow"

**Milestone:** Auth System
**Route(s):** /login
**Owner:** @username

---

### Context
What problem does this solve? Who uses it?
→ "Allows existing users to authenticate and reach the dashboard."

### API Contract
| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| POST | /api/auth/login | { email, password } | { token, user } |
| POST | /api/auth/logout | — | 204 |

### UI States to Handle
- [ ] Default (empty form)
- [ ] Loading (submit in progress)
- [ ] Validation error (client-side, before submit)
- [ ] Server error (wrong credentials, 401)
- [ ] Success (redirect to /dashboard)

### Tasks
- [ ] #21 — LoginForm component
- [ ] #22 — useLogin mutation hook
- [ ] #23 — Token storage + Axios interceptor
- [ ] #24 — Integration tests

### Out of Scope
- Password reset (separate feature #31)
- SSO / OAuth (milestone 3)

### Design Reference
[Figma link or screenshot]
