# client-reader-inline-interactions Specification

## Purpose
Define client reader inline interactions for tapped images, footnotes, links, and paragraph actions.
## Requirements
### Requirement: Reader previews tapped images
The client SHALL show a full-screen image preview when a user activates an image inside reader content.

#### Scenario: User taps a reader image
- **WHEN** the user taps an image rendered in reader content
- **THEN** the reader SHALL show the image on a black full-screen background
- **AND** it SHALL center the image and initially fit the whole image within the viewport
- **AND** a single tap SHALL dismiss the preview and return to reading.

#### Scenario: User zooms a previewed reader image
- **WHEN** the image preview is visible
- **THEN** the reader SHALL support double-tap zoom toggling
- **AND** it SHALL support transform gestures for zooming and panning the image
- **AND** zoom reset SHALL restore the centered fit presentation.

### Requirement: Reader displays tapped footnotes
The client SHALL display footnote content when a user activates a footnote marker inside reader content.

#### Scenario: User taps a footnote marker
- **WHEN** the user taps a text or inline footnote marker
- **THEN** the reader SHALL resolve the footnote content for the active chapter
- **AND** it SHALL show the footnote content in a dismissible surface.

#### Scenario: User taps an inline footnote image marker
- **WHEN** the user taps an inline footnote placeholder whose key also contains image URL data
- **THEN** the reader SHALL extract the footnote id from that placeholder key
- **AND** it SHALL show the corresponding footnote content in a dismissible surface.

#### Scenario: EPUB footnote uses an inline image marker
- **WHEN** a paragraph span references a footnote id and the matching footnote provides a `footnoteImage`
- **THEN** the reader SHALL replace the raw footnote span text, such as `[1]`, with the inline footnote image marker
- **AND** each occurrence of the footnote marker SHALL produce an independent placeholder and hit target
- **AND** marker hit testing SHALL use the same absolute render bounds as drawing, including the text command vertical offset and touch padding.

### Requirement: Reader handles external links safely
The client SHALL handle user-activated external links from reader content without crashing or silently swallowing the action.

#### Scenario: User taps an external link
- **WHEN** the user taps a URL annotation in reader content
- **THEN** the reader SHALL open the link with the supported platform path or show a fallback URL action.

#### Scenario: Reader classifies non-EPUB links
- **WHEN** a tapped link has a URI scheme or uses a protocol-relative URL
- **THEN** the reader SHALL treat it as an external link even if the path ends with `.html`, `.xhtml`, or `.htm`.

### Requirement: Reader supports paragraph long-press actions
The client SHALL show available paragraph actions when the user long-presses reader text.

#### Scenario: User long-presses a paragraph
- **WHEN** the user long-presses a paragraph in reader content
- **THEN** the reader SHALL show a contextual action surface for that paragraph
- **AND** it SHALL include a bookmark action for the paragraph position.
