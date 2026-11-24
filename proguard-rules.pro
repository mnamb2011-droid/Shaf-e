# Keep Application, Activities, and AndroidViewModel subclasses for reflection
-keep class com.shafii.encyclopedia.ShafiiApp { *; }
-keep class com.shafii.encyclopedia.MainActivity { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep Room databases, generated _Impl classes, DAOs, and entities
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep Kotlin metadata & coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
