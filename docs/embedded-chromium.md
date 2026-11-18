# 独立内嵌 Chromium 进度

用户要求内核随 APK 分发，仅由 EasyKiosk 使用，不更新系统 WebView。
当前生产应用尚未切换内核；不可把仅编译成功当成已经完成内核集成。

先通过手动 Actions `内嵌 Chromium 加载验证`，在 Android 10 / 1GB 模拟器上检查应用私有加载机制。
验证项目独立于正式应用，仅加载固定的本地 HTML 页面，不接收用户网址或配置。
为确保有可验证的 x86 Android 10 内核，机制试验使用 AOSP Chromium 134.0.6998.39；
它不是要求交付的最新版，不得将该试验包作为产品发布。
加载库源码固定在 JonaNorman/WebViewUpgrade 提交 5a5a7d11b5e67bba37787a697f45c9f5d45e6dda（Apache-2.0）。
内核包来自其 WebViewPackage 仓库，并固定 SHA-256 校验值。

后续产品门槛：

- 使用最新可兼容 Android 10 的稳定 Chromium，固定真实版本、来源、证书和 SHA-256。
- 初始化必须早于任何 WebView 实例，首次解压在后台进行，不阻塞主线程。
- 验证网页实际内核版本和现代 JS/CSS，不能仅修改 User-Agent。
- 验证离线冷启动、页面桥接、文件、媒体、证书、打印、恢复及渲染进程崩溃。
- 必须明确解决渲染进程隔离：当前试验库用与宿主同 UID 的普通子进程代替 isolated renderer，
  不能未经评估直接带入拥有 Device Owner 权限的产品。
- Android 10 / 1GB 的实际设备仍需实测内存、启动耗时与网页兼容性。

截至调研时官方 Android Chrome stable 为 158.0.8127.42；第三方预编译 Cromite 最新为 153.0.8010.37，
不能把它宣称为最新 Chrome。Google WebView 的 Android 10 与 Android 12L 包必须按 APK 清单区分，
不能通过修改 minSdk 值假装兼容。
