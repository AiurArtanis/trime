# Astra 5：Solar / Luna 配色

日期：2026-09-10。基于用户审核通过的 Solar 第二版和 Luna 第一版，最初实现于 `feat/astra-theme` 工作区；配色及后续细节修正统一提交合入 develop，见后续 astra-6 记录。

- 新增 Solar（暖白、亚麻灰、麦穗金回车键）和 Luna（深蓝灰、石板色、月光蓝）。Solar 回车底色 `#DEC69E`，文字 `#58452D`；Luna 回车底色 `#4A6483`。
- 内部 ID 为 `astra_solar` / `astra_luna`，避免与既有“明月／Luna”的 `luna` 冲突，旧配色保留。
- 独立键格、功能键分色，功能键与回车键的小字分别指定颜色；其他旧配色通过 fallback 保持原有提示色。
- Astra 开启“跟随系统深浅色”后，白天使用 Solar、夜间使用 Luna；长按阴阳图标手动切换时，暗色固定 Luna、浅色固定 Solar，并关闭跟随系统以保存选择。重新开启跟随后恢复自动切换。
- 不强制改变已有主题或配色选择。覆盖安装后，在 Astra 的配色列表选择 Solar / Luna，或长按阴阳图标切换；需要自动切换时开启跟随系统。
- 两个文本键盘的阴阳图标由 22sp 缩至 21sp。

## 验证

ARM64 构建、262 项单元测试（0 failure/error/skipped）、lint 均通过。测试覆盖系统昼夜选择、旧配色选择后的自动切换、手动选择刷新持久性、其他主题隔离与色值。审核稿色值、主布局小字映射及图标尺寸核对通过，APK 中 Astra YAML 与验证源码逐字节一致。签名、包名 `com.osfans.trime.debug.fresh`、显示名“同文输入法(Astra)”与仅 `arm64-v8a` 检查通过。

无三星实机 ADB 验证；审核图为布局预览，不是实机截图。

## 交付

`\\192.168.1.171\codex\trime-dev\trime-debug\trime-astra-5-arm64-v8a-debug.apk`

SHA256：`0635b5c9b815f3cbff570d8b5bafef16a2a7134928e8513e338956b490fbec35`，本地与 NAS 一致。构建日志：`E:\trime-build-env\astra-5-validation.log`。下一交付编号为 astra-6。
