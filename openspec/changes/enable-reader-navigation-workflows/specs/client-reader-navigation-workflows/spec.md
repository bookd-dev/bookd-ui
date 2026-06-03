## ADDED Requirements

### Requirement: Reader executes TOC chapter jumps
The client SHALL jump from reader TOC entries to the requested chapter in scroll mode.

#### Scenario: User selects a TOC chapter
- **WHEN** the user selects a TOC entry for a chapter
- **THEN** the reader SHALL load that chapter when needed
- **AND** it SHALL scroll to the start of that chapter after render anchors are ready.

### Requirement: Reader restores saved scroll position
The client SHALL restore the saved local reader position after the target content is ready.

#### Scenario: Reader opens with saved local progress
- **WHEN** the reader opens a book with saved local chapter and paragraph position
- **THEN** the reader SHALL load the saved chapter
- **AND** it SHALL scroll to the saved position after the target chapter is renderable.

### Requirement: Reader supports bookmark management
The client SHALL let users create, view, jump to, and delete reader bookmarks using the existing bookmark API paths.

#### Scenario: User adds a bookmark
- **WHEN** the user adds a bookmark at the current reader position
- **THEN** the reader SHALL call the existing add-bookmark path
- **AND** it SHALL show the new bookmark in the reader bookmark list.

#### Scenario: User selects a bookmark
- **WHEN** the user selects an existing bookmark
- **THEN** the reader SHALL load the bookmark chapter when needed
- **AND** it SHALL scroll to the bookmark paragraph position after render anchors are ready.

#### Scenario: User deletes a bookmark
- **WHEN** the user deletes a bookmark
- **THEN** the reader SHALL call the existing delete-bookmark path
- **AND** it SHALL remove the bookmark from the reader bookmark list.

### Requirement: Programmatic reader jumps do not corrupt progress
The client SHALL distinguish programmatic reader jumps from user scrolling until the jump has completed.

#### Scenario: Programmatic jump is pending
- **WHEN** the reader is waiting for a programmatic jump target to become renderable
- **THEN** it SHALL NOT save an unrelated intermediate scroll position as the selected target.
