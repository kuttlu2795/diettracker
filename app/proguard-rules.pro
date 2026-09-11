# Keep Room entities and Retrofit/Gson models
-keep class com.dietary.tracker.data.entities.** { *; }
-keep class com.dietary.tracker.network.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okio.**
-dontwarn retrofit2.**
