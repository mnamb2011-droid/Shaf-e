package com.shafii.encyclopedia.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val issueId: Int,
    val issueTitle: String,
    val bookTitle: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "personal_notes")
data class PersonalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val issueId: Int,
    val issueTitle: String,
    val noteText: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey val issueId: Int,
    val issueTitle: String,
    val bookTitle: String,
    val lastVisitedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val queryText: String,
    val lastSearchedMillis: Long = System.currentTimeMillis()
)
