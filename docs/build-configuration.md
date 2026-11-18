# 构建变量契约

状态：拟实施的接口。`.env.example` 仅提供示例；当前无 dotenv 加载器、配置生成器或 APK 编译 Action。

## 输入来源

未来手动 Action 的非空输入覆盖同名 Repository Variables，再使用下表默认值；必填项没有有效值时构建失败。敏感输入来自 Repository Secrets，不放到普通 Variables 或 workflow_dispatch 输入。生成器只接收进程环境变量；本地调试可自行导出，不能执行不可信 `.env` 为 shell 代码。

| 环境变量 | 类型/默认值 | 校验与用途 |
| --- | --- | --- |
| `KIOSK_START_URL` | 必填字符串 | 完整 http/https URL；不接受空白、控制字符、userinfo 或占位域名；允许内网域名/IP。查询参数可含 `&`，必须由 JSON 序列化而非文本替换注入 |
| `KIOSK_ADMIN_PIN` | 必填 Secret | 4～6 位数字；禁止固定通用 PIN 回退；不得输出到日志或配置清单 |
| `KIOSK_AUTO_START` | boolean，`true` | 开机自动启动意图，不替代 ROM 支持 |
| `KIOSK_LOCK_MODE` | boolean，`true` | 启动时启用展台锁定；完整保护需 Device Owner |
| `KIOSK_KEEP_SCREEN_ON` | boolean，`true` | 保持屏幕常亮 |
| `KIOSK_ORIENTATION` | enum，`portrait` | `portrait`、`landscape`、`auto` |
| `KIOSK_AUTO_RELOAD` | boolean，`true` | 网页加载错误自动重试；不代表能检测业务停滞 |
| `KIOSK_CONFIG_REVISION` | string，`1` | 1～64 个字母、数字、点、横线或下划线，记录预置配置版本；不是强制覆盖设备配置的开关 |
| `ANDROID_VERSION_NAME` | 必填字符串 | 1～64 个字母、数字、点、横线或下划线，例如 `0.1.0-kiosk.1` |
| `ANDROID_VERSION_CODE` | 必填正整数 | 1～2100000000；每个可升级安装包必须大于上一版，禁止覆盖为同版本号 |
| `ANDROID_ABIS` | CSV，`arm64-v8a,armeabi-v7a,x86_64` | 只允许这三个 ABI，去重且不能为空；x86_64 用于模拟器，实机按 CPU 选择 |

boolean 仅接受小写字符串 `true`、`false`。不接受 `yes`、`1`、空字符串或大小写变体。示例文件中的 URL 与 PIN 是占位内容，不允许用于正式产物。

## 固定产品策略

默认显示模式固定为 `webview`；全屏、隐藏系统导航/状态栏、禁用 Cloud 与其他显示模式由产品策略统一控制，不向构建者暴露上游全部开关。系统栏能否完全限制仍取决于设备管理权限。

下一阶段计划将 Android 包名改为 `com.wifi504.easykiosk`，显示名改为 `Easy Kiosk`，不把包名作为随意变化的环境变量；这需要同步 Kotlin 包、Manifest 组件、桥接与脚本引用。当前包名仍为 `com.freekiosk`，不可使用未来包名执行部署命令。

## 生成与落盘设计

拟新增 `scripts/generate-kiosk-config.mjs`，使用标准库校验后一次性生成临时目录中的配置、原生资源和版本参数。最终目标文件路径要在实现时确定并写入同一契约，不能让 JS 与 Kotlin 各自定义默认值。

配置生效顺序为：设备保存值 > 当前 APK 预置值 > 明确声明的产品默认值。第一次启动必须在 JS 的批量读取与 Kotlin 原生启动策略之前完成初始化；既有 ADB/设置写入继续作为管理员修改。跨层初始化应有版本标记、并发保护、失败重试与迁移策略。

URL、PIN 即使来自 Secrets，打入 APK 后也不能视为无法提取的秘密。不要嵌入服务器管理密钥、长期账户密码或业务访问令牌；初始 PIN 只作现场维护入口，应支持修改并按上游安全存储方式保存。
