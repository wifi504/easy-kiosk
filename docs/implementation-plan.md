# 源码现状与实施计划

## 当前证据

基线：上游 `release/2.1.0-beta.3`，本项目稳定分支为 `main`，开发验证使用 `dev`；不要重新添加 upstream remote 或复制上游其他分支。

| 源码位置 | 现状与后续关注点 |
| --- | --- |
| `App.tsx`、`src/navigation/AppNavigator.tsx` | 加载语言和导航，当前首屏为 Kiosk；拟在界面挂载前等待预置初始化 |
| `src/utils/storage.ts` | `KEYS` 与持久化读写；URL 未配置返回 null，锁定和开机启动默认 false |
| `src/screens/KioskScreen.tsx` | `loadSettings()` 先处理待应用 ADB 配置，再批量读 `getAllSettings()`；存在本地默认值，AUTO_RELOAD 与单独 getter 的默认值不一致，不能只改一个 getter |
| `src/screens/settings/SettingsScreenNew.tsx` | 设置读取与保存入口，后续保留维护必需项与可修改能力 |
| `src/utils/secureStorage.ts`、`src/screens/PinScreen.tsx` | 管理员认证；预置 PIN 要沿用安全存储，不另造明文认证路径 |
| `android/app/src/main/java/com/freekiosk/BootReceiver.kt` | 原生开机路径直接读取 `@kiosk_*`；不能只让 JS 看到默认参数 |
| `KioskModule.kt`、`KioskWatchdogService.kt`（同目录） | 锁定、Device Owner 与恢复；保留系统边界，检查初始化顺序 |
| `android/app/build.gradle` | 包名与版本写死；签名缺失回退 debug；会把第三方声明复制进 APK |
| `android/build.gradle`、`android/gradle/wrapper/gradle-wrapper.properties` | SDK/NDK/Kotlin 与 Gradle 版本来源 |
| `patches/` | WebView、摄像头等原生依赖补丁；裁剪前不能直接删除 |

## 分步实施（全部待做）

1. **配置契约与生成器**：实现环境变量校验、类型化 schema、跨 JS/Kotlin 默认配置生成；测试缺参、布尔值、带查询参数 URL、转义、PIN/版本/ABI 边界与日志脱敏。
2. **首次启动与迁移**：同一配置源、先初始化后消费、幂等写入、失败不写成功标记；测试首次启动、重启、升级、本地 false/0 值、PIN 修改、恢复预置及原生开机路径。
3. **应用身份**：改显示名与包名，同步 Kotlin/Manifest/ADB/脚本引用；保留来源署名与许可，禁止与上游官方签名或应用混淆。新包名意味着独立安装，不承诺覆盖上游安装。
4. **纯展台界面**：保留单网页、维护设置与 PIN；去除无关模式入口、Cloud 注册营销及首次初始化表单；不误删网页、看门狗、设置、权限或恢复依赖。
5. **Actions 与签名**：实现手动编译、Secrets、artifact、签名检查与清理，禁止 debug fallback；先验证可重复构建，再运行部署验收。
6. **性能与设备**：Android 10、1GB 内存实测，关注 WebView OOM、渲染器恢复、网络重连、长时间运行和设备重启，不能以桌面模拟器 CPU/GPU 等价于展台硬件。

## 本次准备完成范围

- [x] 中文 README、文档索引、配置契约、CI 设计、源码定位与验收清单。
- [x] 移除上游产品介绍、旧路线图、旧操作文档和文档宣传图片；技术历史仍可通过 Git 查阅。
- [x] 移除 Wiki 自动同步 workflow，重写贡献和问题反馈模板。
- [x] 保留 MIT、原作者、第三方完整许可与业务源码。
- [ ] 实现上述六步（下一阶段，不属于本次准备）。

此前文档准备阶段未运行 Actions；当前远程功能裁剪已获授权，可在 dev 提交、推送并运行 Actions，禁止本地构建和未经要求合并 main。后续提交作者为 `WIFI连接超时 <localaccount@qq.com>`，配置限定在此仓库。

## 当前新增阶段

REST API、MQTT 与 Cloud 功能裁剪见 [删除范围与验证](remote-removal.md)。此前“不修改业务”的限制仅适用于文档准备阶段，不约束用户随后明确授权的功能删除。
