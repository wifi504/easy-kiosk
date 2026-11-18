# 脚本目录

此目录仍保留上游设备部署工具，本次未改脚本行为。它们不负责读取根目录 `.env.example`，也不能替代未来的预置配置生成器。

| 路径 | 当前用途 |
| --- | --- |
| `deploy_mac.zsh` | 上游 macOS 交互部署工具 |
| `set-device-owner.ps1` | 检查 Device Owner 条件并执行设备管理设置 |
| `install-boot-autostart.ps1` | 为特定 ROM 安装系统级开机启动补救文件 |
| `device/` | 上述工具使用的 Android 系统文件 |

工具当前仍使用上游包名。执行前查看参数与脚本源代码，指定正确设备；`-Fix` 或系统启动补救涉及 root、修改 `/system` 和重启，不是普通展台安装必需步骤。不要为验证文档而运行它们。

未来拟新增的配置生成器及构建步骤见 [构建变量](../docs/build-configuration.md) 和 [Actions 设计](../docs/github-actions.md)。
