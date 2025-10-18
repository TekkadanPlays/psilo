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

# Keep custom icons
-keep class com.example.views.ui.icons.** { *; }
-keep class androidx.compose.material.icons.Icons$Outlined { *; }

# Keep Compose Material Icons
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.ui.graphics.vector.** { *; }

# Optimization flags for better performance
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep data classes with @Immutable annotation
-keep @androidx.compose.runtime.Immutable class * { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Ktor optimizations
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.views.**$$serializer { *; }
-keepclassmembers class com.example.views.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.views.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Remove logging in release builds for better performance
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Compose specific optimizations
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }