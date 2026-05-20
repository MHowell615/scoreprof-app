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

# Prevent Volley and JSON classes from being scrambled
-keep class com.android.volley.** { *; }
-keep class org.json.** { *; }

# 1. Protect Kotlinx Serialization (Crucial for ScoreProf's server communication)
-keepattributes *Annotation*, Signature, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# 2. Protect Room Entities
# This ensures Room can still map database columns to your Kotlin objects
-keep @androidx.room.Entity class * { *; }

# 3. Protect Hilt
# While Hilt is usually good, this prevents its generated factories from being stripped
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep your data models so the API can read them
-keep class cloud.scoreprof.app.data.** { *; }