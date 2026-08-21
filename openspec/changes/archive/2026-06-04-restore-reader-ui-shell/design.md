## Context

`ReaderScreen` currently delegates to `ReaderContent` only when the first chapter is loaded. Menu, settings, TOC, image preview, footnote, and progress-conflict components were removed during the canvas refactor, leaving multiple TODO callbacks and no visible recovery path for loading or error states.

## Goals / Non-Goals

**Goals:**
- Restore reader chrome without reintroducing the old monolithic `ReaderContent`.
- Keep `ReaderContent` focused on content rendering and let `ReaderScreen` coordinate shell state.
- Make progress conflicts explicit and testable.
- Keep all user-facing text in Compose resources.

**Non-Goals:**
- Implement actual TOC/bookmark scrolling, page mode, image preview, footnote dialogs, external links, or paragraph actions. Those are covered by later changes.
- Change reader data models, backend contracts, or `ReaderEngine`.

## Decisions

- Use small shell components instead of reviving the deleted large components wholesale, so the UI can wrap the canvas renderer without coupling to old scroll/page assumptions.
- Keep overlay state local to `ReaderScreen` unless a state affects persistence or navigation; settings updates still flow through `ReaderViewModel.updateSettings`.
- Show progress-conflict choices from state and call `useLocalProgress`, `useRemoteProgress`, or `dismissProgressConflict` only after user action.
- Add UI tests or focused state/component tests for shell behavior where feasible; do not require device-only validation for basic visibility.

## Risks / Trade-offs

- Restoring UI shell first means some menu actions may be visible before their final workflows are implemented. Mitigation: disable or route unfinished actions to no-op placeholders only when the corresponding later change has not landed.
- Overlay state in `ReaderScreen` can grow. Mitigation: keep state names explicit and extract only repeated UI into component files.
