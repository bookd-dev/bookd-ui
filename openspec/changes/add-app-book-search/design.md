## Context

`RouteSearchBook` is already registered in `AppNav` and the book source header navigates to it, but `SearchBookScreen` is currently empty. The client has `BookApi.getBooks` for source paging and `BookRepository` cache support by source, but no search API call, cached metadata search query, or search-specific ViewModel state.

## Goals / Non-Goals

**Goals:**

- Implement the existing search-book route without adding a new route.
- Add client API/repository support for backend App book search.
- Use network-first search and upsert returned rows into the existing `BookEntity` cache.
- Use cached `BookEntity` matches as fallback after network failure when available.
- Provide a search ViewModel with query, result, loading, empty, error, and pagination state.
- Build UI with Screen/Content separation, `rememberScreenContext`, localized strings, and `AppPreviewContent` previews.

**Non-Goals:**

- Do not add search history or persisted recent keywords.
- Do not add advanced filters beyond backend-supported optional `sourceId`.
- Do not redesign book source or bookshelf screens.
- Do not change navigation serializers because `RouteSearchBook` already exists.

## Decisions

- Add `SearchBookViewModel` rather than reuse `BookSourceViewModel`.
  - Rationale: search state is global and query-driven, while book source state is keyed by source tabs.
  - Alternative considered: store search state in `BookSourceViewModel`. Rejected to avoid coupling unrelated paging maps.

- Extend `BookRepository` with `searchBooks`.
  - Rationale: `BookRepository` already owns App book API access and SQLDelight `BookEntity` writes.
  - Alternative considered: create a separate `SearchRepository`. Rejected because it would duplicate book upsert/cache mapping.

- Add SQLDelight cached search queries over `BookEntity`.
  - Rationale: cached fallback needs structured queries instead of ad hoc filtering in Kotlin.
  - Alternative considered: fetch all cached books and filter in memory. Rejected because it scales poorly and bypasses SQLDelight query ownership.

- Trigger backend search only for submitted, trimmed non-blank queries.
  - Rationale: this avoids backend calls for empty input and makes query reset behavior deterministic.
  - Alternative considered: search on every keystroke. Rejected for the first pass because it needs debounce and cancellation behavior that is not required for functional search.

- Reuse existing book result display patterns.
  - Rationale: search results should feel consistent with source and bookshelf book cards.
  - Alternative considered: create a dense new search-only result design. Rejected until there are more search-specific signals to show.

## Risks / Trade-offs

- [Risk] Cached fallback may omit books never loaded into local cache. Mitigation: use backend results whenever available and treat fallback as a failure recovery path.
- [Risk] A submit-only interaction may feel less instant than type-ahead search. Mitigation: keep the ViewModel query boundaries simple now; debounce can be added later without changing the route.
- [Risk] Search result UI can drift from existing book item behavior. Mitigation: reuse existing cover/title/author/format/chapter display conventions and add preview coverage.

## Migration Plan

No navigation or database schema migration is required beyond adding SQLDelight queries. If rollback is needed, the search route can return to a minimal placeholder while leaving existing book source and bookshelf behavior unchanged.

## Open Questions

- Whether to add type-ahead debounce after the first usable search screen ships.
- Whether future filtering should include tags, bookshelf scope, format, or read status.
