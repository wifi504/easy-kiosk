# 移除远程管理

此轮在 `dev` 实施，不合并 `main`，所有测试与 APK 编译使用 GitHub Actions。

## 已删除范围

- REST HTTP 服务器、鉴权密钥、命令分发、状态查询、远程截图、远程相机流。
- MQTT Broker 连接、自动发现、命令订阅、状态与图片发布、密码和后台重连。
- Cloud 注册/扫码/预配令牌、凭据、配置推送、命令轮询、心跳、云端播放列表和服务器校时。
- 对应设置入口、多语言文案、缓存心跳报告、专属测试、后台连接保活模式。
- NanoHTTPD、HiveMQ 及仅为它们保留的 R8 规则、依赖声明和许可条目。

## 保留能力

WebView、Device Owner、开机启动、展台看门狗、本地 PIN/设置/备份、网页 HTTP Basic Auth、本地摄像头移动检测和网页语音播报保留。网页请求自身业务服务的联网能力不属于已删除的远程管理，INTERNET 权限继续保留。

原 HTTP 模块中承载的本地功能迁移到 `DeviceUtilitiesModule`，其中不含监听端口、远程命令执行或状态上报。Android TV 遥控器属于本地输入，与远程管理协议不同。

旧备份中的已删除功能字段不再导入，导出不会夹带旧的云端原始配置；现有本地网页和维护设置继续保留。卸载清数据不是本次迁移要求。

## 验证入口

运行 `.github/workflows/verify-android.yml`：TypeScript、相对 main 的 ESLint 新增错误检查、Jest、Android JVM 测试和 release APK 编译。构建包嵌入 JS，可脱离 Metro；当前沿用上游 debug keystore 作为开发测试签名，仅通过 Actions artifact 交付，不作正式发布。

“安装即用”的环境变量预置、品牌/包名切换、纯展台界面和正式签名仍是后续阶段，见 [实施计划](implementation-plan.md)。

ESLint 的存量问题以 main 的实际检查结果作为基线，不把第三方压缩脚本纳入本项目规则；流水线报告存量数量并阻止新增错误，不宣称整个上游已无 lint 问题。
