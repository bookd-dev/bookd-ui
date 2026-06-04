## Why

The current reader refactor removed most reader chrome and now renders content only through the scroll-mode canvas path. Users need visible reader controls, settings, loading/error feedback, and progress-conflict handling before deeper navigation and interaction features can be rebuilt safely.

## What Changes

- Restore a lightweight reader UI shell around the existing canvas scroll renderer.
- Reintroduce top/status/menu/settings/TOC surfaces with Compose Multiplatform components and localized strings.
- Replace the current automatic progress-conflict resolution with an explicit user choice.
- Preserve the current `ReaderEngine`, `ScrollModeContent`, page-anchor cache, and progress-saving contracts.

## Capabilities

### New Capabilities
- `client-reader-ui-shell`: Reader chrome, settings, menu, loading/error, and progress-conflict user surfaces.

### Modified Capabilities

## Impact

- Affects `bookd-ui` reader screens, reader ViewModel effect handling, Compose resources, and focused UI/unit tests.
- No backend API, database schema, or reader engine changes are planned.
