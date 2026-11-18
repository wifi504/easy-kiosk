# GitHub Actions APK 编译设计

状态：准备方案。当前没有可运行的 APK 编译 workflow；本次删除上游 Wiki 同步流程，不触发编译或发布。待配置生成、初始化及签名接入完成后，才创建 `.github/workflows/build-apk.yml`。

## 触发和输入

首版使用 `workflow_dispatch`，可选输入只覆盖网址、配置版本、应用版本和 ABI；PIN 和签名密码只从 Secrets 读取。仅从 `main` 手动运行，不在 PR（尤其外部 fork PR）中使用部署密钥或签名密钥。不自动发布 GitHub Release，不推送配置生成结果。

非敏感参数见 [构建变量契约](build-configuration.md)。签名 Secret 拟采用：

| Secret | 内容 |
| --- | --- |
| `KIOSK_ADMIN_PIN` | 初始管理员 PIN |
| `ANDROID_KEYSTORE_BASE64` | 固定 release keystore 的 Base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | alias |
| `ANDROID_KEY_PASSWORD` | 私钥密码 |

密钥文件与密码不进 Git、不进入 artifact、不回显。当前 Gradle 仍使用 `FREEKIOSK_UPLOAD_*` 属性并在缺失时回退 debug keystore；未来正式流水线必须先校验签名完整，且缺失时明确失败，禁止把回退 debug 签名的包作为正式预置包。

## 计划流水线

1. 校验分支和全部必填参数；只授予 `contents: read`。限制并发，同一环境的构建不相互覆盖。
2. Checkout 指定提交，使用 Node 22、JDK 17；`npm ci` 执行现有 patch-package，禁止忽略依赖补丁失败。
3. 安装 Android SDK platform 36、build-tools 36.0.0、NDK 27.1.12297006，使用仓库 Gradle 8.13 wrapper。工具链数值取当前项目配置，不混同最低可运行 Android 版本。
4. 运行生成器并检查类型/schema/URL/PIN/版本/ABI；用安全的环境变量传参和 JSON 序列化，不把用户输入拼进 shell 脚本。含 PIN 的生成文件不上传。
5. 执行 `npm run typecheck`、`npm run lint`、`npm test -- --runInBand`；新增配置生成器与首次启动迁移的针对性测试。
6. 将固定 release keystore 解码到 runner 临时目录，临时生成 Gradle 签名属性，再执行 `./gradlew --no-daemon assembleRelease -PreactNativeArchitectures=...`。JS 必须嵌入，不能依赖 Metro。
7. 使用 `apksigner verify --verbose --print-certs` 验证签名；用 APK 分析工具核对包名、versionCode、versionName、ABI、启动 Activity、许可证资源和所需权限。
8. 产出 `easy-kiosk-<version>-<config-revision>.apk`、SHA-256 文件和无敏感数据的构建清单（源码 SHA、工具链、版本、ABI、配置版本）。清单不含完整 URL、PIN 或签名密码。
9. 仅通过 Actions artifact 交付，拟保留 14 天；清理临时密钥、签名属性与生成配置，失败时同样清理。不要将这些文件收入依赖缓存。

实施时固定第三方 Action 至审核过的提交，并单独验证 GitHub 表达式、YAML、权限和 artifact 名称。当前没有选择 Action 提交号，不能将本文当成已通过验证的 workflow。

## 完成条件

只有在手动 Action 真正跑通、签名验证通过，且该 artifact 在全新 Android 10 设备上无需填写配置直接进入预置 URL 后，才能把 README 的状态更新为“可用”。编译成功不足以证明首次启动功能完成。
