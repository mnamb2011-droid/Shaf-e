-- ============================================================================
-- الموسوعة الشافعية الشاملة | The Comprehensive Shafi'i Fiqh Encyclopedia
-- Normalized Offline SQLite Schema (Version 1.0) with FTS5 Arabic Search
-- ============================================================================

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = DELETE;
PRAGMA page_size = 4096;

-- 1. Database Metadata & Versioning
CREATE TABLE IF NOT EXISTS db_metadata (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);

-- 2. Editorial Verification Statuses (Strict Policy — Section 11)
CREATE TABLE IF NOT EXISTS verification_status (
    code TEXT PRIMARY KEY NOT NULL,
    label_ar TEXT NOT NULL,
    description TEXT NOT NULL,
    severity_rank INTEGER NOT NULL
);

-- 3. Traditional Books of Shafi'i Fiqh (17 Books)
CREATE TABLE IF NOT EXISTS books (
    id INTEGER PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    short_title TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    description TEXT NOT NULL
);

-- 4. Hierarchical Chapters & Sections
CREATE TABLE IF NOT EXISTS chapters (
    id INTEGER PRIMARY KEY NOT NULL,
    book_id INTEGER NOT NULL,
    parent_chapter_id INTEGER,
    title TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

-- 5. Historical Scholarly Periods
CREATE TABLE IF NOT EXISTS scholarly_periods (
    id INTEGER PRIMARY KEY NOT NULL,
    code TEXT UNIQUE NOT NULL,
    name_ar TEXT NOT NULL,
    hijri_range TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    description TEXT NOT NULL
);

-- 6. Shafi'i Scholars & Authorities
CREATE TABLE IF NOT EXISTS scholars (
    id INTEGER PRIMARY KEY NOT NULL,
    full_name TEXT NOT NULL,
    common_name TEXT NOT NULL,
    name_normalized TEXT NOT NULL,
    birth_year_hijri TEXT,
    death_year_hijri TEXT,
    period_id INTEGER NOT NULL,
    teachers TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    students TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    contributions TEXT NOT NULL,
    bibliography TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (period_id) REFERENCES scholarly_periods(id)
);

-- 7. Primary & Secondary Reference Books
CREATE TABLE IF NOT EXISTS reference_works (
    id INTEGER PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    title_normalized TEXT NOT NULL,
    author_id INTEGER,
    author_name_fallback TEXT NOT NULL,
    authorship_date TEXT NOT NULL DEFAULT 'غير محدد بدقة',
    subject TEXT NOT NULL,
    book_type TEXT NOT NULL, -- متن، شرح، حاشية، مختصر، فتاوى، أصول، قواعد، تخريج، موسوعة
    print_status TEXT NOT NULL,
    manuscript_status TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (author_id) REFERENCES scholars(id)
);

-- 8. Verified Print Editions
CREATE TABLE IF NOT EXISTS editions (
    id INTEGER PRIMARY KEY NOT NULL,
    reference_id INTEGER NOT NULL,
    publisher TEXT NOT NULL,
    editor TEXT NOT NULL DEFAULT 'غير محدد',
    publication_year TEXT NOT NULL DEFAULT 'غير محدد',
    volumes_count TEXT NOT NULL DEFAULT 'غير محدد',
    notes TEXT NOT NULL DEFAULT '',
    is_verified INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (reference_id) REFERENCES reference_works(id) ON DELETE CASCADE
);

-- 9. Manuscript Records
CREATE TABLE IF NOT EXISTS manuscripts (
    id INTEGER PRIMARY KEY NOT NULL,
    reference_id INTEGER NOT NULL,
    library_name TEXT NOT NULL,
    catalog_number TEXT NOT NULL DEFAULT 'يحتاج مراجعة الفهرس',
    copy_date TEXT NOT NULL DEFAULT 'غير محدد',
    completeness_notes TEXT NOT NULL DEFAULT '',
    verification_status_code TEXT NOT NULL,
    FOREIGN KEY (reference_id) REFERENCES reference_works(id) ON DELETE CASCADE,
    FOREIGN KEY (verification_status_code) REFERENCES verification_status(code)
);

-- 10. Jurisprudential Issues (All 19 Structured Sections)
CREATE TABLE IF NOT EXISTS issues (
    id INTEGER PRIMARY KEY NOT NULL,
    chapter_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    title_normalized TEXT NOT NULL,
    fiqh_classification TEXT NOT NULL,
    issue_framing TEXT NOT NULL,           -- تصوير المسألة
    short_ruling TEXT NOT NULL,            -- الحكم المختصر
    mutamad_summary TEXT NOT NULL,         -- المعتمد في المذهب
    mutamad_early TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    mutamad_late TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    rafii_nawawi_agreement TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    qadim_summary TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    jadid_summary TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    ashab_summary TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    khilaf_awjuh_summary TEXT NOT NULL DEFAULT 'لم يثبت لدينا نص موثق',
    historical_evolution TEXT NOT NULL,    -- تطور الترجيح عبر العصور
    evidence_discussion TEXT NOT NULL,     -- مناقشة الأدلة
    reason_for_disagreement TEXT NOT NULL, -- سبب الخلاف
    comparative_summary TEXT NOT NULL,     -- خلاصة مقارنة
    verification_status_code TEXT NOT NULL,
    stage_level INTEGER NOT NULL DEFAULT 1,
    keywords_normalized TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    FOREIGN KEY (verification_status_code) REFERENCES verification_status(code)
);

-- 11. Historical & Scholarly Opinions per Issue
CREATE TABLE IF NOT EXISTS opinions (
    id INTEGER PRIMARY KEY NOT NULL,
    issue_id INTEGER NOT NULL,
    scholar_id INTEGER NOT NULL,
    period_id INTEGER NOT NULL,
    opinion_type TEXT NOT NULL,            -- قول_الشافعي_القديم | قول_الشافعي_الجديد | قول_الأصحاب | وجه | طريق | معتمد_المتقدمين | معتمد_المتأخرين | اختيار_المحققين | اجتهاد_معاصر
    qadim_jadid_class TEXT NOT NULL,       -- قديم | جديد | مرجح_للقديم | موافق_للجديد | غير_منطبق
    is_mutamad INTEGER NOT NULL DEFAULT 0,
    is_contemporary INTEGER NOT NULL DEFAULT 0,
    opinion_text TEXT NOT NULL,
    reasoning_summary TEXT NOT NULL DEFAULT '',
    attribution_notes TEXT NOT NULL DEFAULT '',
    verification_status_code TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    FOREIGN KEY (scholar_id) REFERENCES scholars(id),
    FOREIGN KEY (period_id) REFERENCES scholarly_periods(id),
    FOREIGN KEY (verification_status_code) REFERENCES verification_status(code)
);

-- 12. Opinion Source Citations (Strict Anti-Fabrication Policy)
CREATE TABLE IF NOT EXISTS opinion_sources (
    id INTEGER PRIMARY KEY NOT NULL,
    opinion_id INTEGER NOT NULL,
    reference_id INTEGER NOT NULL,
    edition_id INTEGER,
    volume TEXT,                           -- NULL when not verified against a specific print edition
    page TEXT,                             -- NULL when not verified against a specific print edition
    section_location TEXT NOT NULL,        -- Verified chapter/bab location inside the classical text
    citation_type TEXT NOT NULL,           -- نص_مباشر | ملخص_بالمعنى | نسبة_فقهية | استنتاج_تحريري
    checked_against_original INTEGER NOT NULL DEFAULT 0,
    verification_status_code TEXT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (opinion_id) REFERENCES opinions(id) ON DELETE CASCADE,
    FOREIGN KEY (reference_id) REFERENCES reference_works(id),
    FOREIGN KEY (edition_id) REFERENCES editions(id),
    FOREIGN KEY (verification_status_code) REFERENCES verification_status(code)
);

-- 13. Evidences (أدلة الأقوال)
CREATE TABLE IF NOT EXISTS evidence (
    id INTEGER PRIMARY KEY NOT NULL,
    issue_id INTEGER NOT NULL,
    opinion_id INTEGER,
    evidence_type TEXT NOT NULL,           -- قرآن | سنة | إجماع | قياس | أثر_صحابي | قاعدة_فقهية | تعليل_نظري
    text_content TEXT NOT NULL,
    source_reference TEXT NOT NULL,
    authenticity_note TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    FOREIGN KEY (opinion_id) REFERENCES opinions(id) ON DELETE SET NULL
);

-- 14. Textual Quotations
CREATE TABLE IF NOT EXISTS quotations (
    id INTEGER PRIMARY KEY NOT NULL,
    opinion_id INTEGER NOT NULL,
    reference_id INTEGER NOT NULL,
    edition_id INTEGER,
    volume TEXT,
    page TEXT,
    quotation_text TEXT NOT NULL,
    is_exact_verbatim INTEGER NOT NULL DEFAULT 0,
    checked_against_original INTEGER NOT NULL DEFAULT 0,
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (opinion_id) REFERENCES opinions(id) ON DELETE CASCADE,
    FOREIGN KEY (reference_id) REFERENCES reference_works(id),
    FOREIGN KEY (edition_id) REFERENCES editions(id)
);

-- 15. Cross-References between Related Issues
CREATE TABLE IF NOT EXISTS cross_references (
    id INTEGER PRIMARY KEY NOT NULL,
    source_issue_id INTEGER NOT NULL,
    target_issue_id INTEGER NOT NULL,
    relation_type TEXT NOT NULL,           -- نظير_فقهي | مبني_عليه | مستثنى_منه | متمم | خلاف_مشابه
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (source_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    FOREIGN KEY (target_issue_id) REFERENCES issues(id) ON DELETE CASCADE
);

-- 16. Shafi'i Technical Terminology & Synonyms (مصطلحات المذهب والمرادفات)
CREATE TABLE IF NOT EXISTS terminology (
    id INTEGER PRIMARY KEY NOT NULL,
    term TEXT NOT NULL,
    term_normalized TEXT NOT NULL,
    definition TEXT NOT NULL,
    synonyms TEXT NOT NULL DEFAULT '',
    usage_context TEXT NOT NULL DEFAULT '',
    source_note TEXT NOT NULL DEFAULT ''
);

-- 17. FTS5 Full-Text Search Index (Normalized Arabic)
CREATE VIRTUAL TABLE IF NOT EXISTS search_index USING fts5(
    issue_id UNINDEXED,
    title_norm,
    body_norm,
    scholars_norm,
    references_norm,
    keywords_norm,
    tokenize = 'unicode61 remove_diacritics 2'
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_chapters_book ON chapters(book_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_chapters_parent ON chapters(parent_chapter_id);
CREATE INDEX IF NOT EXISTS idx_issues_chapter ON issues(chapter_id);
CREATE INDEX IF NOT EXISTS idx_issues_title_norm ON issues(title_normalized);
CREATE INDEX IF NOT EXISTS idx_opinions_issue ON opinions(issue_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_opinions_scholar ON opinions(scholar_id);
CREATE INDEX IF NOT EXISTS idx_opinions_period ON opinions(period_id);
CREATE INDEX IF NOT EXISTS idx_opinion_sources_opinion ON opinion_sources(opinion_id);
CREATE INDEX IF NOT EXISTS idx_opinion_sources_ref ON opinion_sources(reference_id);
CREATE INDEX IF NOT EXISTS idx_evidence_issue ON evidence(issue_id);
CREATE INDEX IF NOT EXISTS idx_quotations_opinion ON quotations(opinion_id);
CREATE INDEX IF NOT EXISTS idx_cross_refs_source ON cross_references(source_issue_id);
CREATE INDEX IF NOT EXISTS idx_scholars_period ON scholars(period_id);
CREATE INDEX IF NOT EXISTS idx_terminology_norm ON terminology(term_normalized);
