## Why

The Compose client currently renders main app content too close to the device status and navigation bars, and dark mode leaves several text surfaces with black or low-contrast gray styling. This makes the bookshelf and other primary screens visually clipped or unreadable on mobile devices.

## What Changes

- Add a global client app shell contract for system status bar and navigation bar safe-area handling.
- Ensure primary screens reserve system insets so top content, bottom tabs, and scrollable content are not hidden by cutouts, status bars, navigation bars, or home indicators.
- Normalize dark-theme text and icon colors so user-visible content remains readable against dark backgrounds.
- Avoid black-versus-dark-gray visual distinctions in normal dark-mode content surfaces, except for the bottom tab selected/unselected state where a muted distinction is intentional.
- Add focused client tests or preview validation for shell inset handling and dark-theme contrast behavior where feasible.

## Capabilities

### New Capabilities

- `client-app-shell-safe-areas-theme`: Defines global Compose client shell requirements for safe-area layout, system bars, bottom navigation, and light/dark theme readability.

### Modified Capabilities

None.

## Impact

- Affects `bookd-ui` shared Compose app shell, bottom navigation, theme tokens, and primary screen containers such as bookshelf, book sources, and settings.
- May affect Android/iOS/Desktop composition where platform safe-area behavior differs.
- No backend API, database schema, route, or deployment contract changes are planned.
