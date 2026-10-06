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

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Gson serialization rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Domain and Data Models (prevent R8 from stripping properties or changing names used by SQLite or reflection)
-keep class com.gtc.app_finance.domain.model.** { *; }
-keep class com.gtc.app_finance.data.entity.** { *; }
-keep class com.gtc.app_finance.data.database.** { *; }

# Koin Dependency Injection
-keep class io.insert.koin.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**