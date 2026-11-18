# 项目专用 R8 规则。依赖自带的 consumer rules 继续由 Gradle 合并。

# Keep host obfuscated names separate from the upstream engine's already-obfuscated DEX.
-repackageclasses com.wifi504.easykiosk.internal

# The engine is merged after R8; preserve shared runtime names and binary APIs.
# Otherwise R8 can rename/remove AndroidX code referenced only by the later engine.
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class android.support.** { *; }
-keep interface android.support.** { *; }
-keep interface com.google.common.util.concurrent.ListenableFuture { *; }
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep interface kotlinx.** { *; }

# AndroidX uses this public reflection boundary, replaced only after host-API auditing.
-keep class org.chromium.support_lib_boundary.** { *; }
-keep interface org.chromium.support_lib_boundary.** { *; }
