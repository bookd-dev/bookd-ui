## Purpose

定义客户端短暂反馈事件在页面订阅者销毁与重建过程中的交付边界，确保未处理事件不会丢失、已处理事件不会因配置变化重复显示。

## ADDED Requirements

### Requirement: Transient error feedback is delivered at most once
The client SHALL model transient error feedback as consumable events rather than durable replayable screen state.

#### Scenario: Error occurs before the feedback host starts collecting
- **WHEN** a transient error occurs while no feedback host is actively collecting events
- **THEN** the client SHALL retain the unconsumed event until a feedback host can display it

#### Scenario: Feedback host is rebuilt after displaying an error
- **WHEN** a transient error has already been consumed and displayed
- **AND** the page or application composition is rebuilt by a configuration change
- **THEN** the client SHALL NOT display the consumed error again

#### Scenario: A new error occurs after rebuild
- **WHEN** a distinct transient error occurs after the feedback host is rebuilt
- **THEN** the client SHALL display the new error once
