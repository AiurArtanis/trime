# Astra 15：完整上游迁移

2026-11-09。用户明确要求以最新上游为基础，保留上游功能，再增加 Astra 个性化扩展。本记录取代 astra-13/14 的选择性整合策略。

## 基线与兼容方案

- 上游基线 `a6307961`；`src` 与该提交一致。个人主线仍为 `develop`。
- 旧主线 `b03799b1` 保存在 `archive/astra14-before-upstream-migration`。迁移分支从真实上游创建，最终通过合并保留上游与个人历史，不使用 `ours` 伪装同步。
- 相对早期纯上游基线 `15487288`，包含 97 个上游提交；相对本轮开始的 src `102b9cd2` 新增 1 个提交（首次解锁前显示键盘）。此前未整合的重构在本轮一并纳入。

| 模块 | 保留的上游实现 | Astra 兼容方式 |
| --- | --- | --- |
| 主题 | kaml/serialization、源文件 DSL、诊断、主题级动作及字体缓存、配色 map | 独立 Astra YAML；旧动作适配新 token；候选字号扩展字段；目录按 ID 去重；标准主题资源与上游一致 |
| 明暗配色 | 上游 light_scheme/dark_scheme 解析与任意自选配色 | Solar/Luna 双向链接；手动切换临时覆盖，下一次真实系统明暗变化恢复跟随；尊重主动关闭跟随 |
| 文件同步 | 新 core.data.UserDataManager、StorageAccess、双向 manifest | 首次升级备份两端，以现用本地文件建立基线，不传播旧索引删除；随后恢复上游同步规则 |
| 剪贴板与收藏 | 新 clip.db、独立实体及 DAO、上游旧表读取迁移 | 迁移前备份旧数据库及 WAL/SHM；成功标记避免已迁移旧文件重放；原始数据仍可恢复 |
| Direct Boot | 首次解锁前键盘、DE/CE 隔离、WorkManager 延迟初始化、存储就绪重试 | 常用记录遵守对应存储上下文；不复制私密剪贴板/常用历史到解锁前区域；配置替换要求解锁 |
| Rime 生命周期 | 上游启动与会话框架、最新 librime | 输入/API 与部署互斥；原生维护完成后才恢复；安全同步先停库；导入事务失败回滚 |
| 键盘与工具 | 全局双击 Shift、中文模式重置 Shift、新回车与弹窗行为 | 保留 Astra 阴阳/英文图标、语音长按空格、符号页常用优先、组合 emoji/符号频次与标点过滤 |
| 构建 | Gradle 9.8.2、SDK 37、NDK 30、CMake 4.1.2、Boost 1.92 和上游依赖 | 仅 ARM64；沿用包名、显示名及 debug 签名 |

原版配置导入与雾凇更新仍使用事务备份和回滚；SnakeYAML 仅服务旧配置导入工具，不替代上游主题加载器。保留维护诊断，避免在诊断导出中记录输入正文。

## 现有数据与恢复

- 包名保持 `com.osfans.trime.debug.fresh`，应用名保持 `同文输入法(Astra)`；覆盖安装保留应用数据。不要先卸载旧版。
- 解锁后的首次旧版升级，在内部 `files/astra-upgrade-15/private` 保存旧 `clipboard.db`、`collection.db`、WAL/SHM、`symbol_history` 和设置快照。完整快照带 `.complete` 标记；复制校验失败不发布完成标记，也不删除源数据。
- 首次外部目录同步在应用外部文件目录 `backups/astra15-<目录标识>` 保存 `local` 与 `external`。冲突文件优先现有本地版本，外部原版留在快照；外部独有文件正常导入；文件/目录类型冲突停止同步并保留备份。
- 旧剪贴板的正文、置顶、时间和类型进入上游新结构；旧 HTML 等原始数据库内容保存在快照。常用记录原频次及排序延续，旧 `@ / % #` 等记录加载时过滤。
- 备份只创建一次，后续重试不覆盖首次快照。新版数据库不可假定能被旧 APK 直接读取；回退需使用备份恢复旧数据，不应直接降级覆盖。

## 验证与交付

使用仓库 wrapper，在 `E:/trime-build-env/trime-env.ps1` 环境下执行 `spotlessApply :app:assembleDebug :app:testDebugUnitTest :app:lintDebug spotlessCheck`，`BUILD_ABI=arm64-v8a`。

测试覆盖上游主题解析/诊断、Astra 布局和日夜状态、符号历史、配置事务回滚、快照完整性及维护互斥。最终结果见交接顶部；日志保留在 `E:/trime-build-env/astra-15-merged-validation.log`。本地构建和单元测试不能代替三星实机上的覆盖安装、SAF 授权和真实旧数据库迁移验收。

NAS 交付名 `trime-astra-15-arm64-v8a-debug.apk` 及 `.sha256`。每周自动化 `astra` 已改为本策略，每周六北京时间 10:00 执行；构建结束使用仓库 wrapper 正常停止空闲守护进程，保留缓存与产物。
