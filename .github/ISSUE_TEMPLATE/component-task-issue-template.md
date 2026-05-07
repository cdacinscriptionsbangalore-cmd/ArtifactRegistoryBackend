---
name: Component Task issue template
about: Create a report to describe component functionality
title: 'COMPONENT: '
labels: enhancement
assignees: ''
type: Task

---

## Task: [ComponentName] Component
**Type:** Component
**Feature:** #12 Login Flow
**Estimate:** [S / M / L]

### Component Type
- [ ] Design system (no API calls, pure props)
- [ ] Feature component (may use hooks)

### Props Interface
```ts
interface LoginFormProps {
  onSuccess: (user: User) => void
  isLoading?: boolean
  error?: string | null
}
```

### Variants / States
| State | Description |
|-------|-------------|
| Default | Empty form, submit enabled |
| Loading | Inputs disabled, button shows spinner |
| Error | Error message shown below form |
| Success | Handled by parent via onSuccess callback |

### Acceptance Criteria
- [ ] Renders all states correctly
- [ ] Calls onSuccess with user data on valid submit
- [ ] Shows inline validation (email format, required fields)
- [ ] Does not call API directly (hook handles that)
- [ ] Unit tested (required fields, error display, onSuccess called)

### Notes
- Email field: type="email", autocomplete="email"
- Password field: toggle visibility icon
