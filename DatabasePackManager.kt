package com.shafii.encyclopedia.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class IntegrityReport(
    val isHealthy: Boolean,
    val integrityMessage: String,
    val foreignKeyErrors: Int,
    val contentVersion: String,
    val contentStage: String,
    val databaseSizeBytes: Long,
    val booksCount: Int,
    val issuesCount: Int,
    val opinionsCount: Int,
    val scholarsCount: Int,
    val referencesCount: Int
)

/**
 * Manages the separate, replaceable offline encyclopedia SQLite database:
 * - First-launch extraction of compressed assets/db/shafii_encyclopedia.db.gz
 * - Importing external .db or .db.gz content packs (with pre-swap integrity verification)
 * - Exporting compressed offline backups & restoring from backup
 * - Running PRAGMA integrity_check & PRAGMA foreign_key_check
 */
class DatabasePackManager(private val context: Context) {

    companion object {
        const val ASSET_GZ_PATH = "db/shafii_encyclopedia.db.gz"
        const val ACTIVE_DB_NAME = "shafii_encyclopedia_active.db"
        const val FALLBACK_MISSING_TEXT = "لم يثبت لدينا نص موثق"
    }

    fun getActiveDatabaseFile(): File {
        val dir = File(context.filesDir, "encyclopedia_db")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, ACTIVE_DB_NAME)
    }

    fun ensureStarterDatabaseReady(): File {
        val target = getActiveDatabaseFile()
        if (target.exists() && target.length() > 4096L) {
            return target
        }
        val tmp = File(target.parentFile, "${ACTIVE_DB_NAME}.tmp")
        context.assets.open(ASSET_GZ_PATH).use { rawIn ->
            GZIPInputStream(rawIn).use { gzIn ->
                FileOutputStream(tmp).use { out ->
                    gzIn.copyTo(out)
                }
            }
        }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
        return target
    }

    fun runIntegrityCheck(dbFile: File = getActiveDatabaseFile()): IntegrityReport {
        if (!dbFile.exists()) {
            return IntegrityReport(false, "ملف قاعدة البيانات غير موجود", -1, "-", "-", 0L, 0, 0, 0, 0, 0)
        }
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val integrityMsg = db.rawQuery("PRAGMA integrity_check;", null).use { c ->
                if (c.moveToFirst()) c.getString(0) else "unknown"
            }
            val fkErrors = db.rawQuery("PRAGMA foreign_key_check;", null).use { c -> c.count }
            val version = queryMeta(db, "content_version")
            val stage = queryMeta(db, "content_stage")
            val books = queryCount(db, "books")
            val issues = queryCount(db, "issues")
            val opinions = queryCount(db, "opinions")
            val scholars = queryCount(db, "scholars")
            val refs = queryCount(db, "reference_works")

            IntegrityReport(
                isHealthy = integrityMsg.equals("ok", ignoreCase = true) && fkErrors == 0 && books > 0,
                integrityMessage = integrityMsg,
                foreignKeyErrors = fkErrors,
                contentVersion = version,
                contentStage = stage,
                databaseSizeBytes = dbFile.length(),
                booksCount = books,
                issuesCount = issues,
                opinionsCount = opinions,
                scholarsCount = scholars,
                referencesCount = refs
            )
        } catch (e: Exception) {
            IntegrityReport(false, "خطأ في الفحص: ${e.localizedMessage}", -1, "-", "-", dbFile.length(), 0, 0, 0, 0, 0)
        } finally {
            db?.close()
        }
    }

    /**
     * Imports a user-selected .db or .db.gz pack safely:
     * Validates SQLite integrity & schema before replacing the active database.
     */
    fun importPackFromUri(uri: Uri): Result<IntegrityReport> {
        val active = getActiveDatabaseFile()
        val staging = File(active.parentFile, "staging_import.db")
        return try {
            val resolver = context.contentResolver
            val inputStream: InputStream = resolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("تعذر فتح الملف المختار."))

            val rawBytes = inputStream.use { it.readBytes() }
            val isGzip = rawBytes.size >= 2 && rawBytes[0] == 0x1F.toByte() && rawBytes[1] == 0x8B.toByte()

            if (isGzip) {
                GZIPInputStream(rawBytes.inputStream()).use { gz ->
                    FileOutputStream(staging).use { out -> gz.copyTo(out) }
                }
            } else {
                FileOutputStream(staging).use { out -> out.write(rawBytes) }
            }

            val report = runIntegrityCheck(staging)
            if (!report.isHealthy) {
                staging.delete()
                return Result.failure(
                    IllegalArgumentException("فشل فحص سلامة الحزمة: ${report.integrityMessage} (أخطاء الروابط: ${report.foreignKeyErrors})")
                )
            }

            val backup = File(active.parentFile, "${ACTIVE_DB_NAME}.prev")
            if (active.exists()) active.copyTo(backup, overwrite = true)
            if (active.exists()) active.delete()
            staging.renameTo(active)

            Result.success(report)
        } catch (e: Exception) {
            if (staging.exists()) staging.delete()
            Result.failure(e)
        }
    }

    /**
     * Exports the active encyclopedia database as a compressed .db.gz backup file.
     */
    fun exportCompressedBackup(): Result<File> {
        return try {
            val active = ensureStarterDatabaseReady()
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val outFile = File(backupDir, "shafii_encyclopedia_backup.db.gz")
            FileInputStream(active).use { fis ->
                GZIPOutputStream(FileOutputStream(outFile)).use { gzos ->
                    fis.copyTo(gzos)
                }
            }
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetToStarterDatabase(): Result<IntegrityReport> {
        return try {
            val active = getActiveDatabaseFile()
            if (active.exists()) active.delete()
            ensureStarterDatabaseReady()
            Result.success(runIntegrityCheck())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryMeta(db: SQLiteDatabase, key: String): String {
        return try {
            db.rawQuery("SELECT value FROM db_metadata WHERE key = ?", arrayOf(key)).use { c ->
                if (c.moveToFirst()) c.getString(0) else "1.0.0"
            }
        } catch (_: Exception) {
            "غير محدد"
        }
    }

    private fun queryCount(db: SQLiteDatabase, table: String): Int {
        return try {
            db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }
}
