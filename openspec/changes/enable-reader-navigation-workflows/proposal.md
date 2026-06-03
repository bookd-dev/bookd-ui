## Why

The reader ViewModel still emits scroll effects for TOC/bookmark jumps, but `ReaderScreen` does not execute them against the scroll content. The current client models receive chapter content as a plain `List<ContentElement>` without stable element anchors, so saved progress and bookmarks rely on fragile paragraph/page indices. Users need reliable TOC, bookmark, and saved-position workflows that survive reader relayouts and reasonable reparses before richer inline interactions are added.

## What Changes

- Accept stable content anchors from chapter content APIs and preserve index fallback for older data.
- Persist anchor-aware local progress and send anchor-aware remote progress/bookmark positions.
- Implement programmatic scroll-to-position for the current scroll-mode canvas list using anchor-first resolution.
- Restore TOC and bookmark workflows as reader actions using anchor-aware positions.
- Restore saved scroll position after chapter content, content anchors, and page anchors are ready.

## Capabilities

### New Capabilities
- `client-reader-navigation-workflows`: TOC jumps, bookmark management, and saved-position restoration in the client reader.

### Modified Capabilities

## Impact

- Affects `bookd-ui` reader models, local progress storage, reader shell, scroll-mode content contract, `ReaderEffect.ScrollToPosition`, bookmarks state, and tests.
- Depends on backend chapter content, progress, and bookmark responses exposing stable anchor fields while keeping index-based fallbacks.
