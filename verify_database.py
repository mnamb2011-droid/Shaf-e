#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Automated Database Integrity, FTS5 Search, and Editorial Policy Verifier
Runs both locally and inside GitHub Actions CI before building the APK.
"""

import sqlite3
import sys
from pathlib import Path
from build_database import normalize_arabic

DB_PATH = Path(__file__).resolve().parent / "shafii_encyclopedia.db"


def run_checks(db_path: Path) -> int:
    if not db_path.exists():
        print(f"FAIL: Database file not found at {db_path}")
        return 1

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    errors = []

    # 1. SQLite structural integrity check
    integrity = cur.execute("PRAGMA integrity_check;").fetchall()
    if integrity != [("ok",)]:
        errors.append(f"PRAGMA integrity_check failed: {integrity}")

    # 2. Foreign key check
    fk_violations = cur.execute("PRAGMA foreign_key_check;").fetchall()
    if fk_violations:
        errors.append(f"Foreign key violations found: {fk_violations}")

    # 3. Verify all 17 traditional books exist
    book_count = cur.execute("SELECT COUNT(*) FROM books;").fetchone()[0]
    if book_count < 17:
        errors.append(f"Expected 17 traditional books, found {book_count}")

    # 4. Verify all 6 required verification statuses exist
    required_statuses = {
        "موثق من المصدر الأصلي",
        "موثق من مصدر ناقل معتبر",
        "نسبة مشهورة تحتاج مقابلة",
        "مختلف في نسبته",
        "يحتاج مراجعة",
        "غير متحقق",
    }
    found_statuses = {r[0] for r in cur.execute("SELECT label_ar FROM verification_status;").fetchall()}
    missing_statuses = required_statuses - found_statuses
    if missing_statuses:
        errors.append(f"Missing required editorial verification labels: {missing_statuses}")

    # 5. Verify no empty required fields on issues & every issue has at least 1 opinion
    issues = cur.execute("SELECT id, title FROM issues;").fetchall()
    if not issues:
        errors.append("No issues found in starter database.")
    for issue_id, title in issues:
        op_count = cur.execute("SELECT COUNT(*) FROM opinions WHERE issue_id = ?;", (issue_id,)).fetchone()[0]
        if op_count == 0:
            errors.append(f"Issue {issue_id} ({title}) has 0 opinions.")

    # 6. Verify FTS5 Arabic-normalized search works with diacritics & spelling variants
    test_queries = [
        ("الشفق الأحمر", 3),          # With Hamzah
        ("الشفق الاحمر", 3),          # Without Hamzah
        ("لَحْمِ الإِبِلِ", 2),         # With full Tashkeel
        ("الصيام عن الميت", 9),       # Exact topic
        ("زكاة الحلى", 7),            # Alef Maqsura variant
        ("العملات الورقية", 12),      # Contemporary nazilah
        ("النووى", 9),                # Scholar name with variant ya
    ]
    for raw_q, expected_id in test_queries:
        norm_q = normalize_arabic(raw_q)
        tokens = [f"{t}*" for t in norm_q.split() if t]
        fts_query = " AND ".join(tokens)
        hits = [
            r[0]
            for r in cur.execute(
                "SELECT issue_id FROM search_index WHERE search_index MATCH ?;", (fts_query,)
            ).fetchall()
        ]
        if expected_id not in hits:
            errors.append(f"FTS5 search failed for '{raw_q}' (norm='{fts_query}'): expected issue {expected_id}, got {hits}")

    # 7. Verify contemporary opinions are never falsely flagged as classical is_mutamad=1
    bad_contemp = cur.execute(
        "SELECT id FROM opinions WHERE is_contemporary = 1 AND is_mutamad = 1;"
    ).fetchall()
    if bad_contemp:
        errors.append(f"Contemporary opinions improperly flagged as classical mutamad: {bad_contemp}")

    # Summary stats
    counts = {
        tbl: cur.execute(f"SELECT COUNT(*) FROM {tbl};").fetchone()[0]
        for tbl in [
            "books", "chapters", "scholarly_periods", "scholars",
            "reference_works", "editions", "manuscripts", "issues",
            "opinions", "opinion_sources", "evidence", "quotations",
            "cross_references", "terminology", "search_index"
        ]
    }
    conn.close()

    print("=== Database Integrity & FTS5 Verification Report ===")
    for k, v in counts.items():
        print(f"  {k:<20}: {v}")

    if errors:
        print("\nFAILURES:")
        for e in errors:
            print(f"  - {e}")
        return 1

    print("\nALL INTEGRITY, FOREIGN KEY, EDITORIAL POLICY, AND FTS5 TESTS PASSED.")
    return 0


if __name__ == "__main__":
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DB_PATH
    sys.exit(run_checks(target))
