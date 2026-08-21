## ADDED Requirements

### Requirement: Search route provides a usable App search screen
The Compose client SHALL implement the existing search-book route as a searchable book discovery screen.

#### Scenario: Search screen is opened
- **WHEN** the user navigates to the search-book route
- **THEN** the client SHALL show a search input focused on entering a book keyword
- **AND** it SHALL show an initial empty state before a non-blank query is submitted.

#### Scenario: Blank query is ignored
- **WHEN** the user submits an empty or whitespace-only query
- **THEN** the client SHALL clear current search results and SHALL NOT call the backend search API.

#### Scenario: Search results are loaded
- **WHEN** the user submits a non-blank query
- **THEN** the client SHALL request the first page of App book search results using the trimmed query
- **AND** it SHALL show a loading state until results or an error are available.

### Requirement: Search results support paging and navigation
The Compose client SHALL present search results as selectable book rows or cards with incremental paging.

#### Scenario: Matching books are displayed
- **WHEN** the backend returns matching books for a submitted query
- **THEN** the client SHALL show each result with cover, title, author when present, format, and chapter count when present.

#### Scenario: Result opens detail
- **WHEN** the user selects a search result
- **THEN** the client SHALL navigate to the existing book detail route for that book id.

#### Scenario: More results are available
- **WHEN** the displayed search response has `hasMore` equal to `true` and the user reaches the end of the list
- **THEN** the client SHALL request the next page using the same trimmed query and append the returned books.

#### Scenario: Query changes reset paging
- **WHEN** the user submits a different non-blank query
- **THEN** the client SHALL discard prior result pages and restart from offset `0`.

### Requirement: Search state handles empty, error, and cache fallback behavior
The Compose client SHALL make search result state explicit and recover through existing error handling paths.

#### Scenario: No results are returned
- **WHEN** the backend returns an empty result set for a submitted query
- **THEN** the client SHALL show a search-specific empty state for that query.

#### Scenario: Search fails without cache fallback
- **WHEN** the search request fails and no matching cached rows are available
- **THEN** the client SHALL surface the failure through the existing global exception path or visible search error state.

#### Scenario: Search falls back to local cache
- **WHEN** the search request fails after matching books have previously been cached locally
- **THEN** the client SHALL return matching cached rows for the submitted query while preserving the submitted query in state.

#### Scenario: Returned books update local cache
- **WHEN** the backend returns search results
- **THEN** the client SHALL upsert the returned book rows into the existing SQLDelight `BookEntity` cache.
