## MODIFIED Requirements

### Requirement: Book source header exposes direct refresh and search actions
The client SHALL expose refresh and search as direct book source header actions rather than requiring a menu interaction for search or refresh.

#### Scenario: Book source header actions are shown
- **WHEN** the user views the book source tab header
- **THEN** the client SHALL show peer refresh and search actions in the header.

#### Scenario: Search action is activated
- **WHEN** the user activates the book source header search action
- **THEN** the client SHALL navigate to the implemented App book search experience on the existing search-book route.
