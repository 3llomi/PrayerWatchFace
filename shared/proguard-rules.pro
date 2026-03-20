#Shared
-keep class com.devlomi.shared.** { *; }
-keepclassmembers class com.devlomi.shared.** { *; }
#DataStore
-keep class androidx.datastore.*.** {*;}
# Keep Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }