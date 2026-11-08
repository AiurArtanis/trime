# Astra 6：模式图标、Z 键提示与上游同步

日期：2026-09-10。

## 行为与提交

- 英文输入模式下，符号/明暗切换键显示原有 Command 符号 `⌘`；中文模式显示 21sp 阴阳图标。点击打开符号页、长按切换明暗的动作保持不变。
- 中文模式 Z 上方提示为 `·`；英文仍为 `-`。移除两个文本布局固定的 `label_symbol`，由实时输入模式决定提示；长按输出仍为中文 `·`、英文 `_`。
- 上述细节与 Solar / Luna 配色一起提交为 `a2a6406f`，主题分支 `feat/astra-theme` 已推送，连同此前独立主题实现快进合入并推送至 `develop`。
- 查询上游默认分支为 `develop`，新增 1 个提交 `15487288`：切换配色时刷新复用中的按键预览气泡和长按弹窗。
- `src` 从 `8351a570` 快进至 `15487288`，并推送到 `origin/src`，与 `upstream/develop` 完全一致；未加入个人定制。
- 上游修复涉及 5 个文件、33 行新增及 4 行删除；只补充弹窗刷新链，不改变个人布局、配色或输入行为。以 `310649ad` 无冲突合并到 `develop`。

## 验证与交付

合并前后 ARM64 构建、262 项测试（0 failure/error/skipped）与 lint 均通过。包名 `com.osfans.trime.debug.fresh`、显示名“同文输入法(Astra)”、APK 签名和仅 ARM64 ABI 检查通过，包内 Astra YAML 与验证源码一致。

交付：`\\192.168.1.171\codex\trime-dev\trime-debug\trime-astra-6-arm64-v8a-debug.apk`。

SHA256：`97071cb1699c2063340d032d8d3b85eaf3c301545ce0695804e5b24455db3248`，本地/NAS 一致。APK 对应源码提交 `310649ad`；后续仅文档提交。下一交付编号 astra-7。

详见构建日志 `E:\trime-build-env\astra-6-validation.log` 和合并后日志 `E:\trime-build-env\astra-6-upstream-validation.log`。无三星实机 ADB 验证，不能将本地测试当成手机验证。
