-- Migration Template: Schema v1 -> v2 (Incremental Content / Feature Extension)
-- Always backup the existing database before applying migrations.
PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;

-- Example: add optional editorial reviewer metadata for Stage 3/4
-- ALTER TABLE issues ADD COLUMN reviewed_by TEXT DEFAULT '';
-- ALTER TABLE issues ADD COLUMN reviewed_at_utc TEXT DEFAULT '';

UPDATE db_metadata SET value = '2' WHERE key = 'schema_version';

COMMIT;
PRAGMA foreign_keys = ON;
PRAGMA integrity_check;
