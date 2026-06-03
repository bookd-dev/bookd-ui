## Context

`ReaderEffect.ScrollToPosition` is currently collected but not implemented. `ScrollModeContent` owns the `LazyListState`, page-anchor map, and item-to-chapter mapping needed to convert a chapter/paragraph target into a concrete list item. Bookmarks are loaded and mutable in `ReaderViewModel`, but there is no visible workflow for users to add, delete, or jump to them.

## Goals / Non-Goals

**Goals:**
- Provide deterministic scroll-mode jumps for TOC, bookmarks, and saved progress.
- Keep scroll mapping inside `ScrollModeContent` or a small helper near it, where anchors and list state are available.
- Restore bookmark add/delete/jump workflows using existing APIs.

**Non-Goals:**
- Implement horizontal page mode jumps.
- Add annotation/highlight storage.
- Change backend bookmark or progress response shapes.

## Decisions

- Extend the reader content contract with an explicit scroll request state or controller rather than trying to drive `LazyListState` from `ReaderScreen`.
- Delay programmatic scroll until the target chapter is present in `adjacentChapters` and its page anchors are ready, avoiding no-op jumps while anchors are still calculating.
- Resolve TOC chapter jumps through `ReaderViewModel.jumpToChapter`, then allow the scroll content to move to the top of the loaded target chapter.
- Treat bookmark add/delete as ViewModel-owned state changes; UI surfaces only collect note input and dispatch actions.

## Risks / Trade-offs

- Anchor readiness can make jumps feel delayed for large chapters. Mitigation: show a small in-reader loading affordance while a jump target is pending.
- A paragraph index maps to page-anchor indices rather than old raw element rows. Mitigation: document and test the mapping with title-inserted headings and sparse chapter windows.
