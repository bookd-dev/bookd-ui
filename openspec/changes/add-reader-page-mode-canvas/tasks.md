## 1. Page Mode Data Model

- [ ] 1.1 Add `PageModeContent` under the reader content package.
- [ ] 1.2 Define internal page entry data with stable chapter/page keys and loading placeholders.
- [ ] 1.3 Reuse or extract page-anchor cache helpers so page and scroll modes share cache semantics.

## 2. Page Mode Rendering

- [ ] 2.1 Render pages with `ReaderPageCanvas` and `ReaderEngine.prepareRenderCommands`.
- [ ] 2.2 Add horizontal pager behavior with left/center/right tap zones.
- [ ] 2.3 Preserve title-only chapters as reachable page entries.
- [ ] 2.4 Wire page changes to `ReaderViewModel.updatePagePosition` and chapter changes to pager callbacks.

## 3. Reader Integration And Tests

- [ ] 3.1 Branch `ReaderContent` by `ReaderSettings.pageMode`.
- [ ] 3.2 Keep settings changes from the reader shell able to switch between scroll and page modes.
- [ ] 3.3 Add tests for page list construction, title-only chapters, and boundary chapter transitions.
- [ ] 3.4 Run `cd bookd-ui && ./gradlew :shared:jvmTest` and the current compile task.
