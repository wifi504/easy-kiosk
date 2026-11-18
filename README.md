# easy-kiosk

面向固定网页的 Android 专用展台项目，由 [FreeKiosk](https://github.com/RushB-fr/freekiosk) 派生。

目标是：在 GitHub Actions 中通过环境变量预置网址和展台参数，编译出安装后首次打开即可使用的 APK；管理员仍可通过 PIN 进入设置修改。

## 当前阶段

**目前仅完成改造准备，尚未实现安装即用。** 代码基线来自上游 `release/2.1.0-beta.3`，提交 `e7dfcc327b47e48c5468124bcbe3da5e8333409d`。此次清理的是文档和上游文档发布流程，业务代码、应用名称、包名、版本、设置入口及现有功能均未改造。

当前代码仍需按上游方式配置。仓库暂未提供 APK 编译 Action；根目录 `.env.example` 是下一阶段的变量契约，**当前程序不会读取这些变量**。不能直接复制示例后宣称已生成预置 APK。

## 目标使用方式

1. 在自己的 GitHub 仓库设置构建变量和签名密钥。
2. 手动运行 APK 编译 Action，下载对应设备配置的签名 APK。
3. 安装并首次启动，直接全屏打开预设网址，无需填写初始化表单。
4. 通过约定手势和管理员 PIN 进入设置；本地修改重启后继续生效。

首次启动不等于安装后自动执行。Android 授权、Device Owner 开通和设备首次部署属于系统步骤，APK 内置参数不能替代它们。

## 文档

- [文档索引](docs/README.md)
- [改造范围与行为约定](docs/product-scope.md)
- [构建变量契约](docs/build-configuration.md)
- [GitHub Actions 编译设计](docs/github-actions.md)
- [源码现状与实施计划](docs/implementation-plan.md)
- [设备部署与验收](docs/deployment-and-acceptance.md)
- [开发环境](docs/development.md)
- [来源、作者与许可证](docs/attribution.md)

## 项目与许可

本项目仓库：[wifi504/easy-kiosk](https://github.com/wifi504/easy-kiosk)。本地仅保留 `origin`，唯一开发分支为 `main`；上游地址仅用于来源说明，不配置为 Git remote。

原作者署名：**Copyright (c) 2025 Rushb**。原始 [MIT 许可证](LICENSE) 与 [第三方许可声明](THIRD-PARTY-NOTICES.md) 完整保留。easy-kiosk 是独立派生项目，不代表上游官方产品。
