# Astra 13：选择性同步上游

日期：2026-10-13。

## 分支与合入范围

- 用户所称个人主线在本仓库为 `develop`（origin 默认分支），没有个人 `main` 分支。
- `src` 从 `15487288` 快进到上游 `e4934abf`，共 41 个提交，保持纯上游并推送到 `origin/src`。
- 个人主线保留现有主题、部署互斥、同步及常用符号逻辑，通过 `cherry-pick -x` 选取以下独立改动，保留上游来源。

| 上游提交 | 个人主线提交 | 改动 |
| --- | --- | --- |
| `7c2a9c35` | `cbeeea50` | 符号面板临时关闭、恢复英文标点时，修正选项名 `ascii_punch` 为 `ascii_punct`。 |
| `af23c4a6` | `0009dc8c` | 切回中文模式时清除 Shift 按下和锁定状态，并刷新键盘外观。 |

## 上游其他更新及暂缓原因

- 主题系统：读取源主题、扩展 librime 配置 DSL、加载时诊断，以及设置内复制/分享诊断；YAML 改用 kaml，主题/音效改用 kotlinx.serialization，重构配色、按键动作及缓存。涉及 Astra 主题、Solar/Luna 和现有主题迁移，需单独适配验证。
- 存储同步：引入 StorageAccess 2.0.0，重构 SAF 访问、目录权限持久化、文件选择与同步。与本 fork 原版/雾凇导入、备份及维护互斥交叉，暂不整体替换。
- 启动：增加 Direct Boot 支持，以及重启后 Rime 重试、等待主题和存储延迟可用后的恢复。涉及启动生命周期及数据目录，需连同上项审查，暂缓。
- 键盘：全局增加双击 Shift 锁定开关。Astra 文本布局已配置 `double_click: Shift_L3`；暂缓全局手势变化，保留现有行为。
- 工具链和 CI：Gradle 9.8.1、Boost 1.92.0、Spotless/ktlint 升级和 nightly 发布调整。本次个人主线不升级构建工具链。

这些内容均已进入 `src`，未视为已合入个人主线。后续同步仍需核对上述两个已回移提交，不能仅靠分支祖先关系判断。

## 验证

构建日志：`E:\trime-build-env\astra-13-validation.log`。
ARM64 Debug 构建、271 项单元测试（0 失败、0 错误、0 跳过）和 Android lint 通过；APK 签名、显示名、包名及仅 ARM64 检查通过。构建对应代码提交 `0009dc8c`，后续提交仅补充交接文档。

交付：`\\192.168.1.171\codex\trime-dev\trime-debug\trime-astra-13-arm64-v8a-debug.apk`。
SHA256：`fc3b046d924ca9fa36da56f9117dbe92176a98d290881b3c0a149024c8d96e7f`，本地与 NAS 一致，同目录保存 `.sha256`。

没有三星实机验证；建议实机核对英文大写锁定后切回中文，以及英文标点开启时从符号面板输入。
