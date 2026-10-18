# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
