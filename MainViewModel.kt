package com.shafii.encyclopedia.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shafii.encyclopedia.ShafiiApp
import com.shafii.encyclopedia.data.db.BookmarkEntity
import com.shafii.encyclopedia.data.db.PersonalNoteEntity
import com.shafii.encyclopedia.data.db.ReadingHistoryEntity
import com.shafii.encyclopedia.data.db.SearchHistoryEntity
import com.shafii.encyclopedia.data.repository.*
import com.shafii.encyclopedia.ui.theme.ReaderTypographyConfig
import com.shafii.encyclopedia.util.IntegrityReport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: EncyclopediaRepository = (application as ShafiiApp).repository
    private val prefs = application.getSharedPreferences("shafii_ui_prefs", Context.MODE_PRIVATE)

    // Theme & Typography Settings
    private val _darkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _readerConfig = MutableStateFlow(
        ReaderTypographyConfig(
            fontScale = prefs.getFloat("font_scale", 1.0f),
            lineSpacingMultiplier = prefs.getFloat("line_spacing", 1.55f)
        )
    )
    val readerConfig: StateFlow<ReaderTypographyConfig> = _readerConfig.asStateFlow()

    // Core Encyclopedia State
    private val _metadata = MutableStateFlow<Map<String, String>>(emptyMap())
    val metadata: StateFlow<Map<String, String>> = _metadata.asStateFlow()

    private val _books = MutableStateFlow<List<BookSummary>>(emptyList())
    val books: StateFlow<List<BookSummary>> = _books.asStateFlow()

    private val _chaptersByBook = MutableStateFlow<Map<Int, List<ChapterWithIssues>>>(emptyMap())
    val chaptersByBook: StateFlow<Map<Int, List<ChapterWithIssues>>> = _chaptersByBook.asStateFlow()

    private val _allIssues = MutableStateFlow<List<IssueSummary>>(emptyList())
    val allIssues: StateFlow<List<IssueSummary>> = _allIssues.asStateFlow()

    private val _scholars = MutableStateFlow<List<ScholarProfile>>(emptyList())
    val scholars: StateFlow<List<ScholarProfile>> = _scholars.asStateFlow()

    private val _references = MutableStateFlow<List<ReferenceProfile>>(emptyList())
    val references: StateFlow<List<ReferenceProfile>> = _references.asStateFlow()

    private val _terminology = MutableStateFlow<List<TerminologyItem>>(emptyList())
    val terminology: StateFlow<List<TerminologyItem>> = _terminology.asStateFlow()

    // Selected Issue & Comparison State
    private val _selectedIssue = MutableStateFlow<IssueFullDetail?>(null)
    val selectedIssue: StateFlow<IssueFullDetail?> = _selectedIssue.asStateFlow()

    private val _comparisonIssue = MutableStateFlow<IssueFullDetail?>(null)
    val comparisonIssue: StateFlow<IssueFullDetail?> = _comparisonIssue.asStateFlow()

    private val _comparisonFilter = MutableStateFlow(ComparisonFilter.ALL)
    val comparisonFilter: StateFlow<ComparisonFilter> = _comparisonFilter.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchScope = MutableStateFlow(SearchScope.ALL)
    val searchScope: StateFlow<SearchScope> = _searchScope.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IssueSummary>>(emptyList())
    val searchResults: StateFlow<List<IssueSummary>> = _searchResults.asStateFlow()

    // Integrity & Pack Status
    private val _integrityReport = MutableStateFlow<IntegrityReport?>(null)
    val integrityReport: StateFlow<IntegrityReport?> = _integrityReport.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Reactive User Data from Room
    val bookmarks: StateFlow<List<BookmarkEntity>> = repo.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<PersonalNoteEntity>> = repo.observeAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<ReadingHistoryEntity>> = repo.observeRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repo.observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshAllEncyclopediaData()
    }

    fun refreshAllEncyclopediaData() {
        viewModelScope.launch {
            try {
                _integrityReport.value = repo.runIntegrityCheck()
                _metadata.value = repo.getMetadata()
                _books.value = repo.getBooks()
                val issues = repo.getAllIssues()
                _allIssues.value = issues
                _searchResults.value = issues
                _scholars.value = repo.getScholars()
                _references.value = repo.getReferences()
                _terminology.value = repo.getTerminology()
                _chaptersByBook.value = emptyMap()
                if (issues.isNotEmpty() && _comparisonIssue.value == null) {
                    _comparisonIssue.value = repo.getIssueDetail(issues.first().id)
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء تحميل قاعدة البيانات: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }
    }

    fun loadChaptersForBook(bookId: Int) {
        if (_chaptersByBook.value.containsKey(bookId)) return
        viewModelScope.launch {
            val loaded = repo.getChaptersForBook(bookId)
            _chaptersByBook.value = _chaptersByBook.value + (bookId to loaded)
        }
    }

    fun openIssue(issueId: Int) {
        viewModelScope.launch {
            val detail = repo.getIssueDetail(issueId)
            _selectedIssue.value = detail
            if (detail != null) {
                repo.recordVisit(detail.issue.id, detail.issue.title, detail.issue.bookTitle)
            }
        }
    }

    fun selectIssueForComparison(issueId: Int) {
        viewModelScope.launch {
            _comparisonIssue.value = repo.getIssueDetail(issueId)
        }
    }

    fun setComparisonFilter(filter: ComparisonFilter) {
        _comparisonFilter.value = filter
    }

    fun updateSearch(query: String, scope: SearchScope = _searchScope.value, recordToHistory: Boolean = false) {
        _searchQuery.value = query
        _searchScope.value = scope
        viewModelScope.launch {
            _searchResults.value = repo.searchIssues(query, scope)
            if (recordToHistory && query.trim().length >= 2) {
                repo.recordSearch(query)
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { repo.clearSearchHistory() }
    }

    fun observeIsBookmarked(issueId: Int): Flow<Boolean> = repo.observeIsBookmarked(issueId)
    fun observeNotesForIssue(issueId: Int): Flow<List<PersonalNoteEntity>> = repo.observeNotesForIssue(issueId)

    fun toggleBookmark(issue: IssueRecord, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            repo.toggleBookmark(issue.id, issue.title, issue.bookTitle, currentlyBookmarked)
        }
    }

    fun saveNote(issueId: Int, issueTitle: String, text: String) {
        viewModelScope.launch {
            repo.saveNote(issueId, issueTitle, text)
            _statusMessage.value = "تم حفظ الملاحظة بنجاح."
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch { repo.deleteNote(noteId) }
    }

    // Settings
    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.55f)
        _readerConfig.value = _readerConfig.value.copy(fontScale = clamped)
        prefs.edit().putFloat("font_scale", clamped).apply()
    }

    fun setLineSpacing(mult: Float) {
        val clamped = mult.coerceIn(1.25f, 2.0f)
        _readerConfig.value = _readerConfig.value.copy(lineSpacingMultiplier = clamped)
        prefs.edit().putFloat("line_spacing", clamped).apply()
    }

    // Database Pack Management
    fun runIntegrityCheckNow() {
        viewModelScope.launch {
            val report = repo.runIntegrityCheck()
            _integrityReport.value = report
            _statusMessage.value = if (report.isHealthy) {
                "فحص السلامة ناجح: قاعدة البيانات سليمة (${report.issuesCount} مسألة، ${report.opinionsCount} قولاً)."
            } else {
                "تنبيه في فحص السلامة: ${report.integrityMessage}"
            }
        }
    }

    fun importPack(uri: Uri) {
        viewModelScope.launch {
            val res = repo.importDatabasePack(uri)
            res.onSuccess { report ->
                _integrityReport.value = report
                refreshAllEncyclopediaData()
                _statusMessage.value = "تم استيراد حزمة البيانات بنجاح (الإصدار ${report.contentVersion} — ${report.issuesCount} مسألة)."
            }.onFailure { err ->
                _statusMessage.value = "تعذر استيراد الحزمة: ${err.localizedMessage}"
            }
        }
    }

    fun exportBackup(onReady: (File) -> Unit) {
        viewModelScope.launch {
            repo.exportBackup().onSuccess { file ->
                _statusMessage.value = "تم إنشاء نسخة احتياطية مضغوطة بنجاح."
                onReady(file)
            }.onFailure { err ->
                _statusMessage.value = "فشل إنشاء النسخة الاحتياطية: ${err.localizedMessage}"
            }
        }
    }

    fun resetToStarterDatabase() {
        viewModelScope.launch {
            repo.resetToStarterDatabase().onSuccess { report ->
                _integrityReport.value = report
                refreshAllEncyclopediaData()
                _statusMessage.value = "تمت استعادة قاعدة البيانات التأسيسية بنجاح."
            }.onFailure { err ->
                _statusMessage.value = "فشلت الاستعادة: ${err.localizedMessage}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
