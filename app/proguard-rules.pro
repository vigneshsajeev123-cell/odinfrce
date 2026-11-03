# ProGuard rules for Memory Universe (Offline 3D Android Application)

# Keep Room entities and DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Coil image loader
-keep class coil.** { *; }

# Keep Data classes for database persistence
-keep class com.memoryuniverse.core.database.entity.** { *; }
-keep class com.memoryuniverse.core.database.dao.** { *; }
