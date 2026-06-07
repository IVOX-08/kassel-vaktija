# ProGuard/R8 rules. Minification is disabled until Phase 9; rules collected here as we go.

# kotlinx.serialization — keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class de.igbdsandzakkassel.vaktija.**$$serializer { *; }
-keepclassmembers class de.igbdsandzakkassel.vaktija.** {
    *** Companion;
}
-keepclasseswithmembers class de.igbdsandzakkassel.vaktija.** {
    kotlinx.serialization.KSerializer serializer(...);
}
