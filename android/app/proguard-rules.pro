# 项目专用 R8 规则。依赖自带的 consumer rules 继续由 Gradle 合并。

# Keep host obfuscated names separate from the upstream engine's already-obfuscated DEX.
-repackageclasses com.wifi504.easykiosk.internal

# The engine is merged after R8; preserve the shared Kotlin binary API it can call.
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep interface kotlinx.** { *; }
