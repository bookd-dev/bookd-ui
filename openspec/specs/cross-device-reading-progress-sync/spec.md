# cross-device-reading-progress-sync Specification

## Purpose
定义跨设备阅读位置从本地采集、持久化到云端传播的时效、一致性与冲突规则，使用户能够在另一设备继续到最近阅读的章节和段落。

## Requirements

### Requirement: Reader persists the latest normalized position locally
The client SHALL persist a normalized reading-position snapshot containing chapter index, stable anchor when available, fallback element index, anchor-relative offset, page fallback, aggregate progress, and update time.

#### Scenario: Visible reading position changes
- **WHEN** the reader reports a new user-driven visible position
- **THEN** the client SHALL update the in-memory position immediately
- **AND** it SHALL persist the latest normalized snapshot locally without waiting for an aggregate-progress threshold.

### Requirement: Reader synchronizes settled progress promptly
The client SHALL upload the latest local snapshot after a short inactivity debounce and SHALL NOT require a minimum aggregate-progress difference.

#### Scenario: User pauses after scrolling or paging
- **WHEN** no newer user-driven position arrives for at most one second
- **THEN** the client SHALL start uploading the latest snapshot when network configuration is available.

#### Scenario: Position changes during an upload
- **WHEN** a newer position is produced while a progress upload is active
- **THEN** the client SHALL keep only the latest pending snapshot
- **AND** it SHALL upload that snapshot after the active request completes.

#### Scenario: Chapter boundary is crossed
- **WHEN** the active reading chapter changes because of user scrolling or paging
- **THEN** the client SHALL request an immediate upload of the new chapter position.

### Requirement: Reader flushes progress at explicit boundaries
The client SHALL attempt to flush the latest position when the user explicitly saves progress or exits through the reader navigation action.

#### Scenario: User exits the reader
- **WHEN** the user activates the reader back action
- **THEN** the client SHALL persist the latest snapshot locally
- **AND** it SHALL attempt the cloud upload before emitting reader navigation.

### Requirement: Reader detects precise-position conflicts
The client SHALL compare chapter, anchor, fallback element, anchor-relative offset, and page fallback when determining whether local and remote reading positions conflict.

#### Scenario: Local and remote positions differ within one chapter
- **WHEN** local and remote progress refer to the same chapter but different anchors, fallback elements, offsets, or pages
- **THEN** the client SHALL present the existing progress conflict choice
- **AND** it SHALL NOT silently select one position.

#### Scenario: Local and remote positions are equivalent
- **WHEN** their normalized position coordinates are equivalent even if aggregate percentages or timestamps differ
- **THEN** the client SHALL continue without presenting a conflict.
