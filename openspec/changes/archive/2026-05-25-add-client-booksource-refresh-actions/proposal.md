## Why

The Compose client book source tab only exposed search through a menu and had no direct action for refreshing source-backed local caches. Users need a first-class refresh action that updates all configured book sources instead of only refreshing the currently visible list.

## What Changes

- Replace the book source header menu with peer `Refresh` and `Search` actions.
- Wire `Refresh` to refresh the app source list and the first page of books for every returned source, replacing stale local source/book cache entries.
- Keep `Search` on the existing search-book navigation route.
- Add ViewModel regression coverage for refreshing multiple sources and writing local cache state.

## Capabilities

### New Capabilities

- `client-booksource-refresh-actions`: defines direct book source header actions and all-source refresh behavior for the Compose client.

### Modified Capabilities

None.

## Impact

- Affected client UI: book source tab header.
- Affected client state: `BookSourceViewModel` refresh-all behavior.
- Affected client cache: SQLDelight source and per-source book cache entries refreshed through existing repositories.
- No backend API, database schema, route, or deployment contract changes.
