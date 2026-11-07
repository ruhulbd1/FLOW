# Project specific ProGuard / R8 rules for FLOW Android app

# Preserve Data Models, Entities, and DTOs
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Preserve Moshi JSON serialization and Codegen
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }

# Preserve Retrofit and OkHttp
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Preserve Coil Image Loading and custom ImageLoaderFactory
-keep class coil.** { *; }
-dontwarn coil.**
-keep class com.example.FlowApplication { *; }

# Preserve Coroutines, Flow, and Lifecycle
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**

# Preserve XmlPullParser for RSS Atom parsing
-keep class org.xmlpull.v1.** { *; }
-dontwarn org.xmlpull.v1.**

# Preserve Android Jetpack DataStore Preferences
-keep class androidx.datastore.** { *; }
