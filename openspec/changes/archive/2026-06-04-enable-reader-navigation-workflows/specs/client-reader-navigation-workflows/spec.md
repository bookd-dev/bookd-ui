## ADDED Requirements

### Requirement: Reader executes TOC chapter jumps
The client SHALL jump from reader TOC entries to the requested chapter in scroll mode.

#### Scenario: User selects a TOC chapter
- **WHEN** the user selects a TOC entry for a chapter
- **THEN** the reader SHALL load that chapter when needed
- **AND** it SHALL scroll to the start of that chapter after render anchors are ready.

### Requirement: Reader resolves stable content anchors
The client SHALL resolve reader jump targets by stable content anchor before falling back to paragraph or page indices.

#### Scenario: Saved target has an anchor
- **WHEN** a saved progress or bookmark target includes a content anchor id
- **THEN** the reader SHALL locate the matching element in the loaded chapter content
- **AND** it SHALL scroll to the render page containing that element after render anchors are ready.

#### Scenario: Saved target anchor is missing
- **WHEN** a saved progress or bookmark target includes an anchor id that no longer exists in the loaded chapter
- **THEN** the reader SHALL fall back to the saved paragraph or page index
- **AND** it SHALL keep the fallback target within the available render anchors.

#### Scenario: Page mode receives an anchor jump
- **WHEN** a saved progress, bookmark, or internal link target includes a content anchor id while the reader is in page mode
- **THEN** the reader SHALL locate the render page containing the matching element
- **AND** it SHALL move the pager to that page instead of only moving to the chapter start.

### Requirement: Reader resolves EPUB document links as internal jumps
The client SHALL treat EPUB HTML document links as internal reader navigation targets rather than external browser links.

#### Scenario: EPUB link targets a manifest document
- **WHEN** the user taps an EPUB link whose href references a `.html`, `.xhtml`, or `.htm` document with an optional fragment
- **THEN** the reader SHALL normalize the href relative to the current document href
- **AND** it SHALL match the target against the manifest document list by normalized href, allowing case differences and basename-only fallback
- **AND** it SHALL jump to the matched document chapter inside the reader.

#### Scenario: EPUB link includes a fragment
- **WHEN** the matched manifest document provides an anchor prefix and the tapped link includes a fragment
- **THEN** the reader SHALL sanitize the fragment with the same source-id rules used for EPUB anchors
- **AND** it SHALL jump to the prefixed anchor within the target chapter when that anchor becomes renderable.

#### Scenario: EPUB link targets the current document
- **WHEN** the user taps a fragment-only EPUB link
- **THEN** the reader SHALL resolve the fragment against the current manifest document
- **AND** it SHALL stay inside the reader instead of opening an external URL.

#### Scenario: Manifest document hrefs are unavailable
- **WHEN** the manifest lacks document href metadata
- **THEN** the reader MAY fall back to filename ordinal parsing for common legacy names such as `chapter0.xhtml`, `Chapter_2.xhtml`, `ch-01.xhtml`, `part0001.xhtml`, `section-2.xhtml`, or `0003.xhtml`
- **AND** it SHALL support both zero-based and one-based ordinals as a best-effort compatibility path.

### Requirement: Reader restores saved scroll position
The client SHALL restore the saved local reader position after the target content is ready.

#### Scenario: Reader opens with saved local progress
- **WHEN** the reader opens a book with saved local chapter and anchor-aware position
- **THEN** the reader SHALL load the saved chapter
- **AND** it SHALL scroll to the saved anchor or fallback position after the target chapter is renderable.

### Requirement: Reader supports bookmark management
The client SHALL let users create, view, jump to, and delete reader bookmarks using anchor-aware positions on the existing bookmark API paths.

#### Scenario: User adds a bookmark
- **WHEN** the user adds a bookmark at the current reader position
- **THEN** the reader SHALL call the existing add-bookmark path with chapter, anchor, fallback index, and optional note
- **AND** it SHALL show the new bookmark in the reader bookmark list.

#### Scenario: User selects a bookmark
- **WHEN** the user selects an existing bookmark
- **THEN** the reader SHALL load the bookmark chapter when needed
- **AND** it SHALL scroll to the bookmark anchor or fallback position after render anchors are ready.

#### Scenario: User deletes a bookmark
- **WHEN** the user deletes a bookmark
- **THEN** the reader SHALL call the existing delete-bookmark path
- **AND** it SHALL remove the bookmark from the reader bookmark list.

#### Scenario: Bookmark list shows context and creation time
- **WHEN** the reader renders saved bookmarks
- **THEN** a chapter bookmark SHALL show the resolved chapter name on the first line
- **AND** a paragraph bookmark SHALL show the resolved chapter name on the first line and the resolved paragraph text, note, or fallback paragraph label on the second line
- **AND** long chapter and paragraph text SHALL be constrained to one line with ellipsis
- **AND** the bookmark creation time SHALL remain visible on the first line as compact month-day and hour-minute text.

### Requirement: Programmatic reader jumps do not corrupt progress
The client SHALL distinguish programmatic reader jumps from user scrolling until the jump has completed.

#### Scenario: Programmatic jump is pending
- **WHEN** the reader is waiting for a programmatic jump target to become renderable
- **THEN** it SHALL NOT save an unrelated intermediate scroll position as the selected target.
