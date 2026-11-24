package com.shafii.encyclopedia.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY createdAtMillis DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE issueId = :issueId)")
    fun observeIsBookmarked(issueId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE issueId = :issueId")
    suspend fun deleteBookmark(issueId: Int)

    // Personal Notes
    @Query("SELECT * FROM personal_notes ORDER BY updatedAtMillis DESC")
    fun observeAllNotes(): Flow<List<PersonalNoteEntity>>

    @Query("SELECT * FROM personal_notes WHERE issueId = :issueId ORDER BY updatedAtMillis DESC")
    fun observeNotesForIssue(issueId: Int): Flow<List<PersonalNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: PersonalNoteEntity)

    @Query("DELETE FROM personal_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int)

    // Reading History
    @Query("SELECT * FROM reading_history ORDER BY lastVisitedMillis DESC LIMIT 20")
    fun observeRecentHistory(): Flow<List<ReadingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordVisit(entry: ReadingHistoryEntity)

    @Query("DELETE FROM reading_history")
    suspend fun clearHistory()

    // Search History
    @Query("SELECT * FROM search_history ORDER BY lastSearchedMillis DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordSearch(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
