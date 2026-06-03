## ADDED Requirements

### Requirement: Reader previews tapped images
The client SHALL show an image preview when a user activates an image inside reader content.

#### Scenario: User taps a reader image
- **WHEN** the user taps an image rendered in reader content
- **THEN** the reader SHALL show a preview for that image
- **AND** it SHALL provide a way to dismiss the preview and return to reading.

### Requirement: Reader displays tapped footnotes
The client SHALL display footnote content when a user activates a footnote marker inside reader content.

#### Scenario: User taps a footnote marker
- **WHEN** the user taps a text or inline footnote marker
- **THEN** the reader SHALL resolve the footnote content for the active chapter
- **AND** it SHALL show the footnote content in a dismissible surface.

### Requirement: Reader handles external links safely
The client SHALL handle user-activated external links from reader content without crashing or silently swallowing the action.

#### Scenario: User taps an external link
- **WHEN** the user taps a URL annotation in reader content
- **THEN** the reader SHALL open the link with the supported platform path or show a fallback URL action.

### Requirement: Reader supports paragraph long-press actions
The client SHALL show available paragraph actions when the user long-presses reader text.

#### Scenario: User long-presses a paragraph
- **WHEN** the user long-presses a paragraph in reader content
- **THEN** the reader SHALL show a contextual action surface for that paragraph
- **AND** it SHALL include a bookmark action for the paragraph position.
