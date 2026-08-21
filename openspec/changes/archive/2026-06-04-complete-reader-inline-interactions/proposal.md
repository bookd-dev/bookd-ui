## Why

The canvas reader can detect images, links, footnotes, and text positions, but `ReaderScreen` currently leaves those callbacks as TODOs. Users need those inline interactions completed after the reader shell and navigation workflows are restored.

## What Changes

- Add image preview from canvas image taps.
- Add footnote display from text and inline footnote taps.
- Handle external links safely from reader content.
- Add paragraph long-press actions for the current reader position.

## Capabilities

### New Capabilities
- `client-reader-inline-interactions`: Image, footnote, link, and paragraph long-press interactions inside the client reader.

### Modified Capabilities

## Impact

- Affects reader canvas callbacks, reader shell overlays, platform link handling, resources, and focused tests.
- No backend API changes are planned unless later highlight/annotation storage is explicitly introduced in a separate change.
