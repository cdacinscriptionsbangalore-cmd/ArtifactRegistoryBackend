# Cookie Security Policy

## refreshToken cookie

- Cookie name: `refreshToken`
- `Secure=true` is enforced for production cookies.
- `SameSite=Lax` is used for `refreshToken`.
- Reason: the refresh token is consumed by same-site browser requests from the frontend origin `https://inscriptions.cdacb.in`, so `Lax` is sufficient and reduces CSRF exposure compared to `SameSite=None`.
- Additional protections:
  - CORS is configured to allow only `https://inscriptions.cdacb.in`.
  - The `/oauth2/authenticated/refresh-token` endpoint validates the `Origin` header and rejects requests from unknown origins.

## oauth_flow cookie

- Cookie name: `oauth_flow`
- `Secure=true` and `SameSite=None` are retained for the OAuth flow cookie.
- Reason: the OAuth callback from the identity provider is a cross-site redirect, so `SameSite=None` is required for the OAuth state flow cookie to be accepted by the browser during the redirect.

## Review

- Decision date: 2026-06-02
- Review cadence: review this cookie policy when the OAuth flow or frontend backend deployment topology changes.
