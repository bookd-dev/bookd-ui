## ADDED Requirements

### Requirement: Book source header exposes direct refresh and search actions
The client SHALL expose refresh and search as direct book source header actions rather than requiring a menu interaction for search or refresh.

#### Scenario: Book source header actions are shown
- **WHEN** the user views the book source tab header
- **THEN** the client SHALL show peer refresh and search actions in the header.

#### Scenario: Search action is activated
- **WHEN** the user activates the book source header search action
- **THEN** the client SHALL navigate to the existing search-book route.

### Requirement: Refresh action updates all book source local data
The client SHALL refresh the source list and locally cached first-page book data for every returned source when the book source header refresh action is activated.

#### Scenario: All-source refresh succeeds
- **WHEN** the user activates the book source header refresh action with a configured backend
- **THEN** the client SHALL force-refresh the app source list from the backend
- **AND** it SHALL force-refresh the first page of books for each returned source
- **AND** it SHALL replace stale local source and per-source book cache entries through the existing cache repositories.

#### Scenario: Refresh is already active
- **WHEN** a source or book load operation is already active
- **THEN** the client SHALL prevent starting another all-source refresh operation.
