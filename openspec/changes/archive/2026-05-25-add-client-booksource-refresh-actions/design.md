## Context

The book source screen already has pull-to-refresh for the current source, a search route, and repository methods that support forced refresh by clearing SQLDelight cache entries before fetching remote data. The missing workflow is a direct header action that refreshes all source-backed local data without requiring users to visit each source.

## Goals / Non-Goals

**Goals:**
- Show `Refresh` and `Search` as direct book source header controls.
- Remove the menu-only search interaction from the book source header.
- Refresh the source list and first page of books for every returned source through existing repository APIs.
- Avoid concurrent refresh-all operations while another source/book load is in progress.
- Cover multi-source refresh behavior with a focused JVM test.

**Non-Goals:**
- Do not add or change backend endpoints.
- Do not fetch every paginated page for every source.
- Do not change the existing search route.
- Do not redesign the book source list, paging, or pull-to-refresh behavior.

## Decisions

- Reuse the existing `BookSourceIntent.RefreshAll` intent instead of adding a second refresh intent. The intent now means source-list refresh plus per-source first-page refresh.
- Use the existing `forceRefresh` repository path for each source so stale local rows are replaced by fetched data.
- Keep per-source refresh sequential in the ViewModel. This avoids introducing new concurrency behavior into the cache update path and keeps failure handling straightforward.
- Make `ApiProvider` book source and book API accessors overridable so tests can inject fake APIs in the same style as existing reader repository tests.
- Disable the header refresh button while source or book operations are active to avoid overlapping cache clears and writes.

## Risks / Trade-offs

- [Risk] Refreshing many sources sequentially can take longer than parallel refresh. -> Mitigation: use existing paging and first-page refresh only; parallelization can be considered later if needed.
- [Risk] A later source refresh can fail after earlier sources have already updated. -> Mitigation: the ViewModel attempts every source and then surfaces the first failure through the existing global error path.
- [Risk] Header action text can take more horizontal space than the old icon-only menu. -> Mitigation: use compact Material text buttons with icons and reuse localized short labels.
