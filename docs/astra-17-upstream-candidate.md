# Astra 17 上游整合版修复候选

2026-11-11，用户要求先依据已知故障修复原“最新上游 + Astra”整合版，下次连接手机后再联调。候选独立维护，不替换当前可用主线。

## 修复依据

无线 ADB 已确认 astra-15 在 `DataManager.sync -> copyFile` 中抛出 `FileNotFoundException: shared/opencc/JPVariants.txt`。原 APK 清单声明 57 个文件，JPVariants.txt、JPVariantsRev.txt 实际缺失。OpenCC 安装任务会删除不再存在的字典，但清单任务在非增量执行时继续加载旧输出；此时没有旧文件 REMOVED 事件，错误条目进入 APK。

## 本次处理

1. 清单每次执行都重新遍历本次资源树，不读取旧输出；输出本身不进入清单，路径使用 RELATIVE，目录按现存文件重建。
2. Windows 未创建符号链接的 Git 检出先转换成真实资源内容，输出在 build/generated 下；拒绝缺失或越出 rime 数据目录的链接，不把链接路径文本当成配置文件。
3. 每个 APK 变体新增 `verify<Variant>ApkAssets`，作为 assemble 依赖。检查最终 ZIP 的清单与实际 assets 集合一致，逐文件计算 SHA256，并拒绝未解析的 Git 链接。不是仅验证源目录或 APK 整体哈希。
4. 从 astra-16 返回新框架时，版本 15 的布尔迁移标记不能代表恢复后数据库已处理。现在重新读取存在的旧库，通过正文、类型、置顶及时间判断重复，使用新 ID 插入差异行；保留新库独有记录和恢复版的新记录。不同版本内容可能显示为两条记录，以保留数据为先。事务成功后旧库使用唯一名称归档，不覆盖既有 `.legacy`。
5. 迁移前创建 `files/astra-upgrade-17/private`，同时快照旧库、新库、WAL/SHM、常用记录与设置；不覆盖版本 15 的快照。

## 验证范围

构建逻辑测试覆盖：非增量任务移除旧清单残留、资源删除、Windows 链接解析、匹配 APK 通过、缺失资源失败、字节篡改失败、未声明资源失败、未解析链接失败。应用回归、ARM64 构建、lint 和最终 APK 检查另记结果。

本次不安装手机，不声称启动或数据库再次升级已通过实机验证。保留完整上游主题、存储、Direct Boot 与 Astra 扩展；基线为原 astra-15 的 `a6307961`，并非在本轮额外拉入新上游更新。

已通过 7 项构建逻辑测试、389 项应用测试（均无失败/错误/跳过）、ARM64 构建、spotlessCheck 和 lint（0 errors / 65 warnings）。最终 APK 的 55 个清单文件全部通过存在性和 SHA256 检查。将 NAS 原始 astra-15 APK 输入同一个 Gradle 校验任务时，任务按预期失败，准确列出 JPVariants.txt 和 JPVariantsRev.txt 缺失。负向验证日志 `E:/trime-build-env/astra17-bad-apk-rejected.log`；完整日志 `astra-17-candidate-final.log`，构建逻辑日志 `astra-17-buildlogic-final.log`。

## 下次手机联调

- 先确认当前设备、恢复版 versionCode 与可用状态，备份现有应用数据库、设置、常用记录和词库，不输出用户内容。
- 准备 versionCode 至少 `20270104` 的恢复包，并使其能优先恢复本次测试前快照；当前候选 `20270103`，不能依靠较低版本号的 astra-16 直接覆盖。
- 验证候选签名后覆盖安装，观察设置启动、Rime 启动、拼音候选上屏、中英切换、Solar/Luna、符号历史及频次、语音入口；检查剪贴板/收藏迁移及词库、外部同步授权。
- 出现故障先保存本应用的日志、回滚保障和原数据，不清空数据尝试修复。验证通过后再决定合并 develop、交付正式版本和恢复每周自动化。
