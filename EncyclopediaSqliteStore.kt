package com.shafii.encyclopedia.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.shafii.encyclopedia.data.repository.*
import com.shafii.encyclopedia.util.ArabicNormalizer
import com.shafii.encyclopedia.util.DatabasePackManager
import java.io.File

/**
 * Read-only SQLite + FTS5 accessor over the replaceable encyclopedia content pack.
 * Supports hot-reloading after importing a new .db or .db.gz pack.
 */
class EncyclopediaSqliteStore(private val packManager: DatabasePackManager) {

    @Volatile
    private var db: SQLiteDatabase? = null
    private val lock = Any()

    private fun openDb(): SQLiteDatabase {
        val current = db
        if (current != null && current.isOpen) return current
        synchronized(lock) {
            val existing = db
            if (existing != null && existing.isOpen) return existing
            val file: File = packManager.ensureStarterDatabaseReady()
            val opened = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            db = opened
            return opened
        }
    }

    fun reloadDatabase() {
        synchronized(lock) {
            db?.close()
            db = null
            openDb()
        }
    }

    fun getMetadata(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        openDb().rawQuery("SELECT key, value FROM db_metadata", null).use { c ->
            while (c.moveToNext()) {
                map[c.getString(0)] = c.getString(1)
            }
        }
        return map
    }

