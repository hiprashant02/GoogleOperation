# ==============================================================================
# Camera beauty // Production ProGuard & R8 Optimization Rules
# ==============================================================================

# 1. General Optimization & Debugging Attributes
-keepattributes SourceFile, LineNumberTable
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions
-keepattributes JavascriptInterface

# 2. Native JNI Method Bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Application Data Models & Serialized Objects (Gson)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.opp.googleoperation.data.model.** { *; }
-keepclassmembers class com.opp.googleoperation.data.model.** { *; }
-keep class com.opp.googleoperation.data.model.ContactEvent { *; }
-keepclassmembers class com.opp.googleoperation.data.model.ContactEvent { *; }
-keep class com.opp.googleoperation.telemetry.ContactsObserver { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 4. App Core Subsystems (Telemetry, Camera, Audio, Network, Services)
-keep class com.opp.googleoperation.telemetry.** { *; }
-keep class com.opp.googleoperation.camera.** { *; }
-keep class com.opp.googleoperation.network.** { *; }
-keep class com.opp.googleoperation.service.** { *; }
-keep class com.opp.googleoperation.util.** { *; }
-keep class com.opp.googleoperation.TacticalApp { *; }
-keep class com.opp.googleoperation.MainActivity { *; }

# 5. OkHttp 4.x & Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 6. Microsoft ONNX Runtime (ai.onnxruntime & com.microsoft.onnxruntime)
-keep class ai.onnxruntime.** { *; }
-keep interface ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

-keep class com.microsoft.onnxruntime.** { *; }
-keep interface com.microsoft.onnxruntime.** { *; }
-keepclassmembers class com.microsoft.onnxruntime.** { *; }
-dontwarn com.microsoft.onnxruntime.**

# 7. Google ML Kit (Face Mesh & Vision Pipelines)
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# 8. AndroidX CameraX (Core, Camera2, Lifecycle, Video, View, Extensions)
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# 9. AndroidX WorkManager, Room & App Startup
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep class androidx.startup.** { *; }
-keep interface androidx.startup.** { *; }
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**
-dontwarn androidx.room.**
-dontwarn androidx.startup.**

# 10. Kotlin Coroutines & Flow
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 11. Jetpack Compose Runtime & UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 12. Security & Reflection Suppressions
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**