# 开发环境与检查

## 当前工具链

源码为 React Native 0.82、React 19、TypeScript 与 Kotlin。当前 `package.json` 要求 Node >=20，拟 CI 固定 Node 22 和 JDK 17；Android SDK platform 36、build-tools 36.0.0、NDK 27.1.12297006，Gradle wrapper 8.13。这些数值来自仓库配置，修改依赖或工具链时需重新验证。

```text
npm ci
npm run typecheck
npm run lint
npm test -- --runInBand
```

`npm ci` 的 postinstall 会执行 patch-package，不能忽略补丁错误。开发调试可 `npm start` 配合 `npm run android`。本地 Android 环境需自行配置 SDK 路径和 JDK；`android/local.properties` 与本机 `gradle.properties` 不提交。

现有 release 基线命令为在 `android/` 执行 `./gradlew assembleRelease`（Windows 用 `gradlew.bat assembleRelease`），产物为 `android/app/build/outputs/apk/release/app-release.apk`。这仍是当前上游行为，不能读取 `.env.example` 生成预置展台包。当前缺少 release 签名会回退 debug keystore；后续正式 CI 将禁止该回退。

## 改造工作方式

唯一 remote 为个人仓库 `origin`，稳定分支为 `main`，改造与 Actions 验证使用 `dev`。当前作者配置为 `WIFI连接超时` / `localaccount@qq.com`，使用仓库本地配置，不更改全局作者。

业务功能、存储、原生桥接修改应按实施计划安排针对性测试；本次文档准备只做差异范围、内部链接、配置示例一致性及许可完整性检查，不用完整 APK 构建代替文档检查。

旧操作指南已从工作树移除。需要核对上游技术细节时，可从基线提交的 Git 历史读取，例如 `git show e7dfcc3:docs/adb-configuration.md`，不要将旧指南当成 easy-kiosk 已完成能力。
