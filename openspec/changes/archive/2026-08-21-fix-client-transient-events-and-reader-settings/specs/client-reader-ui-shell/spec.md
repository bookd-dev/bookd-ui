## MODIFIED Requirements

### Requirement: Reader settings are editable from the shell
The client SHALL let users open reader settings from the reader shell, update existing reader setting fields, persist each accepted value locally before returning from the update action, and synchronize the latest value to the configured server.

#### Scenario: Reader setting is changed
- **WHEN** the user changes a supported reader setting from the settings surface
- **THEN** the reader SHALL update the `ReaderSettings` state
- **AND** it SHALL persist the accepted value locally without waiting for delayed remote synchronization
- **AND** it SHALL synchronize the latest pending settings through the existing remote settings path

#### Scenario: Reader is exited immediately after a setting change
- **WHEN** the user changes a reader setting and exits the book before remote synchronization begins or completes
- **THEN** reopening the reader SHALL restore the locally persisted setting
- **AND** a stale server value SHALL NOT overwrite the pending local setting

#### Scenario: Remote settings synchronization fails
- **WHEN** the latest local reader settings cannot be synchronized because the server is unavailable or the request fails
- **THEN** the client SHALL keep the local setting active across reader and application recreation
- **AND** it SHALL retain the setting as pending for a later synchronization attempt

#### Scenario: Older synchronization completes after a newer local change
- **WHEN** a remote response for an older reader settings snapshot completes after a newer local setting has been accepted
- **THEN** the older response SHALL NOT overwrite the newer local setting

#### Scenario: Reader settings surface follows Material Design
- **WHEN** the user opens the reader settings surface
- **THEN** the reader SHALL present settings in a Material Design bottom sheet with grouped controls, consistent spacing, and clear visual hierarchy
- **AND** numeric reader settings SHALL use rounded discrete sliders with visible current value labels and tick marks
- **AND** numeric slider tick marks SHALL omit the minimum and maximum endpoint dots
- **AND** numeric slider thumbs SHALL use a minimal solid dot without a contrasting outer ring
- **AND** numeric slider changes SHALL snap to the nearest supported tick value
- **AND** numeric slider changes SHALL provide light haptic feedback when the selected tick changes on supported platforms
- **AND** page mode and boolean settings SHALL use Material components with clear selected and checked states.