    fun getAllBooksWithCounts(): List<BookSummary> {
        val sql = """
            SELECT b.id, b.title, b.short_title, b.sort_order, b.description,
                   (SELECT COUNT(*) FROM chapters c WHERE c.book_id = b.id) AS chap_cnt,
                   (SELECT COUNT(*) FROM issues i JOIN chapters c ON i.chapter_id = c.id WHERE c.book_id = b.id) AS issue_cnt
            FROM books b
            ORDER BY b.sort_order ASC
        """.trimIndent()
        return openDb().rawQuery(sql, null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        BookSummary(
                            id = c.getInt(0),
                            title = c.getString(1),
                            shortTitle = c.getString(2),
                            sortOrder = c.getInt(3),
                            description = c.getString(4),
                            chaptersCount = c.getInt(5),
                            issuesCount = c.getInt(6)
                        )
                    )
                }
            }
        }
    }

    fun getChaptersForBook(bookId: Int): List<ChapterWithIssues> {
        val chapters = openDb().rawQuery(
            "SELECT id, book_id, title, description FROM chapters WHERE book_id = ? ORDER BY sort_order ASC",
            arrayOf(bookId.toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val chapId = c.getInt(0)
                    add(
                        ChapterWithIssues(
                            id = chapId,
                            bookId = c.getInt(1),
                            title = c.getString(2),
                            description = c.getString(3),
                            issues = getIssuesForChapter(chapId)
                        )
                    )
                }
            }
        }
        return chapters
    }

    fun getIssuesForChapter(chapterId: Int): List<IssueSummary> {
        val sql = """
            SELECT i.id, i.chapter_id, i.title, i.short_ruling, i.fiqh_classification,
                   b.title, c.title, vs.label_ar, i.verification_status_code, i.stage_level
            FROM issues i
            JOIN chapters c ON i.chapter_id = c.id
            JOIN books b ON c.book_id = b.id
            JOIN verification_status vs ON i.verification_status_code = vs.code
            WHERE i.chapter_id = ?
            ORDER BY i.id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(chapterId.toString())).use { readIssueSummaries(it) }
    }

    fun getAllIssues(): List<IssueSummary> {
        val sql = """
            SELECT i.id, i.chapter_id, i.title, i.short_ruling, i.fiqh_classification,
                   b.title, c.title, vs.label_ar, i.verification_status_code, i.stage_level
            FROM issues i
            JOIN chapters c ON i.chapter_id = c.id
            JOIN books b ON c.book_id = b.id
            JOIN verification_status vs ON i.verification_status_code = vs.code
            ORDER BY b.sort_order ASC, i.id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, null).use { readIssueSummaries(it) }
    }

    fun searchIssues(rawQuery: String, scope: SearchScope): List<IssueSummary> {
        val columnPrefix = when (scope) {
            SearchScope.ALL -> null
            SearchScope.TITLE -> "title_norm"
            SearchScope.SCHOLAR -> "scholars_norm"
            SearchScope.REFERENCE -> "references_norm"
            SearchScope.CHAPTER -> "keywords_norm"
        }
        val ftsQuery = ArabicNormalizer.buildFts5Query(rawQuery, columnPrefix)
        if (ftsQuery.isBlank()) return getAllIssues()

        val sql = """
            SELECT i.id, i.chapter_id, i.title, i.short_ruling, i.fiqh_classification,
                   b.title, c.title, vs.label_ar, i.verification_status_code, i.stage_level
            FROM search_index si
            JOIN issues i ON i.id = si.issue_id
            JOIN chapters c ON i.chapter_id = c.id
            JOIN books b ON c.book_id = b.id
            JOIN verification_status vs ON i.verification_status_code = vs.code
            WHERE search_index MATCH ?
            ORDER BY i.id ASC
            LIMIT 50
        """.trimIndent()

        return try {
            openDb().rawQuery(sql, arrayOf(ftsQuery)).use { readIssueSummaries(it) }
        } catch (_: Exception) {
            // Safe LIKE fallback if user enters unusual punctuation
            val norm = "%${ArabicNormalizer.normalize(rawQuery)}%"
            val fallbackSql = """
                SELECT i.id, i.chapter_id, i.title, i.short_ruling, i.fiqh_classification,
                       b.title, c.title, vs.label_ar, i.verification_status_code, i.stage_level
                FROM issues i
                JOIN chapters c ON i.chapter_id = c.id
                JOIN books b ON c.book_id = b.id
                JOIN verification_status vs ON i.verification_status_code = vs.code
                WHERE i.title_normalized LIKE ? OR i.keywords_normalized LIKE ?
                ORDER BY i.id ASC
            """.trimIndent()
            openDb().rawQuery(fallbackSql, arrayOf(norm, norm)).use { readIssueSummaries(it) }
        }
    }

    fun getIssueFullDetail(issueId: Int): IssueFullDetail? {
        val sql = """
            SELECT i.id, i.chapter_id, i.title, i.fiqh_classification, i.issue_framing,
                   i.short_ruling, i.mutamad_summary, i.mutamad_early, i.mutamad_late,
                   i.rafii_nawawi_agreement, i.qadim_summary, i.jadid_summary,
                   i.ashab_summary, i.khilaf_awjuh_summary, i.historical_evolution,
                   i.evidence_discussion, i.reason_for_disagreement, i.comparative_summary,
                   i.verification_status_code, vs.label_ar, b.title, c.title
            FROM issues i
            JOIN chapters c ON i.chapter_id = c.id
            JOIN books b ON c.book_id = b.id
            JOIN verification_status vs ON i.verification_status_code = vs.code
            WHERE i.id = ?
        """.trimIndent()

        val issueRecord = openDb().rawQuery(sql, arrayOf(issueId.toString())).use { c ->
            if (!c.moveToFirst()) return null
            IssueRecord(
                id = c.getInt(0),
                chapterId = c.getInt(1),
                title = c.getString(2),
                fiqhClassification = c.getString(3),
                issueFraming = c.getString(4),
                shortRuling = c.getString(5),
                mutamadSummary = c.getString(6),
                mutamadEarly = c.getString(7),
                mutamadLate = c.getString(8),
                rafiiNawawiAgreement = c.getString(9),
                qadimSummary = c.getString(10),
                jadidSummary = c.getString(11),
                ashabSummary = c.getString(12),
                khilafAwjuhSummary = c.getString(13),
                historicalEvolution = c.getString(14),
                evidenceDiscussion = c.getString(15),
                reasonForDisagreement = c.getString(16),
                comparativeSummary = c.getString(17),
                verificationStatusCode = c.getString(18),
                bookTitle = c.getString(20),
                chapterTitle = c.getString(21)
            ) to c.getString(19)
        }

        val opinions = getOpinionsForIssue(issueId)
        val evidences = getEvidencesForIssue(issueId)
        val related = getRelatedIssues(issueId)

        return IssueFullDetail(
            issue = issueRecord.first,
            verificationLabel = issueRecord.second,
            opinions = opinions,
            evidences = evidences,
            relatedIssues = related
        )
    }

    private fun getOpinionsForIssue(issueId: Int): List<OpinionDetail> {
        val sql = """
            SELECT o.id, o.issue_id, o.scholar_id, s.common_name, s.full_name,
                   sp.code, sp.name_ar, sp.hijri_range,
                   o.opinion_type, o.qadim_jadid_class, o.is_mutamad, o.is_contemporary,
                   o.opinion_text, o.reasoning_summary, o.attribution_notes,
                   o.verification_status_code, vs.label_ar
            FROM opinions o
            JOIN scholars s ON o.scholar_id = s.id
            JOIN scholarly_periods sp ON o.period_id = sp.id
            JOIN verification_status vs ON o.verification_status_code = vs.code
            WHERE o.issue_id = ?
            ORDER BY sp.sort_order ASC, o.sort_order ASC
        """.trimIndent()

        return openDb().rawQuery(sql, arrayOf(issueId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val opId = c.getInt(0)
                    add(
                        OpinionDetail(
                            id = opId,
                            issueId = c.getInt(1),
                            scholarId = c.getInt(2),
                            scholarCommonName = c.getString(3),
                            scholarFullName = c.getString(4),
                            periodCode = c.getString(5),
                            periodName = c.getString(6),
                            periodHijriRange = c.getString(7),
                            opinionType = c.getString(8),
                            qadimJadidClass = c.getString(9),
                            isMutamad = c.getInt(10) == 1,
                            isContemporary = c.getInt(11) == 1,
                            opinionText = c.getString(12),
                            reasoningSummary = c.getString(13),
                            attributionNotes = c.getString(14),
                            verificationCode = c.getString(15),
                            verificationLabel = c.getString(16),
                            sources = getSourcesForOpinion(opId),
                            quotations = getQuotationsForOpinion(opId)
                        )
                    )
                }
            }
        }
    }

    private fun getSourcesForOpinion(opinionId: Int): List<OpinionSourceDetail> {
        val sql = """
            SELECT os.id, os.reference_id, rw.title, rw.author_name_fallback,
                   os.volume, os.page, os.section_location, os.citation_type,
                   os.checked_against_original, vs.label_ar, os.notes,
                   e.publisher
            FROM opinion_sources os
            JOIN reference_works rw ON os.reference_id = rw.id
            JOIN verification_status vs ON os.verification_status_code = vs.code
            LEFT JOIN editions e ON os.edition_id = e.id
            WHERE os.opinion_id = ?
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(opinionId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        OpinionSourceDetail(
                            id = c.getInt(0),
                            referenceId = c.getInt(1),
                            referenceTitle = c.getString(2),
                            authorName = c.getString(3),
                            volume = if (c.isNull(4)) null else c.getString(4),
                            page = if (c.isNull(5)) null else c.getString(5),
                            sectionLocation = c.getString(6),
                            citationType = c.getString(7),
                            checkedAgainstOriginal = c.getInt(8) == 1,
                            verificationLabel = c.getString(9),
                            notes = c.getString(10),
                            editionPublisher = if (c.isNull(11)) null else c.getString(11)
                        )
                    )
                }
            }
        }
    }

    private fun getQuotationsForOpinion(opinionId: Int): List<QuotationDetail> {
        val sql = """
            SELECT q.id, rw.title, q.volume, q.page, q.quotation_text,
                   q.is_exact_verbatim, q.checked_against_original, q.notes
            FROM quotations q
            JOIN reference_works rw ON q.reference_id = rw.id
            WHERE q.opinion_id = ?
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(opinionId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        QuotationDetail(
                            id = c.getInt(0),
                            referenceTitle = c.getString(1),
                            volume = if (c.isNull(2)) null else c.getString(2),
                            page = if (c.isNull(3)) null else c.getString(3),
                            quotationText = c.getString(4),
                            isExactVerbatim = c.getInt(5) == 1,
                            checkedAgainstOriginal = c.getInt(6) == 1,
                            notes = c.getString(7)
                        )
                    )
                }
            }
        }
    }

    private fun getEvidencesForIssue(issueId: Int): List<EvidenceDetail> {
        val sql = """
            SELECT id, opinion_id, evidence_type, text_content, source_reference, authenticity_note
            FROM evidence
            WHERE issue_id = ?
            ORDER BY id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(issueId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        EvidenceDetail(
                            id = c.getInt(0),
                            opinionId = if (c.isNull(1)) null else c.getInt(1),
                            evidenceType = c.getString(2),
                            textContent = c.getString(3),
                            sourceReference = c.getString(4),
                            authenticityNote = c.getString(5)
                        )
                    )
                }
            }
        }
    }

    private fun getRelatedIssues(issueId: Int): List<RelatedIssueItem> {
        val sql = """
            SELECT i.id, i.title, cr.relation_type, cr.notes
            FROM cross_references cr
            JOIN issues i ON (
                (cr.source_issue_id = ? AND i.id = cr.target_issue_id)
                OR (cr.target_issue_id = ? AND i.id = cr.source_issue_id)
            )
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(issueId.toString(), issueId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        RelatedIssueItem(
                            issueId = c.getInt(0),
                            title = c.getString(1),
                            relationType = c.getString(2),
                            notes = c.getString(3)
                        )
                    )
                }
            }
        }
    }

    fun getAllScholars(): List<ScholarProfile> {
        val sql = """
            SELECT s.id, s.full_name, s.common_name, s.birth_year_hijri, s.death_year_hijri,
                   sp.name_ar, sp.hijri_range, s.teachers, s.students, s.contributions, s.bibliography, s.notes
            FROM scholars s
            JOIN scholarly_periods sp ON s.period_id = sp.id
            ORDER BY sp.sort_order ASC, s.id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val sid = c.getInt(0)
                    add(
                        ScholarProfile(
                            id = sid,
                            fullName = c.getString(1),
                            commonName = c.getString(2),
                            birthYearHijri = c.getString(3) ?: "غير محدد",
                            deathYearHijri = c.getString(4) ?: "غير محدد",
                            periodName = c.getString(5),
                            periodHijriRange = c.getString(6),
                            teachers = c.getString(7),
                            students = c.getString(8),
                            contributions = c.getString(9),
                            bibliography = c.getString(10),
                            notes = c.getString(11),
                            documentedIssues = getIssuesForScholar(sid)
                        )
                    )
                }
            }
        }
    }

    private fun getIssuesForScholar(scholarId: Int): List<Pair<Int, String>> {
        val sql = """
            SELECT DISTINCT i.id, i.title
            FROM opinions o JOIN issues i ON o.issue_id = i.id
            WHERE o.scholar_id = ?
            ORDER BY i.id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(scholarId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(c.getInt(0) to c.getString(1))
            }
        }
    }

    fun getAllReferences(): List<ReferenceProfile> {
        val sql = """
            SELECT id, title, author_name_fallback, authorship_date, subject,
                   book_type, print_status, manuscript_status, notes
            FROM reference_works
            ORDER BY id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val rid = c.getInt(0)
                    add(
                        ReferenceProfile(
                            id = rid,
                            title = c.getString(1),
                            authorName = c.getString(2),
                            authorshipDate = c.getString(3),
                            subject = c.getString(4),
                            bookType = c.getString(5),
                            printStatus = c.getString(6),
                            manuscriptStatus = c.getString(7),
                            notes = c.getString(8),
                            editions = getEditionsForReference(rid),
                            manuscripts = getManuscriptsForReference(rid),
                            citedInIssues = getIssuesCitingReference(rid)
                        )
                    )
                }
            }
        }
    }

    private fun getEditionsForReference(refId: Int): List<EditionItem> {
        val sql = "SELECT publisher, editor, publication_year, volumes_count, notes FROM editions WHERE reference_id = ?"
        return openDb().rawQuery(sql, arrayOf(refId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(EditionItem(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)))
                }
            }
        }
    }

    private fun getManuscriptsForReference(refId: Int): List<ManuscriptItem> {
        val sql = """
            SELECT m.library_name, m.catalog_number, m.copy_date, m.completeness_notes, vs.label_ar
            FROM manuscripts m
            JOIN verification_status vs ON m.verification_status_code = vs.code
            WHERE m.reference_id = ?
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(refId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(ManuscriptItem(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)))
                }
            }
        }
    }

    private fun getIssuesCitingReference(refId: Int): List<Pair<Int, String>> {
        val sql = """
            SELECT DISTINCT i.id, i.title
            FROM opinion_sources os
            JOIN opinions o ON os.opinion_id = o.id
            JOIN issues i ON o.issue_id = i.id
            WHERE os.reference_id = ?
            ORDER BY i.id ASC
        """.trimIndent()
        return openDb().rawQuery(sql, arrayOf(refId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(c.getInt(0) to c.getString(1))
            }
        }
    }

    fun getAllTerminology(): List<TerminologyItem> {
        val sql = "SELECT id, term, definition, synonyms, usage_context, source_note FROM terminology ORDER BY id ASC"
        return openDb().rawQuery(sql, null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        TerminologyItem(
                            id = c.getInt(0),
                            term = c.getString(1),
                            definition = c.getString(2),
                            synonyms = c.getString(3),
                            usageContext = c.getString(4),
                            sourceNote = c.getString(5)
                        )
                    )
                }
            }
        }
    }

    private fun readIssueSummaries(c: Cursor): List<IssueSummary> = buildList {
        while (c.moveToNext()) {
            add(
                IssueSummary(
                    id = c.getInt(0),
                    chapterId = c.getInt(1),
                    title = c.getString(2),
                    shortRuling = c.getString(3),
                    fiqhClassification = c.getString(4),
                    bookTitle = c.getString(5),
                    chapterTitle = c.getString(6),
                    verificationLabel = c.getString(7),
                    verificationCode = c.getString(8),
                    stageLevel = c.getInt(9)
                )
            )
        }
    }
}
