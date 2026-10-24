package com.tetrisprime.kotlin

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 10.3 — backup & restore manager.
 *
 * Exports the entire app state (all SharedPreferences namespaces) to a
 * JSON file on disk, and imports it back. Supports auto-rotating
 * backup-of-backup (`tetris-prime-v4:bak`) so a failed import doesn't
 * lose the previous good state.
 *
 * File location: app's private files directory (`/data/data/<pkg>/files/`)
 *   - `tetris-prime-v4-backup.json` — most recent export
 *   - `tetris-prime-v4-bak.json` — previous export (safety net)
 */
object BackupManager {

    private const val FILE_BACKUP = "tetris-prime-v4-backup.json"
    private const val FILE_BACKUP_BAK = "tetris-prime-v4-bak.json"
    private const val MAX_AUTO_BACKUPS = 5

    /** Result of an export operation. */
    sealed class ExportResult {
        data class Success(val file: File, val byteSize: Int, val namespaceCount: Int) : ExportResult()
        data class Failure(val reason: String) : ExportResult()
    }

    /** Result of an import operation. */
    sealed class ImportResult {
        data class Success(val namespaceCount: Int, val schemaVersion: Int) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }

    /**
     * Export all app data to a JSON file.
     * Rotates the previous backup to .bak before writing.
     */
    fun export(ctx: Context): ExportResult {
        return try {
            val json = StorageManager.exportAll(ctx)
            val filesDir = ctx.filesDir
            if (!filesDir.exists()) filesDir.mkdirs()

            val backupFile = File(filesDir, FILE_BACKUP)
            val bakFile = File(filesDir, FILE_BACKUP_BAK)

            // Rotate: if backup exists, copy it to .bak
            if (backupFile.exists()) {
                backupFile.copyTo(bakFile, overwrite = true)
            }

            // Write new backup
            FileOutputStream(backupFile).use { it.write(json.toByteArray(Charsets.UTF_8)) }

            val namespaceCount = StorageSchemas.ALL_NAMESPACES.size
            ExportResult.Success(
                file = backupFile,
                byteSize = json.length,
                namespaceCount = namespaceCount
            )
        } catch (e: Exception) {
            ExportResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Import app data from a JSON file.
     * Before importing, automatically creates a safety backup (.bak).
     */
    fun import(ctx: Context, file: File): ImportResult {
        return try {
            val json = file.readText(Charsets.UTF_8)

            // Pre-import safety backup
            export(ctx)

            val success = StorageManager.importAll(ctx, json)
            if (success) {
                val root = org.json.JSONObject(json)
                val metadata = root.optJSONObject("metadata")
                val schemaVersion = metadata?.optInt("schemaVersion", 0) ?: 0
                val namespaceCount = StorageSchemas.ALL_NAMESPACES.size
                ImportResult.Success(namespaceCount = namespaceCount, schemaVersion = schemaVersion)
            } else {
                ImportResult.Failure("Schema incompatible or invalid format")
            }
        } catch (e: Exception) {
            ImportResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Import from the default backup file (if it exists).
     */
    fun importDefault(ctx: Context): ImportResult {
        val file = File(ctx.filesDir, FILE_BACKUP)
        return if (file.exists()) import(ctx, file)
        else ImportResult.Failure("No backup file found")
    }

    /**
     * Factory reset: clear all data + delete backup files.
     * Returns the number of namespaces cleared.
     */
    fun factoryReset(ctx: Context): Int {
        val cleared = StorageManager.clearAll(ctx)
        // Also clear quick state
        QuickStateStore.clear(ctx)
        // Delete backup files
        File(ctx.filesDir, FILE_BACKUP).delete()
        File(ctx.filesDir, FILE_BACKUP_BAK).delete()
        return cleared
    }

    /** Get info about the current backup file (or null). */
    fun backupInfo(ctx: Context): BackupInfo? {
        val file = File(ctx.filesDir, FILE_BACKUP)
        if (!file.exists()) return null
        return try {
            val json = file.readText(Charsets.UTF_8)
            val root = org.json.JSONObject(json)
            val metadata = root.optJSONObject("metadata")
            BackupInfo(
                file = file,
                byteSize = file.length().toInt(),
                schemaVersion = metadata?.optInt("schemaVersion", 0) ?: 0,
                createdAt = metadata?.optLong("createdAt", 0L) ?: 0L,
                namespaceCount = metadata?.optInt("namespaceCount", 0) ?: 0,
                appVersion = metadata?.optString("appVersion", "unknown") ?: "unknown"
            )
        } catch (e: Exception) { null }
    }

    /** Format a timestamp as a human-readable date string. */
    fun formatTimestamp(ts: Long): String {
        if (ts == 0L) return "—"
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return fmt.format(Date(ts))
    }

    data class BackupInfo(
        val file: File,
        val byteSize: Int,
        val schemaVersion: Int,
        val createdAt: Long,
        val namespaceCount: Int,
        val appVersion: String
    )
}
