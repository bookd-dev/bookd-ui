## Context

`ReaderEffect.ScrollToPosition` is currently collected but not implemented. `ScrollModeContent` owns the `LazyListState`, page-anchor map, and item-to-chapter mapping needed to convert a chapter target into a concrete list item. Current client models represent chapter content as a plain ordered `List<ContentElement>`; paragraph and render page indices are fragile when content is reparsed, images/footnotes are inserted, or reader pagination changes. The client needs to consume stable backend anchors when present and keep fallbacks for older cached content and existing saved positions.

## Goals / Non-Goals

**Goals:**
- Provide stable anchor-first scroll-mode jumps for TOC, bookmarks, and saved progress.
- Preserve compatibility by keeping chapter/page/paragraph index fallbacks when older progress, older bookmarks, or old cached chapter content has no anchor.
- Keep scroll mapping inside `ScrollModeContent` or a small helper near it, where content anchors, render anchors, and list state are available.
- Restore bookmark add/delete/jump workflows using existing endpoint paths with anchor-aware DTOs.

**Non-Goals:**
- Implement horizontal page mode jumps.
- Add annotation/highlight storage.
- Generate backend anchors in the client. Anchor generation belongs to the backend parser; the client only consumes and falls back.

## Decisions

- Extend client reader models with optional/defaulted `anchorId` on renderable `ContentElement` types so old cached JSON remains decodable.
- Represent programmatic jump targets as chapter index plus optional anchor id, paragraph/page fallback index, and scroll offset.
- Resolve jump targets by anchor id first. If the anchor is missing from the loaded chapter, resolve by fallback index and clamp to available render anchors.
- Extend the reader content contract with an explicit scroll request state or controller rather than trying to drive `LazyListState` from `ReaderScreen`.
- Delay programmatic scroll until the target chapter is present in `adjacentChapters`, content anchors are known, and page anchors are ready, avoiding no-op jumps while anchors are still calculating.
- Resolve TOC chapter jumps through `ReaderViewModel.jumpToChapter`, then allow the scroll content to move to the top of the loaded target chapter.
- Treat bookmark add/delete as ViewModel-owned state changes; UI surfaces only collect note input and dispatch actions.
- Treat programmatic jumps as a guarded state. The ViewModel SHALL ignore unrelated intermediate scroll observations until the target anchor or fallback index has been reached.

## Risks / Trade-offs

- Anchor readiness can make jumps feel delayed for large chapters. Mitigation: show a small in-reader loading affordance while a jump target is pending.
- Backend-generated anchors are not guaranteed to survive large text edits. Mitigation: keep index/offset fallback and prefer source HTML ids when available.
- Adding anchor fields changes DTO shapes and local cache JSON. Mitigation: make new fields nullable/defaulted and add tests for existing local progress and cached chapter data.
- A content `anchorId` maps to render page anchors rather than old raw element rows. Mitigation: document and test the mapping with title-inserted headings, sparse chapter windows, and missing-anchor fallback.
