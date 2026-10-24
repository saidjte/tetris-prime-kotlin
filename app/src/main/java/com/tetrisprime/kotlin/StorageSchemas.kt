package com.tetrisprime.kotlin

/**
 * Phase 10.3 — versioned storage schema.
 *
 * Tracks the schema version for each storage namespace so that future
 * migrations can detect old data and upgrade it. Matches the original
 * game's `tetris-prime-v4` schema convention.
 *
 * The backup format embeds the schema version so that imports from
 * older versions can be migrated on load.
 */
object StorageSchemas {

    /** Current schema version. Bumped whenever the storage layout changes. */
    const val CURRENT_VERSION: Int = 4  // matches tetris-prime-v4

    /** Backup namespace tag — appended to backup files for identification. */
    const val BACKUP_NAMESPACE: String = "tetris-prime-v4"

    /** Backup namespace for the backup-of-backup (auto-rotate safety). */
    const val BACKUP_NAMESPACE_BAK: String = "tetris-prime-v4:bak"

    /** All storage namespaces managed by the app. */
    val ALL_NAMESPACES: List<String> = listOf(
        "tetris_prime",              // mode-select stats
        "tetris_prime_settings",     // SettingsManager
        "tetris_prime_stats",        // StatsManager
        "tetris_prime_ach",          // AchievementDefinitions
        "tetris_prime_adventure",    // AdventureProgressManager
        "tetris_prime_continue",     // ContinueSystem
        "tetris_prime_daily",        // DailyChallengeManager
        "tetris_prime_giftcodes",    // GiftCodes
        "tetris_prime_layout",       // LayoutStore (InputManager)
        "tetris_prime_leaderboard",  // LeaderboardManager
        "tetris_prime_missions",     // MissionProgressManager
        "tetris_prime_store"         // StoreManager
    )

    /** Schema version per namespace (for future migration logic). */
    val NAMESPACE_VERSIONS: Map<String, Int> = ALL_NAMESPACES.associateWith { 1 }

    /**
     * Metadata header included in every backup file.
     */
    data class BackupMetadata(
        val namespace: String = BACKUP_NAMESPACE,
        val schemaVersion: Int = CURRENT_VERSION,
        val appVersion: String = "1.10.0",
        val createdAt: Long = System.currentTimeMillis(),
        val namespaceCount: Int = ALL_NAMESPACES.size
    )

    /**
     * Check if a backup's schema version is compatible with the current app.
     * Returns true if the backup can be imported (possibly after migration).
     */
    fun isCompatible(backupSchemaVersion: Int): Boolean =
        backupSchemaVersion in 1..CURRENT_VERSION

    /** Human-readable description of the schema. */
    fun describe(): String = buildString {
        append("Schema v$CURRENT_VERSION ($BACKUP_NAMESPACE)\n")
        append("Namespaces: ${ALL_NAMESPACES.size}\n")
        append("  - ${ALL_NAMESPACES.joinToString("\n  - ")}")
    }
}
