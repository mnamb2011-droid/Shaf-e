package com.shafii.encyclopedia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Dedicated Room database for user-generated data (bookmarks, notes, history).
 * Kept separate from the encyclopedia SQLite content pack so importing updated
 * offline content packs never erases the user's personal notes or favorites.
 */
@Database(
    entities = [
        BookmarkEntity::class,
        PersonalNoteEntity::class,
        ReadingHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UserDataDatabase : RoomDatabase() {
    abstract fun userDataDao(): UserDataDao

    companion object {
        @Volatile
        private var INSTANCE: UserDataDatabase? = null

        fun getInstance(context: Context): UserDataDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserDataDatabase::class.java,
                    "shafii_user_data.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
