## Context

The backend authorization boundary now requires a valid logged-in user token for `/api/app/sources` and `/api/app/books`. The client already centralizes headers through `HeaderProvider`, with `UserRepository` providing the saved token.

## Decisions

- Keep client request paths unchanged.
- Rely on the existing shared header provider to attach bearer tokens.
- Keep existing not-authenticated and token-expired handling semantics.

## Non-Goals

- No client UI, navigation, storage, or repository redesign.
- No role-based source/book filtering on the client in this change.
