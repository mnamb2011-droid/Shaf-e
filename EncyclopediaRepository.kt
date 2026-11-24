package com.shafii.encyclopedia.data.repository

import android.net.Uri
import com.shafii.encyclopedia.data.db.*
import com.shafii.encyclopedia.util.DatabasePackManager
import com.shafii.encyclopedia.util.IntegrityReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

enum class SearchScope(val labelAr: String) {
    ALL("شامل"),
    TITLE("المسألة"),
    SCHOLAR("العالم"),
    REFERENCE("المصدر"),
    CHAPTER("الباب")
}

enum class ComparisonFilter(val labelAr: String) {
    ALL("جميع الأقوال"),
    QADIM_JADID("القديم والجديد فقط"),
    EARLY_AND_INTERMEDIATE("المتقدمون والمتوسطون"),
    LATER_AND_SHAYKHAN("الشيخان والمتأخرون"),
    CONTEMPORARY("البحوث المعاصرة")
}

data class BookSummary(
    val id: Int,
    val title: String,
    val shortTitle: String,
    val sortOrder: Int,
    val description: String,
    val chaptersCount: Int,
    val issuesCount: Int
)

data class ChapterWithIssues(
    val id: Int,
    val bookId: Int,
    val title: String,
    val description: String,
    val issues: List<IssueSummary>
)

data class IssueSummary(
    val id: Int,
    val chapterId: Int,
    val title: String,
    val shortRuling: String,
    val fiqhClassification: String,
    val bookTitle: String,
    val chapterTitle: String,
    val verificationLabel: String,
    val verificationCode: String,
    val stageLevel: Int
)

data class IssueRecord(
    val id: Int,
    val chapterId: Int,
    val title: String,
    val fiqhClassification: String,
    val issueFraming: String,
    val shortRuling: String,
    val mutamadSummary: String,
    val mutamadEarly: String,
    val mutamadLate: String,
    val rafiiNawawiAgreement: String,
    val qadimSummary: String,
    val jadidSummary: String,
    val ashabSummary: String,
    val khilafAwjuhSummary: String,
    val historicalEvolution: String,
    val evidenceDiscussion: String,
    val reasonForDisagreement: String,
    val comparativeSummary: String,
    val verificationStatusCode: String,
    val bookTitle: String,
    val chapterTitle: String
)

data class OpinionSourceDetail(
    val id: Int,
    val referenceId: Int,
    val referenceTitle: String,
    val authorName: String,
    val volume: String?,
    val page: String?,
    val sectionLocation: String,
    val citationType: String,
    val checkedAgainstOriginal: Boolean,
    val verificationLabel: String,
    val notes: String,
    val editionPublisher: String?
)

data class QuotationDetail(
    val id: Int,
    val referenceTitle: String,
    val volume: String?,
    val page: String?,
    val quotationText: String,
    val isExactVerbatim: Boolean,
    val checkedAgainstOriginal: Boolean,
    val notes: String
)

data class OpinionDetail(
    val id: Int,
    val issueId: Int,
    val scholarId: Int,
    val scholarCommonName: String,
    val scholarFullName: String,
    val periodCode: String,
    val periodName: String,
    val periodHijriRange: String,
    val opinionType: String,
    val qadimJadidClass: String,
    val isMutamad: Boolean,
    val isContemporary: Boolean,
    val opinionText: String,
    val reasoningSummary: String,
    val attributionNotes: String,
    val verificationCode: String,
    val verificationLabel: String,
    val sources: List<OpinionSourceDetail>,
    val quotations: List<QuotationDetail>
)

data class EvidenceDetail(
    val id: Int,
    val opinionId: Int?,
    val evidenceType: String,
    val textContent: String,
    val sourceReference: String,
    val authenticityNote: String
)

data class RelatedIssueItem(
    val issueId: Int,
    val title: String,
    val relationType: String,
    val notes: String
)

data class IssueFullDetail(
    val issue: IssueRecord,
    val verificationLabel: String,
    val opinions: List<OpinionDetail>,
    val evidences: List<EvidenceDetail>,
    val relatedIssues: List<RelatedIssueItem>
)

data class ScholarProfile(
    val id: Int,
    val fullName: String,
    val commonName: String,
    val birthYearHijri: String,
    val deathYearHijri: String,
    val periodName: String,
    val periodHijriRange: String,
    val teachers: String,
    val students: String,
    val contributions: String,
    val bibliography: String,
    val notes: String,
    val documentedIssues: List<Pair<Int, String>>
)

data class EditionItem(
    val publisher: String,
    val editor: String,
    val publicationYear: String,
    val volumesCount: String,
    val notes: String
)

