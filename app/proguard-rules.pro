# ─── NexChat ProGuard Rules ────────────────────────────────────────────────

# Keep Moshi models
-keep class com.nexchat.data.remote.dto.** { *; }
-keep class com.nexchat.domain.model.** { *; }
-keepclassmembers class com.nexchat.data.remote.dto.** { *; }
-keepclassmembers class com.nexchat.domain.model.** { *; }

# Moshi codegen
-keep class com.squareup.moshi.** { *; }
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Room entities
-keep class com.nexchat.data.local.entity.** { *; }
-keepclassmembers class com.nexchat.data.local.entity.** { *; }

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class io.socket.engineio.** { *; }
-keep class io.socket.client.** { *; }
-keep class io.socket.parser.** { *; }
-keep class io.socket.thread.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.nexchat.**$$serializer { *; }
-keepclassmembers class com.nexchat.** {
    *** Companion;
}
-keepclasseswithmembers class com.nexchat.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# LiveKit
-keep class io.livekit.** { *; }
-dontwarn io.livekit.**
-keep class livekit.** { *; }
-dontwarn livekit.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Paging
-keep class androidx.paging.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
