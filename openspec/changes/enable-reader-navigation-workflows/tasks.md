## 1. Scroll Request Plumbing

- [ ] 1.1 Replace the TODO `ScrollToPosition` collector with a concrete scroll request contract for scroll mode.
- [ ] 1.2 Expose target chapter, paragraph, and offset information to `ScrollModeContent` without leaking `LazyListState` ownership.
- [ ] 1.3 Add pending-jump state so requests wait for chapter content and page anchors before scrolling.

## 2. TOC And Bookmark UI

- [ ] 2.1 Add TOC surface content using `ReaderState.manifest.toc` and current chapter state.
- [ ] 2.2 Wire TOC selection to `ReaderViewModel.jumpToChapter`.
- [ ] 2.3 Add bookmark list UI using `ReaderState.bookmarks`.
- [ ] 2.4 Wire add, select, and delete bookmark actions to existing ViewModel methods.

## 3. Progress Restoration

- [ ] 3.1 Restore saved local progress through the same pending-jump mechanism used by TOC and bookmarks.
- [ ] 3.2 Prevent programmatic jump transitions from saving unrelated intermediate positions.
- [ ] 3.3 Add tests for jump request creation, bookmark state updates, and progress restoration mapping.
- [ ] 3.4 Run `cd bookd-ui && ./gradlew :shared:jvmTest` and the current compile task.