data class ManuscriptItem(
    val libraryName: String,
    val catalogNumber: String,
    val copyDate: String,
    val completenessNotes: String,
    val verificationLabel: String
)

data class ReferenceProfile(
    val id: Int,
    val title: String,
    val authorName: String,
    val authorshipDate: String,
    val subject: String,
    val bookType: String,
    val printStatus: String,
    val manuscriptStatus: String,
    val notes: String,
    val editions: List<EditionItem>,
    val manuscripts: List<ManuscriptItem>,
    val citedInIssues: List<Pair<Int, String>>
)

data class TerminologyItem(
    val id: Int,
    val term: String,
    val definition: String,
    val synonyms: String,
    val usageContext: String,
    val sourceNote: String
)

class EncyclopediaRepository(
    private val store: EncyclopediaSqliteStore,
    private val userDao: UserDataDao,
    private val packManager: DatabasePackManager
) {
    suspend fun getMetadata(): Map<String, String> = withContext(Dispatchers.IO) { store.getMetadata() }
    suspend fun getBooks(): List<BookSummary> = withContext(Dispatchers.IO) { store.getAllBooksWithCounts() }
    suspend fun getChaptersForBook(bookId: Int): List<ChapterWithIssues> = withContext(Dispatchers.IO) { store.getChaptersForBook(bookId) }
    suspend fun getAllIssues(): List<IssueSummary> = withContext(Dispatchers.IO) { store.getAllIssues() }
    suspend fun searchIssues(query: String, scope: SearchScope): List<IssueSummary> = withContext(Dispatchers.IO) { store.searchIssues(query, scope) }
    suspend fun getIssueDetail(issueId: Int): IssueFullDetail? = withContext(Dispatchers.IO) { store.getIssueFullDetail(issueId) }
    suspend fun getScholars(): List<ScholarProfile> = withContext(Dispatchers.IO) { store.getAllScholars() }
    suspend fun getReferences(): List<ReferenceProfile> = withContext(Dispatchers.IO) { store.getAllReferences() }
    suspend fun getTerminology(): List<TerminologyItem> = withContext(Dispatchers.IO) { store.getAllTerminology() }

    // User data
    fun observeBookmarks(): Flow<List<BookmarkEntity>> = userDao.observeBookmarks()
    fun observeIsBookmarked(issueId: Int): Flow<Boolean> = userDao.observeIsBookmarked(issueId)
    fun observeAllNotes(): Flow<List<PersonalNoteEntity>> = userDao.observeAllNotes()
    fun observeNotesForIssue(issueId: Int): Flow<List<PersonalNoteEntity>> = userDao.observeNotesForIssue(issueId)
    fun observeRecentHistory(): Flow<List<ReadingHistoryEntity>> = userDao.observeRecentHistory()
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>> = userDao.observeSearchHistory()

    suspend fun toggleBookmark(issueId: Int, title: String, bookTitle: String, currentlyBookmarked: Boolean) {
        if (currentlyBookmarked) userDao.deleteBookmark(issueId)
        else userDao.insertBookmark(BookmarkEntity(issueId, title, bookTitle))
    }

    suspend fun saveNote(issueId: Int, issueTitle: String, text: String) {
        if (text.isNotBlank()) {
            userDao.upsertNote(PersonalNoteEntity(issueId = issueId, issueTitle = issueTitle, noteText = text.trim()))
        }
    }

    suspend fun deleteNote(noteId: Int) = userDao.deleteNote(noteId)

    suspend fun recordVisit(issueId: Int, title: String, bookTitle: String) {
        userDao.recordVisit(ReadingHistoryEntity(issueId, title, bookTitle))
    }

    suspend fun recordSearch(query: String) {
        if (query.trim().length >= 2) {
            userDao.recordSearch(SearchHistoryEntity(query.trim()))
        }
    }

    suspend fun clearSearchHistory() = userDao.clearSearchHistory()

    // Pack management & integrity
    suspend fun runIntegrityCheck(): IntegrityReport = withContext(Dispatchers.IO) {
        packManager.ensureStarterDatabaseReady()
        packManager.runIntegrityCheck()
    }

    suspend fun importDatabasePack(uri: Uri): Result<IntegrityReport> = withContext(Dispatchers.IO) {
        val result = packManager.importPackFromUri(uri)
        if (result.isSuccess) {
            store.reloadDatabase()
        }
        result
    }

    suspend fun exportBackup(): Result<File> = withContext(Dispatchers.IO) {
        packManager.exportCompressedBackup()
    }

    suspend fun resetToStarterDatabase(): Result<IntegrityReport> = withContext(Dispatchers.IO) {
        val result = packManager.resetToStarterDatabase()
        if (result.isSuccess) {
            store.reloadDatabase()
        }
        result
    }
}
