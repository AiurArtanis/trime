# 同文输入法(Astra) 开发交接

## 2026-09-10 astra-6 最新状态

- 当前主线 `develop`。独立 Astra 主题、Solar / Luna 和模式图标、Z 提示修正均已合入；主题配色提交 `a2a6406f`，上游合并提交 `310649ad`。详见 [astra-6-refinements.md](astra-6-refinements.md)。
- `src` 与上游 `develop` 一致，均为 `15487288`，已推送 `origin/src`。本次上游只新增弹窗配色刷新修复，无冲突合入主线。
- astra-6 已交付 NAS，262 项测试、lint、ARM64 构建、签名与包内容检查通过；尚无三星实机验证。下一编号 astra-7。
- 下文为历史记录；其中未提交、未合并和下一编号的描述均按对应版本理解。

## 2026-09-10 astra-4 最新状态

- 切换修复连同 astra-3 工作区已提交为 `4513e1ad`，合并并推送至 `origin/develop`。
- 当前开发分支 `feat/astra-theme`，主题实现提交 `94277e32`；详细说明见 [astra-4-theme.md](astra-4-theme.md)。主题分支尚未合并到 develop。
- Astra 独立主题、标准上游布局还原、主题去重、文本符号长按和数字键盘布局完成。259 项测试、lint、签名及仅 ARM64 的包内容检查通过，仍无三星实机验证。
- NAS 已交付 `trime-astra-4-arm64-v8a-debug.apk`，SHA256 `f834683b1322b5840c2237d2819d0dcb62e68b98f42a51197e7226eacbd3c8e4`，本地/NAS 一致。下一交付编号 astra-5。
- 安装后需要在主题列表选择 Astra，现有选择不会自动替换。手动长按阴阳图标切换明暗会关闭跟随系统明暗，以持久保存手动选择。

## 2026-09-10 astra-3 接手后更新

astra-3 已完成源码修复和 NAS 交付，详细证据、改动和限制见 [astra-3-validation.md](astra-3-validation.md)。本文件下文保留 astra-2 接手时的背景；其中“下次 astra-3”“没有重建 APK”等描述属于旧状态。

- 当前工作区包含 astra-2 原有修改和 astra-3 新增修改，均未提交、未推送。
- 已修复 native 维护结束等待、停机后导入、输入/维护互斥、通知事件丢弃、主题和重启入口互斥；补齐 shared-only 导入所需的目标目录依赖、用户配置保护与备份过滤。
- 新增日志页菜单“导出维护诊断（不含输入内容）”，只导出专用阶段记录，不读取普通日志或个人输入。
- 最终 ARM64 构建、252 项测试、lint、包名/显示名/签名/ABI 检查通过；NAS 和本地 SHA256 一致。产物 `trime-astra-3-arm64-v8a-debug.apk`，下一交付编号为 astra-4。
- 仍未拿到三星实机日志，也没有此次 ARM64 实机验证；不能据本地通过宣称用户故障全部解决。后续先查看实机反馈与维护诊断。

更新日期：2026-09-10。本文是新会话的任务入口，不要求继承旧会话推理。

## 当前任务与实机反馈

用户希望按个人习惯维护自己的同文输入法 fork，名称为“同文输入法(Astra)”，目标仅为三星手机。优先恢复稳定输入，不继续叠加功能。

astra-2 用户最新反馈（未拿到手机日志，原因尚未确认）：

1. 初始化“同步原版”期间，Rime 守护程序部署失败，随后同步失败。
2. 不走同步原版，直接选择外部目录，设置雾凇拼音和 tongwen 样式，也不能切换中英文。
3. Rime 守护程序长时间显示部署中；很久后可以输入，但后台仍显示部署。
4. 点击“同步雾凇”后再次无法输入。

请独立审查启动、部署、外部配置同步、互斥锁和事件通知生命周期。不要把先前模拟器成功当成实机已修复，也不要未经日志就认定只是通知未清除。核对长任务是否正常结束、异常是否恢复引擎、启动是否反复触发部署、状态通知是否正确配对；这些是排查方向，不是根因结论。

用户远程发送需求，无法提供实时 ADB 实机调试。必要时设计可由应用导出的诊断日志，避免输出个人输入内容。不要通过删除用户目录或数据库来掩盖问题。

## 仓库与分支

- 本地：`E:\Work\GitHub\trime`，上层规则：`E:\Work\GitHub\AGENTS.md`。
- 用户 fork（origin）：`git@github.com:AiurArtanis/trime.git`。
- 上游（upstream）：`https://github.com/osfans/trime.git`。
- 上游默认开发主线是 `develop`，不是 master；`main` 是发行线。
- `src`：纯上游镜像，当前 `8351a570`，不加入用户定制。它跟踪 `upstream/develop`。该哈希是此前同步结果，不代表未来最新。
- 本地 `develop`：`09d95310`，跟踪 `origin/develop`。
- 当前工作分支：`feat/fresh-setup-rime-sync`，HEAD `521576cd`，跟踪同名 origin 分支。
- 核心重建提交：`b2a588b2`；`521576cd` 仅文档更正。
- 保留旧线：`feat/backspace-clear-composition`（`4a831d46`）、`fix/v21-rime-ice-stability`（`454912fc`）。不要整体合回旧实现。

重要：astra-2 是当前未提交工作区构建的，不只对应 HEAD。接手前运行 git status/diff，保留以下改动：

