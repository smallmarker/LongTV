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

# Preserve stack traces in release builds.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Gson uses field names and generic signatures reflectively.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep JSON model fields stable for reflective parsing.
-keep class com.loong.longtv.data.bean.** { *; }
-keep class com.loong.longtv.data.repository.ApiResult { *; }
-keep class com.loong.longtv.spider.data.bean.** { *; }

# Keep CatVod/Spider integration classes that are instantiated across module boundaries.
-keep class com.loong.longtv.core.spider.** { *; }
-keep class com.github.catvod.crawler.** { *; }
# Keep all app-bundled Spider implementations (loaded reflectively by class name).
-keep class com.github.catvod.spider.** { *; }

# Hilt / Dagger generated graphs rely on annotation metadata.
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManagerHolder { *; }
-keep class **_Factory { *; }
-keep class **_HiltModules_* { *; }
-keep class **_MembersInjector { *; }
-dontwarn dagger.hilt.internal.**
-dontwarn javax.inject.**

# Room databases, entities and DAO metadata are discovered from annotations and generated code.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Media3 / ExoPlayer and AndroidX generally ship consumer rules, but keep custom player-facing code.
-keep class com.loong.longtv.ui.shared.detail.PlayerUi { *; }
-dontwarn androidx.media3.**
-dontwarn org.checkerframework.**

# OkHttp / Retrofit / jsoup warnings are safe to suppress in app shrinking.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.jsoup.**

# Keep Kotlin metadata signatures used by reflection-heavy libraries.
-keep class kotlin.Metadata { *; }
