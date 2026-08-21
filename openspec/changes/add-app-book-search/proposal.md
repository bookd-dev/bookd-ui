## Why

The Compose client already exposes a search action from the book source header, but the `RouteSearchBook` destination is empty. Users need that route to become a real App search screen for finding books by keyword across configured sources.

## What Changes

- Implement `SearchBookScreen` with query entry, initial, loading, empty, error, and result states.
- Add client API/repository support for backend App book search.
- Cache returned book rows in the existing SQLDelight `BookEntity` cache and use matching cached rows as fallback after network failure.
- Add `SearchBookViewModel` with pagination, query reset, and detail-navigation effects.
- Keep the book source header search action on the existing search-book route, now pointing to the implemented search experience.

## Capabilities

### New Capabilities

- `client-app-book-search`: Defines the Compose client search screen, state, cache, and navigation behavior.

### Modified Capabilities

- `client-booksource-refresh-actions`: Clarifies that the existing book source header search action opens the implemented App search experience.

## Impact

- Affected UI: `SearchBookScreen` and related search content/components.
- Affected navigation: existing `RouteSearchBook`, no new route required.
- Affected data layer: `BookApi`, `BookRepository`, SQLDelight `Book.sq`.
- Affected DI: `AppModules.kt` ViewModel registration.
- Affected tests: client repository and ViewModel JVM tests.
