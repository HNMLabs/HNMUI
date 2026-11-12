# Kotlinx Serialization kuralları
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hnmlabs.hnmui.**$$serializer { *; }
-keepclassmembers class com.hnmlabs.hnmui.** {
    *** Companion;
}
-keepclasseswithmembers class com.hnmlabs.hnmui.** {
    kotlinx.serialization.KSerializer serializer(...);
}
