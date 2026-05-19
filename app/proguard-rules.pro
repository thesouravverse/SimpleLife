# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.thesouravverse.simplelife.**$$serializer { *; }
-keepclassmembers class com.thesouravverse.simplelife.** {
    *** Companion;
}
-keepclasseswithmembers class com.thesouravverse.simplelife.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.* class * { *; }

# WorkManager
-keep class androidx.work.** { *; }
