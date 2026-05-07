---
name: Page Task  issue template
about: Create a report to describe a task that needs to be developed.
title: 'Page: '
labels: 'enhancement, type: page'
assignees: ''
type: Task

---

## Task: [Page Name] Page
**Type:** Page
**Feature:** #12 Login Flow
**Estimate:** [S / M / L]

### Route
`/login` — public, redirects to /dashboard if already authenticated

### Components Used
- [ ] LoginForm (Task #21 — new)
- [ ] Button (design system)
- [ ] Input (design system)
- [ ] Toast (design system — for server errors)

### Behavior
- Redirect to /dashboard on successful login
- Redirect to /login if user hits protected route unauthenticated
- Preserve `?redirect=` query param and use it after login

### Acceptance Criteria
- [ ] Page renders without console errors
- [ ] Works on mobile (375px) and desktop (1280px)
- [ ] Loading state visible during submit
- [ ] Error message appears on wrong credentials
- [ ] Passes keyboard navigation (Tab through fields, Enter submits)

### Out of Scope
- "Remember me" checkbox
- Social login buttons
