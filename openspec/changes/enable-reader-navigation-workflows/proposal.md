## Why

The reader ViewModel still emits scroll effects for TOC/bookmark jumps, but `ReaderScreen` does not execute them against the scroll content. Users need reliable TOC, bookmark, and saved-position workflows before page mode and richer inline interactions are added.

## What Changes

- Implement programmatic scroll-to-position for the current scroll-mode canvas list.
- Restore TOC and bookmark workflows as reader actions.
- Wire add/delete bookmark actions to visible UI and current reader position.
- Restore saved scroll position after chapter content and page anchors are ready.

## Capabilities

### New Capabilities
- `client-reader-navigation-workflows`: TOC jumps, bookmark management, and saved-position restoration in the client reader.

### Modified Capabilities

## Impact

- Affects `bookd-ui` reader shell, scroll-mode content contract, `ReaderEffect.ScrollToPosition`, bookmarks state, and tests.
- No backend API changes are planned; existing bookmark and progress endpoints are reused.
