## 1. Background Selection

- [x] 1.1 Add shared cover-path selection logic that accepts real cover images and excludes missing, blank, and backend-generated text covers.
- [x] 1.2 Add JVM regression tests for real image URLs, generated text-cover URLs with query parameters, and missing cover values.

## 2. Book Detail Rendering

- [x] 2.1 Add a shared Compose background layer that crops, enlarges, blurs, and fades the eligible cover image.
- [x] 2.2 Blend the optional image with the active theme background and allow the detail scaffold and top app bar to reveal it without changing existing content behavior.
- [x] 2.3 Preserve the existing opaque theme background when no eligible image background exists.

## 3. Verification

- [x] 3.1 Run the complete shared JVM test suite.
- [x] 3.2 Compile the shared Android main target and iOS simulator target.
- [x] 3.3 Check the scoped source and test diff for whitespace errors and unrelated changes.
