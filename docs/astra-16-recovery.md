# Astra 16 紧急恢复包

2026-11-11 用户报告 astra-15 设置和键盘均启动闪退。没有 ADB 设备或崩溃堆栈，根因尚未确认。此包是恢复到 astra-14 运行代码的紧急措施，不是 astra-15 根因修复证明。

稳定基线 `b03799b1`。保留版本 14 全部功能及原工具链，versionCode 从版本 15 的 `20270101` 提升至 `20270102`。包名 `com.osfans.trime.debug.fresh` 和签名保持不变，解决旧版不能覆盖新版的问题。

启动前检查被版本 15 迁移后改名的 `clipboard.db.legacy` 和 `collection.db.legacy`。旧目标文件缺失时，复制并校验 SQLite 主文件及 WAL/SHM，主文件最后原子发布；同内容的部分恢复可重试，不覆盖不同内容的现存文件。无 `.legacy` 时只使用带 `.complete` 的升级快照。原文件、版本 15 新库 `clip.db` 和快照全部保留。新版独有记录不自动反向合并，后续重新迁入新数据库时必须特别审查，不能直接沿用版本 15 的已迁移标记。

请覆盖安装，不卸载、不清除数据。未进行真实三星覆盖安装验收；本地验证仅检查构建、单元测试、lint、签名、包名、版本号和 NAS 哈希。常用符号、词库和用户配置沿用现存路径。

每周自动化 `astra` 暂停。上游迁移源码和历史保留，待取得启动故障日志并验证修复后再继续。NAS 的版本 15 文件保留作为故障证据，同时增加禁止安装说明。

## 交付凭据

- 恢复包代码 `3c5a72d6`；紧急回退合并 `f043df0d`，已推送个人主线 develop；故障版本保存于 `archive/astra15-startup-regression`，纯上游 src 不回退。
- ARM64 构建及 300 项测试通过，0 失败/错误/跳过；lint 0 errors / 84 warnings。日志 `E:/trime-build-env/astra-16-recovery-final.log`，最终打包日志 `astra-16-recovery-package.log`。
- NAS：`\\192.168.1.171\codex\trime-dev\trime-debug\trime-astra-16-recovery-arm64-v8a-debug.apk`，附 `.sha256`，本地和 NAS 校验一致。
- SHA256：`12ce24631cf7f86c1ccd380fec46e5b8d2e84b381ccbe40a8e9074b0b2ad6428`。
- 签名与 astra-15 一致；`20270102 > 20270101`，可走正常升级安装。versionName 保留 `3.3.13` 表示恢复运行代码，不影响 Android 的 versionCode 升级判断。
- 用户暂时无法连接手机；未取得崩溃堆栈，未验证真实覆盖安装。等待用户确认恢复效果，不能将这些本地检查记为已解决实机启动故障。
