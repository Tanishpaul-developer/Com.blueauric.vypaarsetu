# ==============================================================================
# VyaparSetu - Production ProGuard / R8 Optimization & Obfuscation Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. General Android & Kotlin Configuration
# ------------------------------------------------------------------------------
# Retain stack traces with line numbers for production crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations, generic signatures, inner classes, and exceptions
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 2. Android Jetpack Room Database
# ------------------------------------------------------------------------------
# Preserve Room Database subclasses and generated implementations
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
    void clearAllTables();
}

# Preserve Room DAOs and Entities
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabaseComponent { *; }
-dontwarn androidx.room.paging.**

# Keep all VyaparSetu local database entities and types
-keep class com.example.data.local.** { *; }

# ------------------------------------------------------------------------------
# 3. Retrofit, OkHttp, and Moshi (Networking & Serialization)
# ------------------------------------------------------------------------------
# Retrofit reflection on service interfaces and HTTP annotations
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Moshi JSON serialization
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class **JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}

# ------------------------------------------------------------------------------
# 4. Google Firebase (Authentication, Cloud Firestore, App Check)
# ------------------------------------------------------------------------------
-keepattributes *Annotation*
-dontwarn com.google.firebase.**

# Firestore POJO serialization fields and getters/setters
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Preserve Firebase Authentication and Credential Manager classes
-keep class com.google.firebase.auth.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ------------------------------------------------------------------------------
# 5. ZXing (Zebra Crossing UPI QR Code Generation & Decoding)
# ------------------------------------------------------------------------------
-keep class com.google.zxing.** { *; }
-keepclassmembers class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ------------------------------------------------------------------------------
# 6. Android WorkManager (Background Sync)
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.service.SyncWorker { *; }
-keep class com.example.service.BackgroundSyncWorker { *; }
-keep class com.example.service.DailyClosingWorker { *; }

# ------------------------------------------------------------------------------
# 7. VyaparSetu Domain & Model Classes (Reflection & State Preservation)
# ------------------------------------------------------------------------------
-keep class com.example.data.model.** { *; }
-keep class com.example.domain.** { *; }
-keep class com.example.service.InvoiceGenerator$* { *; }
-keep class com.example.service.ThermalPrinterManager$* { *; }
