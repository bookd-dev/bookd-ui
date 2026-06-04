## 1. Callback Completion

- [x] 1.1 Replace reader image, footnote, link, and paragraph long-press TODO callbacks with real state dispatch.
- [x] 1.2 Ensure callbacks carry enough chapter and paragraph context for overlay actions.
- [x] 1.3 Add missing canvas long-press plumbing if paragraph long-press is not currently emitted.

## 2. Interaction UI

- [x] 2.1 Add dismissible image preview overlay.
- [x] 2.2 Add dismissible footnote overlay resolved from active chapter elements.
- [x] 2.3 Add external-link handling using existing platform helper patterns or a safe fallback.
- [x] 2.4 Add paragraph context menu with bookmark action.

## 3. Resources And Tests

- [x] 3.1 Add all user-facing interaction copy to Compose string resources.
- [x] 3.2 Add tests for footnote resolution, link fallback behavior, and paragraph bookmark action dispatch.
- [x] 3.3 Add hit-test regression tests if canvas event plumbing changes.
- [x] 3.4 Run `cd bookd-ui && ./gradlew :shared:jvmTest` and the current compile task.
