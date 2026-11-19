# EasyKiosk 内嵌 Chromium

用户已确认使用现成的 **158.0.8127.36 稳定版**，优先完成集成。
它不是此前查询到的官方最新补丁 158.0.8127.42，不能混称。

## 来源与构建

预编译 WebView 来自 `holtdroid/chromium-builds` 的 158.0.8127.36 Release，
ARM64 / x86_64 都有独立 WebView APK。该项目公开 Actions 和构建脚本，
说明内核来自官方 Chromium 版本标签，使用公开测试签名，不含 Google 服务或 Widevine。

`config/chromium.json` 固定官方源码提交、提供方、Release ID、资产 ID、文件名和 SHA-256。
所有内核下载、图标生成、构建和测试只在 Actions 执行，不向本地下载内核或安装依赖。
下载时核对发布与资产身份及校验值；使用前再次验证 APK 签名、实际版本、ABI、
最低系统要求、渲染服务及许可资产。Android 10 不兼容时直接失败，不修改 minSdk 伪装兼容。

普通 dev 提交直接复用固定预编译内核和下载缓存，不再触发或等待源码编译。
此前的耗时源码流水线已停用；历史失败和取消记录保留在 GitHub。

## 隔离机制

加载器固定为 JonaNorman/WebViewUpgrade 提交
`5a5a7d11b5e67bba37787a697f45c9f5d45e6dda`（Apache-2.0）。
原版同 UID 子进程已替换为真正的 `isolatedProcess` 服务。
内核 DEX 保留为安装包内的签名只读资产；Android 10 AppComponentFactory 为隔离进程建立
以 BootClassLoader 为父的 InMemoryDexClassLoader，配合已安装原生库加载真实渲染服务。
宿主 DEX 不与内核 DEX 混合，内核 AndroidX/Kotlin/GMS 依赖不和宿主共享。
服务构造时检查 UID 范围，隔离进程跳过 React Native 初始化。
读取安装包 DEX 时逐份校验 SHA-256，服务启动必须同时满足隔离 UID 与独立类加载器。

机制实验 Actions 44272666856 已通过：Android 10 / 1GB，Chromium 134.0.6998.39，
实际渲染 UID 99000，JS/CSS、HTTP/fetch 和宿主私有文件拒绝访问均通过。
134 只用于机制实验，不能交付或当成 158 的验证结果。

## 正式应用验证门槛

首次 WebView 创建前必须完成固定内核加载；失败不回退系统 WebView。
默认 UA 取自真实内核，不伪造浏览器版本。应用最低 Android 版本为 10。

Actions 先执行类型、已有单元测试、合并工具及原生 Manifest/Kotlin 检查，
然后分别构建 ARM64 / x86_64 的完整 release 内容。x64 在 Android 10 / 1GB / 16GB /
1080×1920 / 144dpi 模拟器上检查首次启动、缓存冷启动、真实版本、JS/CSS、
HTTP/fetch、React Native 桥接、隔离 UID、系统 WebView 不变，记录截图和内存报告。
小型 158 隔离验证与两个完整应用架构构建并行；全部运行验证成功后才进入独立正式签名任务。
正式密钥不进入任何构建、第三方源码或模拟器任务。签名前后核对有效载荷一致。

158 的 x86_64 完整应用运行验证已通过；新增 157 的 x86 32 位运行验证也已通过。
ARM64 实机内存、启动速度及具体业务网页还需要对应设备实测。
## 32 位兼容构建

四架构 Actions 使用预编译内核，不从源码构建 Chromium：

| 应用 ABI | Chromium | 提供方 |
| --- | --- | --- |
| arm64-v8a / x86_64 | 158.0.8127.36 | holtdroid/chromium-builds |
| armeabi-v7a / x86 | 157.0.8103.78 | LineageOS |

32 位使用 LineageOS 官方 GitHub 预编译仓库的固定提交，下载 Git LFS 实际对象，
校验固定 SHA-256、长度、APK 签名、版本、最低系统要求与架构；其构建参数和补丁集合
固定在 android_external_chromium-webview_patches 的提交
544b12e3d42a02b4337f31819f6ba917187dcccf。来源链接与 Chromium 许可、Credits 一起入包。
32 位不伪装为 Chromium 158，不回退设备系统 WebView。
x86 32 位新增 Android 10 / 1GB / 16GB 完整应用运行、冷启动、隔离与 TOTP 测试。
ARM32 和 ARM64 均构建与执行原生单元测试，但需要厂商设备补充真实 ARM 运行验证。
正式签名任务保持独立，不执行第三方代码；最终只上传四个独立 APK，删除临时交接附件。

### 四架构验证结果

Actions https://github.com/wifi504/easy-kiosk/actions/runs/44511816206 全部成功，
对应应用提交 a363fae。四个 release APK 均使用既有正式证书签名，
最终附件仅保留四个原始 APK，清理任务已删除所有临时交接附件。
ARM32 内核实际 minSdk=29，内核与最终应用的 ELF 检查分别确认 2 与 19 个原生库均为 ARM32。
x86 32 位完整应用实测 Chromium 157.0.8103.78，现代 JS/CSS、HTTP、桥接、隔离 UID、
系统 WebView 保持不变及网络时间 TOTP/防重放检查通过。
本次云端模拟器首次启动 8909ms，缓存冷启动 4834ms；不能代表 ARM 实机性能。
LineageOS 32 位包携带的 Credits HTML 约 9.3MB，非 sample credits，已保留原文。
ARM32 实机尚未连接，采购设备仍须通过安装及业务页面运行验收。
