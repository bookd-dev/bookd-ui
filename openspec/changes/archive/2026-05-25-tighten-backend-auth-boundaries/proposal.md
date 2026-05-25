## Why

The backend now requires logged-in user authorization for app source and app book list entrypoints. The Compose client already uses a shared header provider for API calls, and this change documents that compatibility requirement.

## What Changes

- Document that authenticated client API calls attach the saved bearer token.
- Document that app library entrypoints use the shared API header provider.
- Preserve existing unauthenticated/token-expired client error handling expectations.

## Capabilities

### New Capabilities

- `client-authenticated-api-access`: defines client-side authenticated API header expectations.

### Modified Capabilities

None.

## Impact

- No client code changes are required by this spec update.
- Affected client contract: app source and app book list API requests depend on the shared header provider.
