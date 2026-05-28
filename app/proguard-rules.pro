# Keep kotlinx.serialization annotations and serializer companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class eu.slickbot.airnav.**$$serializer { *; }
-keepclassmembers class eu.slickbot.airnav.** {
    *** Companion;
}
-keepclasseswithmembers class eu.slickbot.airnav.** {
    kotlinx.serialization.KSerializer serializer(...);
}
