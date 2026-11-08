# Astra 3 部署修复核对

日期：2026-09-10。基于 `feat/fresh-setup-rime-sync` 的 `521576cd` 和 astra-2 未提交工作区；保留原有显示名、导入目录识别及其测试。没有提交或推送。

## 结论与证据边界

本次修复的是源码可证实的生命周期缺陷，尚无三星手机日志或 ARM64 实机运行结果，不能将这些缺陷等同于用户全部故障的已确认根因。

- JNI 原先启动异步维护后立即返回，Kotlin 随即标记 READY；librime `Deployer::Run` 在工作线程真正退出前发送 success。现在 startup 和 sync JNI 均 join 工作线程，再允许会话操作及文件替换。完整部署未启动时主动报告 failure，避免无事件空等五分钟。
- 外部目录导入原先在 `exitRime` 前运行，设置页还有直接导入入口。现在导入在引擎关闭期间执行，MaintenanceGate 在 SAF 挂起期间阻止输入/API 操作，恢复完成后释放；绘制键盘用的开关查询在维护期间立即返回缓存，避免主线程等待大词库。
- 主题异步选择、主题导入以及重启入口使用维护互斥。重启入口也实际遵守 fullCheck 参数。启动成功通知触发的主题加载先等待 READY。
- 守护通知原先使用与输入共用的 15 项 DROP_OLDEST SharedFlow，还会在 start 后清空 logcat。现在使用独立、不丢弃的维护事件 Channel，移除自动清日志；单次通知异常不会结束后续消费。
- 原版仅暴露 shared 时，目标 `/rime` 中已有的词库/Lua 等依赖原先不会进入本地安装。现在合并缺少的文件，并保留目标 custom YAML、default.yaml、user.yaml 和 custom_phrase.txt。相同内容不重复写回和备份。
- SAF 遍历排除 `.bak` 和隐藏临时文件，防止备份再次进入运行目录；部分复制失败不再被当作完整导入成功。

## 诊断与复测

应用日志页右上角菜单 → **导出维护诊断（不含输入内容）**。此入口只导出专用维护记录：时间戳、固定阶段名、异常类名及构建版本；不读取 logcat、词库、个人输入、剪贴板、配置正文或文件路径。记录持久化并限制大小。原有普通日志导出仍是普通日志，不能视为无输入内容的诊断入口。

故障通知中的日志也使用维护阶段快照。它能区分 prepare、停机、native start/success/failure 和 join 返回，但不包含 native 编译错误原文，因此不能单凭该记录确定具体词库或 YAML 错误。

建议在三星设备按以下顺序验证，保留现有目录及数据库：

1. 覆盖安装 astra-3，确认包名未变，打开键盘，等待当前部署结束后切换中英文。
2. 单独验证外部 `/rime` 导入、雾凇方案及 tongwen 样式，观察完成通知是否结束。
3. 执行一次同步雾凇，确认完成后输入和中英文切换恢复；检查 custom YAML、短语和方案选择仍在。
4. 如仍失败或长时间不结束，通过上述专用菜单导出维护诊断，记录所走入口和等待时间。

## 本地验证

- 仅 `BUILD_ABI=arm64-v8a`，未构建 x86/x86_64，也未以旧模拟器 APK 作为本次证据。
- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`。
- 252 项单元测试，0 failure / error / skipped。新增 8 项覆盖互斥、挂起/取消、缓存查询、配置合并和备份排除。
- APK 包名 `com.osfans.trime.debug.fresh`，显示名 `同文输入法(Astra)`，ABI 仅 `arm64-v8a`。
- APK v1/v2 签名验证通过；证书 SHA256 `bca2da32430f40ac2bb99371570306379c26d87cffec82b87041764b5a2bf9da`。
- 构建日志：`E:\trime-build-env\astra-3-validation.log`。交付后以 NAS 同名 `.sha256` 文件记录 APK 哈希。
- 最终构建成功；交付 `\\192.168.1.171\codex\trime-dev\trime-debug\trime-astra-3-arm64-v8a-debug.apk`，23,818,455 字节。NAS 与本地 SHA256 均为 `1145f1db813c5d60df3ec35288479b984b8da2c3fc95d233a4b1f43a5d42cbdf`。签名证书与 astra-2 一致。

## 尚存限制

native join 等待真实维护结束，不会强行中断 librime 线程；若 native 真正挂死，它不是硬超时恢复机制。原版导入和雾凇安装沿用备份/回滚，不保证断电、进程被杀时多文件事务原子性。普通外部镜像仍是逐文件同步，失败会报告错误并重启引擎，但不会自动还原整个目录。维护修复不能修复源配置本身缺失或无效的内容。
