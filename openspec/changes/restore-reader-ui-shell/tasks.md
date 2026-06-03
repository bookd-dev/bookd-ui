## 1. Reader Shell State

- [x] 1.1 Inspect the deleted reader shell components and current `ReaderScreen` TODO callbacks.
- [x] 1.2 Add local shell state for menu, settings, TOC entry surface, loading, error, and progress-conflict visibility.
- [x] 1.3 Ensure `ReaderScreen` does not call `useLocalProgress()` automatically when `hasProgressConflict` is true.

## 2. Reader Shell UI

- [x] 2.1 Add compact top/status/menu UI around `ReaderContent` using existing theme conventions.
- [x] 2.2 Add reader settings surface for existing `ReaderSettings` fields and wire it to `ReaderViewModel.updateSettings` helpers.
- [x] 2.3 Add loading and error surfaces with retry/back actions.
- [x] 2.4 Add progress-conflict UI with local, remote, and dismiss actions.
- [x] 2.5 Simplify the TOC surface to chapter summary, reversible sort, compact chapter metadata, current-chapter highlight, and floating current-chapter locator.

## 3. Resources And Tests

- [x] 3.1 Add all new user-facing copy to Compose string resources in default and Chinese resource sets.
- [x] 3.2 Add focused tests for progress-conflict choice behavior and settings update wiring where feasible.
- [x] 3.3 Add focused tests for TOC display ordering and current progress metadata.
- [x] 3.4 Run `cd bookd-ui && ./gradlew :shared:jvmTest` or the current equivalent test task.
- [x] 3.5 Run `cd bookd-ui && ./gradlew :shared:compileKotlinJvm` or the current equivalent compile task.
