## ADDED Requirements

### Requirement: Reader supports canvas page mode
The client SHALL render a horizontal page-mode reader when reader settings select page mode.

#### Scenario: Page mode is selected
- **WHEN** `ReaderSettings.pageMode` is `PAGE`
- **THEN** the reader SHALL render page-mode content instead of scroll-mode content.

#### Scenario: Scroll mode is selected
- **WHEN** `ReaderSettings.pageMode` is `SCROLL`
- **THEN** the reader SHALL continue rendering the existing scroll-mode content.

### Requirement: Page mode supports cross-chapter navigation
The client SHALL let users move across chapter boundaries in page mode.

#### Scenario: User advances past the final page of a chapter
- **WHEN** the user advances past the final page of the current chapter
- **THEN** the reader SHALL load or reveal the next chapter when one exists
- **AND** it SHALL display the first page of that chapter.

#### Scenario: User retreats before the first page of a chapter
- **WHEN** the user retreats before the first page of the current chapter
- **THEN** the reader SHALL load or reveal the previous chapter when one exists
- **AND** it SHALL display the last page of that chapter.

### Requirement: Page mode preserves title-only chapters
The client SHALL keep chapters with only a title reachable in page mode.

#### Scenario: Title-only chapter is in the reading flow
- **WHEN** a chapter has a title and no body elements
- **THEN** page mode SHALL create a reachable page for that chapter
- **AND** users SHALL be able to continue to adjacent chapters.

### Requirement: Page mode saves page progress
The client SHALL update local and remote page progress from page-mode navigation.

#### Scenario: Page changes in page mode
- **WHEN** the visible page changes in page mode
- **THEN** the reader SHALL update the current page index
- **AND** it SHALL schedule progress persistence through the existing progress path.
