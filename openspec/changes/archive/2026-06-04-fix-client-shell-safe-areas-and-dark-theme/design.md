## Context

The Compose client uses a shared `NavigationSuiteScaffold` for bookshelf, book sources, and settings. On mobile devices the current shell can place top content near the status bar or display cutout and can place the bottom navigation close to the system navigation bar or home indicator. The app also uses a minimal black/white/gray theme, but dark mode currently allows normal text and icon surfaces to rely on black or low-contrast gray distinctions, which makes content hard to read.

## Goals / Non-Goals

**Goals:**
- Centralize system status bar, navigation bar, cutout, and home-indicator safe-area handling in the shared client shell.
- Keep top-level screens visually stable across Android, iOS, and Desktop without adding per-screen padding hacks.
- Make dark-mode user-visible text and icons readable by default through theme tokens and Material color roles.
- Make reader-rendered book content readable in dark mode by injecting active theme foreground/background colors into the reader renderer.
- Preserve a muted selected/unselected distinction for the bottom tab only.
- Add focused regression coverage for inset and dark-theme behavior where the current test stack can cover it.

**Non-Goals:**
- Redesign the bookshelf, book source, settings, detail, or reader workflows.
- Add new user-configurable reader background, font color, or theme settings.
- Change backend APIs, persisted data, routing contracts, or resources unrelated to shell/theme behavior.

## Decisions

- Apply safe-area layout at the app shell boundary, primarily around `AppMainScreen` and the shared scaffold. This keeps bookshelf, book source, and settings content from each inventing independent inset rules.
- Use Compose `WindowInsets` and Material scaffold padding rather than hardcoded top or bottom dp offsets. Device status bars, cutouts, navigation bars, gesture areas, and Desktop windows differ enough that fixed spacing would drift.
- Treat bottom navigation as the only dark-mode surface that may intentionally use a muted gray selected/unselected distinction. Normal content, dialogs, headers, menus, cards, lists, and empty/error states should use readable Material content roles such as `onBackground`, `onSurface`, `onSurfaceVariant`, `primary`, and `error`.
- Prefer theme-token fixes in `Color.kt` / `Theme.kt` before local component overrides. Component-level fixes are still appropriate where an existing component hardcodes black, dark gray, transparent, or an unsuitable color role.
- Pass active theme colors into the reader renderer instead of relying on Compose text defaults inside canvas-measured text. Reader paragraph, heading, footnote, alt, quote, and code text should inherit a readable foreground color for the current app theme.
- Keep platform system bar appearance aligned with the active theme when platform hooks are already available or can be added locally without introducing a new dependency.

## Risks / Trade-offs

- [Risk] Insets can be consumed twice when nested scaffolds already apply their own padding. -> Mitigation: audit `SnackbarHostScaffold`, `AppMainScreen`, and nested screen scaffolds together and verify top and bottom content does not gain duplicate spacing.
- [Risk] Changing dark theme tokens may visually affect many screens. -> Mitigation: keep changes semantic and focused on readability, then cover the critical app shell and representative screen components with tests or previews.
- [Risk] Desktop does not need mobile system bars. -> Mitigation: use Compose insets APIs that resolve to platform-appropriate values instead of adding platform-specific fixed padding.
