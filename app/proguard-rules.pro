# Keep everything from the androidx libraries
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep everything from the OpenCV SDK
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Keep everything from ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep everything from CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep everything from Document Scanner library
-keep class com.document.scanner.** { *; }
-dontwarn com.document.scanner.**

# Keep everything from material design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep everything from androidx activity and appcompat
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**

-keep class androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**

# Keep everything from androidx.constraintlayout
-keep class androidx.constraintlayout.** { *; }
-dontwarn androidx.constraintlayout.**

# Keep everything from androidx.core
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# Keep all classes from libs directory
-keep class ** { *; }
-dontwarn **

# Keep the ML Kit Document Scanner dependency
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
