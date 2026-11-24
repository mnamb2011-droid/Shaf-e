# Keep Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlin metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
