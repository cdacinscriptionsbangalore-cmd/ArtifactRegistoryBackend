---
name: Test Task issue template
about: Describe this issue template's purpose here.
title: 'TEST: '
labels: 'enhancement, type: test'
assignees: ''
type: Task

---

## Task: Tests — [Feature Name]
**Type:** Tests
**Feature:** #12 Login Flow
**Estimate:** [S / M / L]

### Scope
- [ ] Integration tests (RTL + MSW)
- [ ] Unit tests (hooks/utils)
- [ ] E2E test (Playwright) — only if critical flow

### Test Cases

**LoginForm component**
- [ ] Renders email and password fields
- [ ] Shows required error when submitting empty form
- [ ] Shows invalid email error on bad format
- [ ] Disables inputs and button while loading
- [ ] Displays error prop message

**useLogin hook**
- [ ] Stores token on 200 response
- [ ] Returns error message on 401
- [ ] Returns generic error on 500

**Login Page (integration)**
- [ ] Shows skeleton/loading state while submitting
- [ ] Redirects to /dashboard on success
- [ ] Shows toast on server error
- [ ] Preserves ?redirect= param after login

**E2E (Playwright)**
- [ ] User can log in with valid credentials and reach dashboard

### MSW Handlers Needed
- POST /api/auth/login → 200 (happy path)
- POST /api/auth/login → 401 (wrong credentials)
- POST /api/auth/login → 500 (server error)

### Acceptance Criteria
- [ ] All cases above pass
- [ ] Coverage does not drop below threshold
- [ ] No tests rely on implementation details (no testing state directly)
