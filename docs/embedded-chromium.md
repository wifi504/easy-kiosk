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
通过把内核 DEX、原生库和资产合入安装包，让隔离服务从已安装代码加载。
服务构造时检查 UID 范围，隔离进程跳过 React Native 初始化。
宿主混淆保留共享运行时接口，DEX 合并审计共享类的二进制接口，冲突时失败。

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
原生编译检查模式禁止生成可分发 APK；完整应用测试成功后才进入独立正式签名任务。
正式密钥不进入任何构建、第三方源码或模拟器任务。签名前后核对有效载荷一致。

当前接入代码已提交，但 **158 完整应用运行验证尚未通过，不能宣称交付完成**。
ARM64 实机内存、启动速度及具体业务网页还需要对应设备实测。