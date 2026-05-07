---
name: Hook/Data Task issue template
about: Create a report to describe hook or data requirements.
title: 'HOOK: '
labels: 'enhancement, type: hook'
assignees: ''
type: Task

---

## Task: [hookName] Hook
**Type:** Hook
**Feature:** #12 Login Flow
**Estimate:** [S / M / L]

### Hook Signature
```ts
const { mutate: login, isPending, error } = useLogin()
```

### API Call
| Method | Endpoint | Auth required |
|--------|----------|---------------|
| POST | /api/auth/login | No |

### Request
```ts
{ email: string, password: string }
```

### Response
```ts
{ token: string, refreshToken: string, user: User }
```

### Behavior
- On success: store token in httpOnly cookie (or localStorage per arch decision)
- On success: invalidate ['auth', 'me'] query
- On success: call onSuccess callback
- On 401: surface "Invalid email or password" message
- On 5xx: surface "Something went wrong, try again"

### Acceptance Criteria
- [ ] Token stored correctly on success
- [ ] Error messages mapped from status codes
- [ ] Unit tested: success case, 401 case, 5xx case
- [ ] No token stored on failure
