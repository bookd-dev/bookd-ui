## 1. Client Anchor Models

- [x] 1.1 Add nullable/defaulted `anchorId` support to client reader content models.
- [x] 1.2 Extend local progress and reader bookmark DTOs with chapter, anchor, fallback index, and offset fields while preserving existing stored-data fallback.
- [x] 1.3 Add decoding/mapping tests for old cached content and old local progress without anchors.

## 2. Scroll Request Plumbing

- [x] 2.1 Replace the TODO `ScrollToPosition` collector with a concrete anchor-first scroll request contract for scroll mode.
- [x] 2.2 Expose target chapter, anchor id, paragraph/page fallback, and offset information to `ScrollModeContent` without leaking `LazyListState` ownership.
- [x] 2.3 Add pending-jump state so requests wait for chapter content, content anchors, and page anchors before scrolling.
- [x] 2.4 Prevent programmatic jump transitions from saving unrelated intermediate positions.

## 3. TOC, Bookmark, And Progress UI

- [x] 3.1 Wire TOC selection to `ReaderViewModel.jumpToChapter` and close/dismiss the TOC after a request is accepted.
- [x] 3.2 Add bookmark list UI using `ReaderState.bookmarks`.
- [x] 3.3 Wire add, select, and delete bookmark actions to existing ViewModel methods using anchor-aware positions.
- [x] 3.4 Restore saved local progress through the same pending-jump mechanism used by TOC and bookmarks.

## 4. Validation

- [x] 4.1 Add client tests for anchor resolution, fallback mapping, jump request creation, bookmark state updates, and progress restoration mapping.
- [x] 4.2 Run `cd bookd-ui && ./gradlew :shared:jvmTest` and the current compile task.
- [x] 4.3 Run `cd bookd-ui && openspec validate enable-reader-navigation-workflows --strict`.
