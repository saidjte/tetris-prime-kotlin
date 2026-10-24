package com.tetrisprime.kotlin

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 10.2 — quick game-state snapshot.
 *
 * Captures the minimal state needed to resume a game after a crash or
 * app kill: the current board, piece, hold, next queue, score, level,
 * lines, combo, and mode. Stored as a single JSON blob in its own
 * SharedPreferences namespace for fast access.
 *
 * The snapshot is written:
 *   - Every 2 seconds during gameplay (by GameActivity's game loop)
 *   - On every hard drop / line clear
 *   - On onPause()
 *
 * The snapshot is cleared on:
 *   - Game over (after stats are recorded)
 *   - Manual "discard quick state" from settings
 */
object QuickStateStore {

    private const val PREFS = "tetris_prime_quickstate"
    private const val KEY_SNAPSHOT = "snapshot_json"
    private const val KEY_TIMESTAMP = "snapshot_ts"

    data class Snapshot(
        val mode: String,            // GameMode.name
        val score: Long,
        val lines: Int,
        val level: Int,
        val combo: Int,
        val b2bActive: Boolean,
        val holdPiece: String?,     // PieceType.name or null
        val holdUsed: Boolean,
        val nextPieces: List<String>, // List<PieceType.name>
        val boardGrid: Array<Char?>, // width*height, row-major; null = empty
        val boardWidth: Int,
        val boardHeight: Int,
        val elapsedTimeMs: Long,
        val missionIndex: Int,
        val adventureIndex: Int,
        val dailyDate: String?,
        val savedAt: Long = System.currentTimeMillis()
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Save a snapshot. Overwrites any existing snapshot. */
    fun save(ctx: Context, snapshot: Snapshot) {
        val json = JSONObject().apply {
            put("mode", snapshot.mode)
            put("score", snapshot.score)
            put("lines", snapshot.lines)
            put("level", snapshot.level)
            put("combo", snapshot.combo)
            put("b2bActive", snapshot.b2bActive)
            put("holdPiece", snapshot.holdPiece)
            put("holdUsed", snapshot.holdUsed)
            val nextArr = JSONArray()
            snapshot.nextPieces.forEach { nextArr.put(it) }
            put("nextPieces", nextArr)
            val gridArr = JSONArray()
            snapshot.boardGrid.forEach { cell ->
                gridArr.put(if (cell == null) JSONObject.NULL else cell.toString())
            }
            put("boardGrid", gridArr)
            put("boardWidth", snapshot.boardWidth)
            put("boardHeight", snapshot.boardHeight)
            put("elapsedTimeMs", snapshot.elapsedTimeMs)
            put("missionIndex", snapshot.missionIndex)
            put("adventureIndex", snapshot.adventureIndex)
            put("dailyDate", snapshot.dailyDate)
            put("savedAt", snapshot.savedAt)
        }
        prefs(ctx).edit()
            .putString(KEY_SNAPSHOT, json.toString())
            .putLong(KEY_TIMESTAMP, snapshot.savedAt)
            .apply()
    }

    /** Load the saved snapshot, or null if none. */
    fun load(ctx: Context): Snapshot? {
        val raw = prefs(ctx).getString(KEY_SNAPSHOT, null) ?: return null
        return try {
            parse(raw)
        } catch (e: Exception) { null }
    }

    /** True if a snapshot exists. */
    fun exists(ctx: Context): Boolean =
        prefs(ctx).contains(KEY_SNAPSHOT)

    /** Timestamp of the last snapshot (0 if none). */
    fun lastSavedAt(ctx: Context): Long =
        prefs(ctx).getLong(KEY_TIMESTAMP, 0L)

    /** Clear the snapshot (called on game over or manual discard). */
    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    private fun parse(raw: String): Snapshot {
        val root = JSONObject(raw)
        val nextArr = root.getJSONArray("nextPieces")
        val next = (0 until nextArr.length()).map { nextArr.getString(it) }
        val gridArr = root.getJSONArray("boardGrid")
        val grid = (0 until gridArr.length()).map { idx ->
            if (gridArr.isNull(idx)) null else gridArr.getString(idx).first()
        }.toTypedArray()
        return Snapshot(
            mode = root.getString("mode"),
            score = root.getLong("score"),
            lines = root.getInt("lines"),
            level = root.getInt("level"),
            combo = root.getInt("combo"),
            b2bActive = root.getBoolean("b2bActive"),
            holdPiece = if (root.isNull("holdPiece")) null else root.getString("holdPiece"),
            holdUsed = root.getBoolean("holdUsed"),
            nextPieces = next,
            boardGrid = grid,
            boardWidth = root.getInt("boardWidth"),
            boardHeight = root.getInt("boardHeight"),
            elapsedTimeMs = root.getLong("elapsedTimeMs"),
            missionIndex = root.optInt("missionIndex", -1),
            adventureIndex = root.optInt("adventureIndex", -1),
            dailyDate = if (root.isNull("dailyDate")) null else root.optString("dailyDate", null),
            savedAt = root.getLong("savedAt")
        )
    }
}
