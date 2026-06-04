# client-app-shell-safe-areas-theme Specification

## Purpose
TBD - created by archiving change fix-client-shell-safe-areas-and-dark-theme. Update Purpose after archive.
## Requirements
### Requirement: Client app shell respects system safe areas
The Compose client SHALL reserve platform system safe areas for top-level app content and bottom navigation.

#### Scenario: Top-level screen is shown on a mobile device
- **WHEN** the user opens a top-level client screen such as bookshelf, book sources, or settings on a device with a status bar or display cutout
- **THEN** the first visible app content SHALL render below the unsafe status-bar or cutout area
- **AND** top controls SHALL remain tappable and fully visible.

#### Scenario: Bottom navigation is shown on a mobile device
- **WHEN** the top-level bottom navigation is visible on a device with a navigation bar or home indicator
- **THEN** the bottom navigation SHALL render above the unsafe navigation area
- **AND** the selected tab label and icon SHALL remain visible and tappable.

#### Scenario: Scrollable content reaches the bottom
- **WHEN** a top-level screen has scrollable or paged content behind the bottom navigation
- **THEN** the final content item SHALL be reachable without being obscured by the bottom navigation, navigation bar, or home indicator.

### Requirement: Client system bars follow the active theme
The Compose client SHALL make platform system bar appearance compatible with the active light or dark theme when the platform exposes system bars.

#### Scenario: Light theme is active
- **WHEN** the client renders in light theme on a platform with status or navigation bars
- **THEN** system bar backgrounds and icon contrast SHALL remain compatible with the light app shell.

#### Scenario: Dark theme is active
- **WHEN** the client renders in dark theme on a platform with status or navigation bars
- **THEN** system bar backgrounds and icon contrast SHALL remain compatible with the dark app shell.

### Requirement: Dark theme content remains readable
The Compose client SHALL use readable dark-theme content colors for all normal user-visible text and icons.

#### Scenario: Dark theme top-level content is rendered
- **WHEN** bookshelf, book sources, settings, detail, dialogs, menus, empty states, loading states, or error states render in dark theme
- **THEN** normal user-visible text and icons SHALL use Material theme content roles with sufficient contrast against their container
- **AND** they SHALL NOT use black or near-black colors against dark containers.

#### Scenario: Secondary metadata is rendered in dark theme
- **WHEN** secondary labels, progress text, author text, helper text, or metadata render in dark theme
- **THEN** the text SHALL remain readable against its background
- **AND** muted styling SHALL NOT make the text disappear or depend on a black-versus-dark-gray distinction.

#### Scenario: Reader book content is rendered in dark theme
- **WHEN** the reader renders book content in scroll mode or page mode while dark theme is active
- **THEN** paragraph text, headings, footnotes, image alternate text, quote text, and code text SHALL use a readable foreground color against the reader background
- **AND** the canvas renderer SHALL NOT fall back to black text for normal book content.

### Requirement: Dark theme avoids black-gray hierarchy outside bottom tabs
The Compose client SHALL NOT use black-versus-dark-gray differences as the primary visual hierarchy for normal dark-mode content, except in the bottom tab selected/unselected state.

#### Scenario: Normal content surface is rendered in dark theme
- **WHEN** a normal screen, list item, header, dialog, menu, card, action row, or empty/error surface renders in dark theme
- **THEN** hierarchy SHALL be expressed through readable theme roles, typography, spacing, icon weight, or semantic colors
- **AND** black and dark-gray differences SHALL NOT be required to identify readable text, controls, or state.

#### Scenario: Bottom tab state is rendered in dark theme
- **WHEN** the top-level bottom tab renders selected and unselected items in dark theme
- **THEN** it MAY use a muted gray distinction between selected and unselected tab labels or icons
- **AND** both selected and unselected tab labels and icons SHALL remain visible and tappable.

