# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**

# ===== Kotlin Coroutines =====
-keepclassmembers class kotlinx.coroutines.** { *; }

# ===== Room Database =====
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ===== JSON =====
-keep class org.json.** { *; }

# ===== App Entities & DAOs =====
-keep class com.usmandiscountstore.app.data.local.entity.** { *; }
-keep class com.usmandiscountstore.app.data.local.dao.** { *; }

# ===== TensorFlow Lite =====
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ===== ML Kit =====
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ===== Retain signatures =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
