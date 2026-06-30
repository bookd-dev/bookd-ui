## 1. Client Data Layer

- [x] 1.1 Add `BookApi.searchBooks` for `GET /api/app/books/search`.
- [x] 1.2 Add SQLDelight cached search and cached search count queries over `BookEntity`.
- [x] 1.3 Add `BookRepository.searchBooks` with trimmed query handling, network-first search, result upsert, and cached fallback.
- [x] 1.4 Add repository tests for blank query behavior, result caching, and cached fallback.

## 2. Search ViewModel

- [x] 2.1 Add `SearchBookState`, intents, and effects for query, results, loading, error, pagination, and detail navigation.
- [x] 2.2 Implement first-page search, load-more append, duplicate-operation guards, and query reset behavior.
- [x] 2.3 Register `SearchBookViewModel` in `AppModules.kt`.
- [x] 2.4 Add ViewModel tests for submit, blank query, load more, query reset, and result selection.

## 3. Search UI

- [x] 3.1 Implement `SearchBookScreen` using `rememberScreenContext` and Screen/Content separation.
- [x] 3.2 Add localized strings for the search field, initial state, no-result state, visible errors, and loading-more state.
- [x] 3.3 Render selectable result rows or cards with cover, title, author, format, and chapter count.
- [x] 3.4 Wire result selection to the existing `RouteBookDetail`.
- [x] 3.5 Add `AppPreviewContent` previews for initial, loading, empty, and populated states.

## 4. Verification

- [x] 4.1 Run targeted client JVM tests for search repository and ViewModel behavior.
- [x] 4.2 Run `openspec validate add-app-book-search --strict` in the client OpenSpec root.
