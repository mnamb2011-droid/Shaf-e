#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Incremental Content Pack Importer (JSON -> SQLite + FTS5 Rebuild)
Allows scholars and editors to add verified Stage 2/3/4 issues without touching Android code.
Usage:
    python3 import_pack.py sample_expansion_pack.json shafii_encyclopedia.db
"""

import gzip
import json
import shutil
import sqlite3
import sys
from pathlib import Path
from build_database import build_fts5_index, normalize_arabic


def import_json_pack(pack_path: Path, db_path: Path) -> None:
    data = json.loads(pack_path.read_text(encoding="utf-8"))
    conn = sqlite3.connect(db_path)
    conn.execute("PRAGMA foreign_keys = ON;")
    cur = conn.cursor()

    try:
        for issue in data.get("issues", []):
            cur.execute(
                """INSERT OR REPLACE INTO issues(
                    id, chapter_id, title, title_normalized, fiqh_classification, issue_framing,
                    short_ruling, mutamad_summary, mutamad_early, mutamad_late, rafii_nawawi_agreement,
                    qadim_summary, jadid_summary, ashab_summary, khilaf_awjuh_summary,
                    historical_evolution, evidence_discussion, reason_for_disagreement,
                    comparative_summary, verification_status_code, stage_level, keywords_normalized
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    issue["id"], issue["chapter_id"], issue["title"], normalize_arabic(issue["title"]),
                    issue["fiqh_classification"], issue["issue_framing"], issue["short_ruling"],
                    issue["mutamad_summary"], issue.get("mutamad_early", "لم يثبت لدينا نص موثق"),
                    issue.get("mutamad_late", "لم يثبت لدينا نص موثق"),
                    issue.get("rafii_nawawi_agreement", "لم يثبت لدينا نص موثق"),
                    issue.get("qadim_summary", "لم يثبت لدينا نص موثق"),
                    issue.get("jadid_summary", "لم يثبت لدينا نص موثق"),
                    issue.get("ashab_summary", "لم يثبت لدينا نص موثق"),
                    issue.get("khilaf_awjuh_summary", "لم يثبت لدينا نص موثق"),
                    issue["historical_evolution"], issue["evidence_discussion"],
                    issue["reason_for_disagreement"], issue["comparative_summary"],
                    issue["verification_status_code"], issue.get("stage_level", 2),
                    normalize_arabic(issue.get("keywords", "")),
                ),
            )

        for op in data.get("opinions", []):
            cur.execute(
                """INSERT OR REPLACE INTO opinions(
                    id, issue_id, scholar_id, period_id, opinion_type, qadim_jadid_class,
                    is_mutamad, is_contemporary, opinion_text, reasoning_summary,
                    attribution_notes, verification_status_code, sort_order
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    op["id"], op["issue_id"], op["scholar_id"], op["period_id"],
                    op["opinion_type"], op["qadim_jadid_class"],
                    int(op.get("is_mutamad", 0)), int(op.get("is_contemporary", 0)),
                    op["opinion_text"], op.get("reasoning_summary", ""),
                    op.get("attribution_notes", ""), op["verification_status_code"],
                    op.get("sort_order", 1),
                ),
            )

        for src in data.get("opinion_sources", []):
            cur.execute(
                """INSERT OR REPLACE INTO opinion_sources(
                    id, opinion_id, reference_id, edition_id, volume, page, section_location,
                    citation_type, checked_against_original, verification_status_code, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    src["id"], src["opinion_id"], src["reference_id"], src.get("edition_id"),
                    src.get("volume"), src.get("page"), src["section_location"],
                    src["citation_type"], int(src.get("checked_against_original", 0)),
                    src["verification_status_code"], src.get("notes", ""),
                ),
            )

        if "pack_version" in data:
            cur.execute(
                "INSERT OR REPLACE INTO db_metadata(key, value) VALUES ('content_version', ?)",
                (str(data["pack_version"]),),
            )

        build_fts5_index(cur)
        conn.commit()
        conn.execute("VACUUM;")
    finally:
        conn.close()

    gz_out = db_path.with_suffix(".db.gz")
    with open(db_path, "rb") as f_in, gzip.open(gz_out, "wb", compresslevel=9) as f_out:
        shutil.copyfileobj(f_in, f_out)
    print(f"Imported pack '{pack_path.name}' into {db_path} and updated {gz_out}.")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 import_pack.py <pack.json> <target.db>")
        sys.exit(1)
    import_json_pack(Path(sys.argv[1]), Path(sys.argv[2]))
