# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ===== Keep4 R8 规则 =====

# Shizuku 通过 Binder/反射工作，保留其 API 入口（避免被 R8 裁剪导致 Shizuku 不可用）
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# 应用组件兜底（清单中声明的组件 AGP 会自动保留，这里显式声明更稳妥）
-keep class com.aya.keep4.MainActivity { *; }
-keep class com.aya.keep4.service.** { *; }
-keep class com.aya.keep4.receiver.** { *; }

# 通知渠道/无障碍配置等通过 XML 引用，保留资源引用（R8 已处理，无需额外配置）