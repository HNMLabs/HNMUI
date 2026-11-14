# Kotlinx Serialization & R8 tam koruma kuralları
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

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
-keep @kotlinx.serialization.Serializable class com.hnmlabs.hnmui.model.** { *; }
-keepclassmembers enum com.hnmlabs.hnmui.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

