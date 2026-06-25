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

# Preserve useful stack traces in release builds.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Gson uses field names, generic signatures and no-arg/default constructors reflectively.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep JSON/config models stable for reflective parsing.
# SourceRepository parses Spider JSON into these Kotlin data classes with Gson.
-keep class com.loong.longtv.engine.source.model.** { *; }
-keep class com.loong.longtv.data.bean.** { *; }

# Keep CatVod/Spider integration classes. SpiderLoader instantiates bundled spiders by
# class name from api.json, for example csp_AppRJ -> com.github.catvod.spider.AppRJ.
-keep class com.loong.longtv.engine.** { *; }
-keep class com.github.catvod.crawler.** { *; }
-keep class com.github.catvod.spider.** { *; }
-keep class com.github.catvod.net.** { *; }
-keep class com.github.catvod.utils.** { *; }
-keep class com.dnsoverhttps.** { *; }

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
-keep class com.loong.player.** { *; }
-dontwarn androidx.media3.**
-dontwarn org.checkerframework.**

# OkHttp / Retrofit / jsoup warnings are safe to suppress in app shrinking.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.jsoup.**
-dontwarn javax.annotation.**

# Keep Kotlin metadata signatures used by reflection-heavy libraries.
-keep class kotlin.Metadata { *; }
