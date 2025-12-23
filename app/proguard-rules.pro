# Keep the complete app package to prevent class not found exceptions
-keep class com.prayag.omr_scan_aar.** { *; }

# Specifically keep your main activity and any related components
-keep public class com.prayag.omr_scan_aar.presentation.main.MainActivity
-keepclassmembers class com.prayag.omr_scan_aar.presentation.main.MainActivity {
    public <init>(...);
}

# Keep all activities from manifest
-keep public class com.prayag.omr_scan_aar.presentation.omrresult.ResultActivity
-keep public class com.prayag.omr_scan_aar.presentation.scanner.DocumentScannerActivity
-keep public class com.prayag.omr_scan_aar.MyApplication

# Keep all activities, fragments, services, etc.
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment

# Rules for Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    <init> (...);
}
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.koin.core.annotation.** <methods>;
    @org.koin.core.annotation.** <fields>;
}
# Rules for Kotlin reflection
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.coroutines.** {
    public <methods>;
}
-dontwarn kotlin.reflect.jvm.internal.**

## Rules for serializable classes
#-keepclassmembers class * implements java.io.Serializable {
#    static final long serialVersionUID;
#    private static final java.io.ObjectStreamField[] serialPersistentFields;
#    private void writeObject(java.io.ObjectOutputStream);
#    private void readObject(java.io.ObjectInputStream);
#    java.lang.Object writeReplace();
#    java.lang.Object readResolve();
#}