- `app/build.gradle.kts`：Debug 显示名改为“同文输入法(Astra)”。
- `app/src/main/java/com/osfans/trime/data/sync/ConfigurationTransfer.kt`：修复原版 files 下只有 shared 时被跳过的问题。
- `app/src/test/java/com/osfans/trime/data/sync/ConfigurationTransferTest.kt`：四个目录选择回归测试，未跟踪新文件。
- `docs/fresh-rebuild.md`：NAS 版本命名和仅 ARM64 的约定。
- 本交接文件。

## 保留的功能要求

- 标准 tongwenfeng 主题：行间距 8，键盘高度竖屏/横屏 230/180。
- 候选基准 22sp；竖屏单码点候选 21sp、双/三码点 23sp，其余 22sp；横屏 22sp，按宽度自然排布，不固定 8 字。
- 仅按下退格时已有组合输入，长按 600ms 清空组合输入；触发后不继续删除宿主文本。没有组合输入时维持正常连续退格。开启按键振动时触发对应反馈。
- 初始化存储页提供“同步原版”，配置导入主存储根目录 `/rime`（`/storage/emulated/0/rime`）。首次 Android 目录授权不可绕过。
- 更多菜单提供“同步雾凇”：点击自动从官方仓库下载并更新、时间戳 .bak 备份、成功/失败提示；不应每次弹目录选择器。
- 保护用户 custom YAML、自定义短语、用户数据库和已有设置，不擅自改变已选方案。
- 历史需求还包括数字九宫格右上角退格、右上角下两格空格，及常用符号持久化；本次纯上游重建没有迁移这些功能，不能声称已实现。
- 个人词汇学习和一般自动化方案已被用户暂缓，不在当前修复范围。

## 当前实现与限制

- 包名仍是 `com.osfans.trime.debug.fresh`，用于隔离旧 Debug 的坏状态，与原版共存；名称不是包名，不要随意变更包名导致授权丢失。
- 原版 SAF provider：`com.osfans.trime.provider`，根 `files`。用户截图证实此处只有 shared 子目录；目标 primary:rime 已有 cn_dicts、en_dicts、lua、build 和用户数据。
- astra-2 修复了 shared-only 文件发现，但并未解决最新报告的部署失败。
- 导入排除 build、sync、活动 .userdb、隐藏路径、备份及 installation.yaml。因此不是逐字节全量复制，需保护运行数据。
- 雾凇更新下载 iDvel/rime-ice nightly full.zip，验证 GitHub SHA256，解压检查路径和体积，保留个性配置，不强制选方案。
- 当前外部同步在维护互斥锁下完成；本地安装在引擎停止时进行；重启和部署事件等待逻辑是优先审计对象。
- 备份/异常回滚不是断电或进程被杀情况下的多文件原子事务。

重点文件：`core/Rime.kt`、`core/RimeApi.kt`、`core/RimeDaemon.kt`、`ui/setup/SetupActivity.kt`，以及 `data/sync/` 内 ConfigurationTransfer、ConfigurationInstall、ExternalConfigurationInstall、RimeIceUpdate、ImportedThemeTuning。

此前构建修正：Windows Git 符号链接占位文件由 DataChecksumsPlugin 准备为真实 YAML 资源；Kotlin incremental 已禁用，避免接口委托残留导致 AbstractMethodError；native min_log_level=1 避免逐词 INFO 洪泛。不要无依据撤销这些修正，但也不要把它们当成这次三星故障的已知根因。

## 编译、验证与发布

环境集中在 `E:\trime-build-env`。PowerShell 必须 dot-source：

```powershell
. E:/trime-build-env/trime-env.ps1
$env:BUILD_ABI='arm64-v8a'
gradle.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

- 仅构建 ARM64（准确 ABI 名为 arm64-v8a）；用户明确不要再编译 x86/x86_64，包括为了模拟器而偷偷增加构建架构。
- JDK17、Gradle9.5.1、Android SDK/NDK 和缓存均由环境脚本配置；SDK 为 `E:\trime-build-env\android-sdk`。
- APK 从 `app/build/outputs/apk/debug/output-metadata.json` 获取，不凭猜测挑旧产物。
- 所有交付 Debug APK 放 NAS：`\\192.168.1.171\codex\trime-dev\trime-debug`。
- 文件名：`trime-astra-N-arm64-v8a-debug.apk`，逐次发行递增，不覆盖旧文件。
- 已交付 astra-1、astra-2；下次 **astra-3**。该编号目前是交付文件名，不等于 Android versionCode/versionName。
- 应用显示名必须“同文输入法(Astra)”。
- 发布前验证编译、相关回归测试、APK 签名、包名、显示名、ABI，复制后比较 NAS 和本地 SHA256。
- astra-2：244 项测试通过，lint 成功；仅目录识别有回归测试，没有本次三星实机验证，用户反馈说明部署仍有问题。
- 先前 b2a588b2 曾在 Android15 x86_64 模拟器验证导入、部署、更新、切换中英文；这只说明当时样例通过，不覆盖用户现有大词库/外部目录/三星环境。模拟器旧 APK 不能作为新 ARM64 修改的测试证据。
- 旧模拟器可能仍在运行，AVD trime-fresh-test，adb emulator-5554；不要求重新构建 x86。

## 工作约定

中文沟通，结论在前。先读现状再改，保留未提交改动。此次交接没有提交、推送或重建 APK。
新会话应先复核本文与实际源码，针对最新故障推进修复；不要继续沿用“已修复”的旧结论。无法验证的行为明确说明，不用不断发布未经诊断的版本代替定位。
