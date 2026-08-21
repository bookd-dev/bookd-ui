## Why

客户端把一次性全局错误保存在可重放状态中，导致横竖屏切换或页面重建时重复显示已经处理过的“未配置网络地址”提示。同时，阅读器设置仅通过延迟的远端请求保存，用户快速退出或网络同步失败时会丢失刚修改的字号等设置。

## What Changes

- 将全局错误提示建模为有缓冲、消费后不重放的一次性事件，避免页面重建重复显示旧提示。
- 将阅读器设置改为本地同步持久化、远端防抖同步；待同步本地值优先于服务端旧值。
- 在远端同步成功时仅确认与当前本地快照一致的设置，避免较旧响应覆盖更新值。
- 增加一次性错误事件与阅读器设置快速退出恢复的单元测试。

## Capabilities

### New Capabilities

- `client-transient-feedback-lifecycle`: 定义客户端一次性错误提示在页面或配置重建过程中的消费和非重放语义。

### Modified Capabilities

- `client-reader-ui-shell`: 强化阅读器设置的本地持久化、快速退出恢复和远端同步失败语义。

## Impact

- 客户端共享层：`AppViewModel`、`SnackbarHostScaffold`、`AppScreen`。
- 阅读器数据与状态层：`ReaderViewModel`、`ReaderRepository`、Koin 仓库注册。
- 客户端 JVM 单元测试；Android、iOS 共享代码编译链路。
- 不改变后端 API、数据库结构或对外请求格式。
