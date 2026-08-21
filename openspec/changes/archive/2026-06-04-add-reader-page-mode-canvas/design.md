## Context

`ReaderContent` currently documents page mode as future work and always delegates to `ScrollModeContent`. `ReaderViewModel` still includes `onPagerChapterChanged`, `updatePagePosition`, and pager direction state, but there is no `PageModeContent` implementation in the current reader tree.

## Goals / Non-Goals

**Goals:**
- Implement page mode as a first-class rendering path using the existing canvas engine.
- Preserve cross-chapter navigation in both directions.
- Handle title-only and very short chapters as reachable pages.
- Persist page index progress separately from scroll paragraph progress.

**Non-Goals:**
- Add page-turn animations beyond the currently modeled setting contract.
- Rebuild old Compose element views or `SubcomposeLayout`.
- Change `ReaderEngine` pagination semantics unless a tested engine bug is found.

## Decisions

- Build `PageModeContent` as `HorizontalPager` over a flattened list of chapter page entries.
- Compute page anchors per chapter using the same cache-key strategy as scroll mode, including settings and viewport dimensions.
- Include placeholder/loading page entries while anchors for a loaded chapter are pending, so pager indices do not collapse and title-only chapters remain reachable.
- Keep tap zones in `PageModeContent`: left for previous page, center for menu, right for next page.

## Risks / Trade-offs

- Flattened multi-chapter page lists can shift as anchors finish computing. Mitigation: keep stable keys by chapter index and page index, and keep placeholders until real anchors replace them.
- Page mode and scroll mode may diverge in progress semantics. Mitigation: test both `chapterPageIndex` and scroll-percent update paths.
