## 1. Spec

- [x] 1.1 Add `client-booksource-refresh-actions` capability delta.
- [x] 1.2 Document direct header actions and all-source refresh behavior.

## 2. Implementation

- [x] 2.1 Replace the book source header menu with direct refresh and search buttons.
- [x] 2.2 Route search to the existing search-book destination.
- [x] 2.3 Update `RefreshAll` to force-refresh the source list and every returned source's first book page.
- [x] 2.4 Remove the unused book source menu enum.

## 3. Verification

- [x] 3.1 Add a JVM unit test for multi-source refresh-all behavior.
- [x] 3.2 Run the targeted client JVM test.
