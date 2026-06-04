## 1. Shell Insets

- [x] 1.1 Audit `SnackbarHostScaffold`, `AppScreen`, `AppMainScreen`, and nested top-level screens for current status-bar, navigation-bar, scaffold padding, and inset consumption behavior.
- [x] 1.2 Update the shared top-level app shell so bookshelf, book sources, and settings content renders below status bars and display cutouts without duplicate top padding.
- [x] 1.3 Update bottom navigation inset handling so tab icons and labels render above navigation bars and home indicators.
- [x] 1.4 Ensure top-level scrollable or paged content reserves enough bottom space for the bottom navigation and platform navigation safe area.
- [x] 1.5 Align platform system bar background and icon contrast with the active light or dark theme where platform hooks are available.

## 2. Dark Theme Readability

- [x] 2.1 Audit dark-mode text/icon colors for primary app screens, dialogs, menus, list items, empty states, loading states, and error states.
- [x] 2.2 Update `Color.kt` / `Theme.kt` dark color tokens so normal text and icons remain readable against their dark containers.
- [x] 2.3 Replace component-local black, near-black, or low-contrast gray color usage with Material theme content roles where it affects user-visible text or icons.
- [x] 2.4 Keep the bottom tab selected/unselected muted distinction, while ensuring both states remain visible and tappable.
- [x] 2.5 Verify normal dark-mode content no longer relies on black-versus-dark-gray differences for readable hierarchy outside the bottom tab.
- [x] 2.6 Inject active theme colors into the reader renderer so scroll and page mode book text remains visible in dark mode.

## 3. Verification

- [x] 3.1 Add focused JVM/common tests for dark-theme token contrast or color-role mapping where feasible.
- [x] 3.2 Add focused tests or previews for app shell inset behavior covering top content and bottom tab visibility where feasible.
- [x] 3.3 Run `cd bookd-ui && ./gradlew :shared:jvmTest`.
- [x] 3.4 Run `cd bookd-ui && ./gradlew :shared:compileKotlinJvm`.
- [x] 3.5 Run `openspec validate fix-client-shell-safe-areas-and-dark-theme --strict` in both the root and `bookd-ui` OpenSpec roots.
