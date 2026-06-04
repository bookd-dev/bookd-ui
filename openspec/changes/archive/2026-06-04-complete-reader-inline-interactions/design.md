## Context

`ReaderPageCanvas` already detects image bounds, URL annotations, footnote annotations, and inline footnote placeholders. `ReaderScreen` currently passes TODO callbacks, so the detection work does not produce user-visible behavior.

## Goals / Non-Goals

**Goals:**
- Convert existing canvas callbacks into visible reader interactions.
- Keep interactions non-destructive and recoverable.
- Avoid blocking rendering while images or footnote content are displayed.

**Non-Goals:**
- Persist highlights, annotations, or copied selections to backend storage.
- Add a new rich text selection engine.
- Change canvas hit-testing unless tests prove a hit-test bug.

## Decisions

- Host image preview and footnote content as reader shell overlays managed by `ReaderScreen`.
- Resolve footnote ids against the current chapter element list passed through scroll/page content.
- Treat external links as explicit user actions; use existing platform/browser helpers if present, otherwise show the URL in a safe fallback surface.
- Implement paragraph long press as a context menu for actions that can be resolved from the current paragraph position, such as adding a bookmark.

## Risks / Trade-offs

- Footnote ids can be chapter-local. Mitigation: resolve against the active chapter elements provided by the content renderer and avoid cross-chapter assumptions.
- Platform link handling differs across Android, iOS, and Desktop. Mitigation: isolate platform-specific launching behind existing expect/actual or helper patterns if available.
