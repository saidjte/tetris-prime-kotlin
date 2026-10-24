package com.tetrisprime.kotlin

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 10.1 — unified storage facade.
 *
 * Provides a single entry point for reading/writing all app data.
 * Behind the scenes, data lives in multiple SharedPreferences namespaces
 * (kept for backward compatibility with Phase 6-9 managers), but this
 * facade exposes:
 *
 *   - exportAll()  → JSON snapshot of every namespace (for backup)
 *   - importAll()  → restore from a JSON snapshot (with schema check)
 *   - clearAll()   → factory reset
 *   - namespaceSnapshot(name) → per-namespace export
 *
 * This is the foundation that BackupManager and BackupRestoreActivity build on.
 */
object StorageManager {

    /**
     * Export all namespaces as a single JSON object.
     * Format:
     * {
     *   "metadata": { ... StorageSchemas.BackupMetadata ... },
     *   "namespaces": {
     *     "tetris_prime_settings": { "key1": "value1", ... },
     *     "tetris_prime_stats": { ... },
     *     ...
     *   }
     * }
     */
    fun exportAll(ctx: Context): String {
        val metadata = JSONObject().apply {
            put("namespace", StorageSchemas.BACKUP_NAMESPACE)
            put("schemaVersion", StorageSchemas.CURRENT_VERSION)
            put("appVersion", "1.10.0")
            put("createdAt", System.currentTimeMillis())
            put("namespaceCount", StorageSchemas.ALL_NAMESPACES.size)
        }

        val namespaces = JSONObject()
        for (ns in StorageSchemas.ALL_NAMESPACES) {
            namespaces.put(ns, exportNamespace(ctx, ns))
        }

        return JSONObject().apply {
            put("metadata", metadata)
            put("namespaces", namespaces)
        }.toString(2)
    }

    /**
     * Export a single namespace as a JSON object of key→value.
     * SharedPreferences stores all values as their string representation,
     * so we preserve the type via the getAll() map.
     */
    fun exportNamespace(ctx: Context, namespace: String): JSONObject {
        val prefs = ctx.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        val all = prefs.all
        val obj = JSONObject()
        for ((key, value) in all) {
            when (value) {
                is Boolean -> obj.put(key, value)
                is Int     -> obj.put(key, value)
                is Long    -> obj.put(key, value)
                is Float   -> obj.put(key, value)
                is String  -> obj.put(key, value)
                is Set<*>  -> {
                    val arr = JSONArray()
                    value.forEach { arr.put(it.toString()) }
                    obj.put(key, arr)
                }
                null       -> { /* skip nulls */ }
                else       -> obj.put(key, value.toString())
            }
        }
        return obj
    }

    /**
     * Import a previously-exported JSON snapshot.
     * Returns true on success, false on schema incompatibility.
     *
     * Existing data in each namespace is CLEARED before import.
     */
    fun importAll(ctx: Context, json: String): Boolean {
        val root = JSONObject(json)
        val metadata = root.optJSONObject("metadata") ?: return false
        val schemaVersion = metadata.optInt("schemaVersion", 0)
        if (!StorageSchemas.isCompatible(schemaVersion)) return false

        val namespaces = root.optJSONObject("namespaces") ?: return false
        for (ns in StorageSchemas.ALL_NAMESPACES) {
            val nsData = namespaces.optJSONObject(ns) ?: continue
            importNamespace(ctx, ns, nsData)
        }
        return true
    }

    /**
     * Import a single namespace from a JSON object.
     * Clears existing data first, then writes each key with type inference.
     */
    fun importNamespace(ctx: Context, namespace: String, data: JSONObject) {
        val prefs = ctx.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        val editor = prefs.edit().clear()
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = data.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int     -> editor.putInt(key, value)
                is Long    -> editor.putLong(key, value)
                is Double  -> editor.putFloat(key, value.toFloat())
                is Float   -> editor.putFloat(key, value)
                is String  -> editor.putString(key, value)
                is JSONArray -> {
                    val set = (0 until value.length()).map { value.getString(it) }.toSet()
                    editor.putStringSet(key, set)
                }
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
    }

    /**
     * Clear ALL app data (factory reset).
     * Returns the number of namespaces cleared.
     */
    fun clearAll(ctx: Context): Int {
        var cleared = 0
        for (ns in StorageSchemas.ALL_NAMESPACES) {
            ctx.getSharedPreferences(ns, Context.MODE_PRIVATE).edit().clear().apply()
            cleared++
        }
        return cleared
    }

    /**
     * Clear a single namespace.
     */
    fun clearNamespace(ctx: Context, namespace: String) {
        ctx.getSharedPreferences(namespace, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Get a snapshot of a namespace's size (key count) and approximate byte size.
     */
    fun namespaceInfo(ctx: Context, namespace: String): NamespaceInfo {
        val prefs = ctx.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        val all = prefs.all
        val keyCount = all.size
        val byteSize = all.entries.sumOf { (k, v) ->
            k.length + (v?.toString()?.length ?: 0)
        }
        return NamespaceInfo(name = namespace, keyCount = keyCount, byteSize = byteSize)
    }

    data class NamespaceInfo(
        val name: String,
        val keyCount: Int,
        val byteSize: Int
    )

    /**
     * Get info for all namespaces (for the storage UI).
     */
    fun allNamespaceInfo(ctx: Context): List<NamespaceInfo> =
        StorageSchemas.ALL_NAMESPACES.map { namespaceInfo(ctx, it) }

    /**
     * Total byte size across all namespaces.
     */
    fun totalByteSize(ctx: Context): Int =
        allNamespaceInfo(ctx).sumOf { it.byteSize }
}
