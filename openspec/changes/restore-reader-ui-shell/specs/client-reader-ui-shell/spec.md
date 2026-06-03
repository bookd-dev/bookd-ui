## ADDED Requirements

### Requirement: Reader screen exposes operational chrome
The client SHALL provide reader chrome for navigation, current reading context, and reader actions while preserving the current canvas scroll renderer.

#### Scenario: Reader content is loaded
- **WHEN** the reader has loaded the current chapter content
- **THEN** the reader SHALL render the content area
- **AND** it SHALL make reader chrome actions available without replacing the rendered content.

#### Scenario: Reader status metadata is visible
- **WHEN** the reader has loaded the current chapter content
- **THEN** the reader SHALL show compact reading progress at the bottom-left of the reader viewport
- **AND** it SHALL show the current device time at the bottom-right of the reader viewport
- **AND** both status values SHALL remain visible when the reader menu shell is hidden
- **AND** the rendered book content SHALL reserve the status metadata area instead of appearing beneath or overlapping it.

#### Scenario: Reader menu is toggled
- **WHEN** the user taps the configured menu activation area
- **THEN** the reader SHALL show or hide the reader menu shell.

### Requirement: Reader screen communicates loading and failure states
The client SHALL show explicit reader loading and failure states instead of leaving the screen blank.

#### Scenario: Book or chapter is loading
- **WHEN** the reader is loading the book manifest or chapter content
- **THEN** the reader SHALL display a loading state.

#### Scenario: Reader load fails
- **WHEN** the reader cannot load the book or current chapter
- **THEN** the reader SHALL display an error state with a retry or back navigation action.

### Requirement: Reader settings are editable from the shell
The client SHALL let users open reader settings from the reader shell and update existing reader setting fields.

#### Scenario: Reader setting is changed
- **WHEN** the user changes a supported reader setting from the settings surface
- **THEN** the reader SHALL update the `ReaderSettings` state
- **AND** it SHALL persist the setting through the existing settings update path.

#### Scenario: Reader settings surface follows Material Design
- **WHEN** the user opens the reader settings surface
- **THEN** the reader SHALL present settings in a Material Design bottom sheet with grouped controls, consistent spacing, and clear visual hierarchy
- **AND** numeric reader settings SHALL use rounded discrete sliders with visible current value labels and tick marks
- **AND** numeric slider tick marks SHALL omit the minimum and maximum endpoint dots
- **AND** numeric slider thumbs SHALL use a minimal solid dot without a contrasting outer ring
- **AND** numeric slider changes SHALL snap to the nearest supported tick value
- **AND** numeric slider changes SHALL provide light haptic feedback when the selected tick changes on supported platforms
- **AND** page mode and boolean settings SHALL use Material components with clear selected and checked states.

### Requirement: Progress conflicts require explicit user choice
The client SHALL require explicit user choice when local and remote progress conflict.

#### Scenario: Progress conflict is detected
- **WHEN** the reader detects both local and remote progress with different chapter positions
- **THEN** the reader SHALL show a progress conflict surface
- **AND** it SHALL NOT automatically choose local or remote progress before user action.

#### Scenario: User chooses a progress source
- **WHEN** the user chooses local or remote progress
- **THEN** the reader SHALL load the selected progress source
- **AND** it SHALL dismiss the progress conflict surface.
