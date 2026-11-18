# 中文文档索引

本文档描述 easy-kiosk 的改造准备，不是已完成的产品说明。

| 文档 | 内容 |
| --- | --- |
| [远程功能裁剪](remote-removal.md) | 此轮删除范围、共享能力和 Actions 验证 |
| [改造范围](product-scope.md) | 单网页展台、首次启动、可修改配置及边界 |
| [构建变量](build-configuration.md) | 变量、类型、校验、默认值与配置优先级 |
| [Actions 编译设计](github-actions.md) | 触发、签名、校验、产物与失败处理 |
| [实施计划](implementation-plan.md) | 当前源码位置、后续任务和完成条件 |
| [部署与验收](deployment-and-acceptance.md) | Android 10 设备、Device Owner、故障测试 |
| [开发环境](development.md) | 当前工具链与本地检查 |
| [来源与许可](attribution.md) | 原作者、MIT 与第三方声明 |

下一阶段顺序：统一配置入口 → 编译时生成配置 → 首次启动应用配置 → 简化展台界面 → 签名 APK 流程 → 实机验收。每一步都需要独立验证，详见实施计划。
