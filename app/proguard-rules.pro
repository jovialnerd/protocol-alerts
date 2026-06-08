# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.protocol.alerts.**$$serializer { *; }
-keepclassmembers class com.protocol.alerts.** { *** Companion; }
-keepclasseswithmembers class com.protocol.alerts.** { kotlinx.serialization.KSerializer serializer(...); }
