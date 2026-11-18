# EasyKiosk 签名与版本

正式 APK 使用专用 RSA 4096 位 JKS 密钥，别名 `easy-kiosk`，有效期 10000 天。
仓库仅记录配置，不存储密钥和密码。GitHub Actions Secrets：

- `EASYKIOSK_KEYSTORE_BASE64`
- `EASYKIOSK_STORE_PASSWORD`
- `EASYKIOSK_KEY_ALIAS`
- `EASYKIOSK_KEY_PASSWORD`

Actions 独立签名步骤临时还原密钥，签名完成后清理。缺少密钥时不允许回退到测试签名。
密钥及密码应独立备份；之后发布始终沿用此密钥。

Android `versionName` 为构建提交 SHA 前 7 位；设置界面从已安装 APK 读取。
`versionCode` 为 100000 加 Actions 运行编号，保证同一流水线后续构建递增。
应用显示名称为 EasyKiosk；保留原有包名、原生模块注册名、配置键和网页桥接接口以兼容已有功能。
原 FreeKiosk 签名或测试签名安装不能直接用这份正式签名覆盖；先备份配置，测试设备上卸载重装。
Device Owner 设备需先妥善解除设备管理再换装。

开屏仅保留原图标与“开始配置”按钮，通过按钮进入原有 PIN/配置流程。
版本信息仍可在设置页查看，开屏不展示。原作者 MIT 许可保留在仓库和 APK assets 中。
构建与测试只在 GitHub Actions 执行。

## 签名作业隔离

Chromium/加载器构建、DEX 合并和模拟器测试作业不接收正式签名 Secrets。
这些作业仅使用公开测试密钥处理临时测试包，内部产物明确标记不可部署、仅保留 1 天。
测试全部通过后，独立签名作业在全新的运行器上只解析和签名 APK，不检出源码、不运行 Gradle/npm、
不加载第三方构建代码、不执行 APK。正式凭据只在该签名步骤可见，临时密钥在退出时删除。
正式签名前后校验 APK 非签名内容的规范化哈希，确保发布的代码与已测试的 release 内容相同。
最终仅 `EasyKiosk-signed-*` 为可部署正式签名产物。
