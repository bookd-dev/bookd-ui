## Why

The Compose book detail screen currently uses the same flat theme background for every book, even when a real cover image is available. Reusing a softened cover image can strengthen the visual relationship to the selected book while preserving the existing, readable appearance for generated text covers and books without covers.

## What Changes

- Render a real book cover as a full-screen, cropped background on the Compose book detail page.
- Soften the cover background through blur, reduced opacity, and an active-theme color overlay so existing text and controls remain readable.
- Keep the existing theme background unchanged when the cover is absent or is the backend-generated text cover identified by the established `book_<id>_generated.*` filename contract.
- Add focused regression coverage for real-image, generated-text, and missing-cover selection behavior.

## Capabilities

### New Capabilities

- `client-book-detail-cover-background`: Defines cover-aware book detail backgrounds, text-cover fallback behavior, and readability expectations for the Compose client.

### Modified Capabilities

None.

## Impact

- Affected client code: `shared` book-detail Compose screen and background rendering component.
- Affected tests: `shared` JVM tests for background-cover selection.
- No client data model, API contract, navigation, dependency, or reader behavior changes.
