## ADDED Requirements

### Requirement: Client sends bearer tokens for authenticated API calls
The client SHALL attach the saved bearer token to backend API requests when a user session is available.

#### Scenario: Authenticated request is sent with a saved token
- **WHEN** the client sends an API request after a user session token has been saved
- **THEN** the request SHALL include `Authorization: Bearer <token>`
- **AND** it SHALL preserve the configured language header.

#### Scenario: App library entrypoints are requested
- **WHEN** the client requests app source or app book list entrypoints
- **THEN** the request SHALL use the shared API header provider so the backend can authorize the current user.

#### Scenario: No user token is available
- **WHEN** an authenticated backend API rejects a request because no token is available
- **THEN** the client SHALL surface the existing not-authenticated or token-expired handling path rather than treating the response as successful data.
