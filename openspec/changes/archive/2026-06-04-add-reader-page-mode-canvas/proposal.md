## Why

Reader settings still model `PageMode.PAGE`, and the ViewModel retains pager-related methods, but the current UI always renders scroll mode. Users need the page mode setting to produce a real horizontal canvas reader again.

## What Changes

- Add a canvas-backed `PageModeContent` using `ReaderEngine`, `ReaderPageCanvas`, and horizontal paging.
- Branch `ReaderContent` by `ReaderSettings.pageMode`.
- Support cross-chapter page navigation, title-only chapters, and page progress persistence.
- Reuse existing chapter content and page-anchor cache behavior where possible.

## Capabilities

### New Capabilities
- `client-reader-page-mode-canvas`: Horizontal page-mode reader backed by the canvas reader engine.

### Modified Capabilities

## Impact

- Affects reader content components, page-mode settings, pager-related ViewModel callbacks, and tests.
- No backend or database schema changes are planned.
