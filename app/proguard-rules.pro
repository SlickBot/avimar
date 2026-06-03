# Keep kotlinx.serialization annotations and serializer companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class eu.slickbot.avimar.**$$serializer { *; }
-keepclassmembers class eu.slickbot.avimar.** {
    *** Companion;
}
-keepclasseswithmembers class eu.slickbot.avimar.** {
    kotlinx.serialization.KSerializer serializer(...);
}
