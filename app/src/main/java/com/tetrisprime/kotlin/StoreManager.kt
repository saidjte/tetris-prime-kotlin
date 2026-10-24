package com.tetrisprime.kotlin

import android.content.Context

/**
 * Persistent store: currency, owned items, equipped items.
 * Currency formula on game end: max(5, min(300, floor(2*lines + score/500)))
 */
object StoreManager {

    private const val PREFS = "tetris_prime_store"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Current currency balance. */
    fun getCoins(ctx: Context): Int =
        prefs(ctx).getInt("coins", 200)

    /** Add coins (e.g. after a game). Returns new balance. */
    fun addCoins(ctx: Context, amount: Int): Int {
        val newBal = getCoins(ctx) + amount
        prefs(ctx).edit().putInt("coins", newBal).apply()
        return newBal
    }

    /** Spend coins (returns false if insufficient). */
    fun spend(ctx: Context, amount: Int): Boolean {
        val bal = getCoins(ctx)
        if (bal < amount) return false
        prefs(ctx).edit().putInt("coins", bal - amount).apply()
        return true
    }

    /** Compute reward from a finished game. */
    fun computeReward(lines: Int, score: Long): Int =
        maxOf(5, minOf(300, (2 * lines + (score / 500).toInt())))

    // ---- Owned items ----

    private fun ownedKey(category: String) = "owned_$category"

    /** Set of owned item IDs in a category (fx / theme / voice). */
    fun getOwned(ctx: Context, category: String): Set<String> {
        // Default: include free items (price = 0)
        val free: Set<String> = when (category) {
            "fx" -> setOf("classic")
            "theme" -> ThemeDefinitions.ALL_THEMES.filter { it.price == 0 }.map { it.id }.toSet()
            "voice" -> setOf("raffina")
            else -> emptySet()
        }
        val stored = prefs(ctx).getStringSet(ownedKey(category), emptySet()) ?: emptySet()
        return free + stored
    }

    fun addOwned(ctx: Context, category: String, id: String) {
        val cur = getOwned(ctx, category).toMutableSet()
        cur.add(id)
        prefs(ctx).edit().putStringSet(ownedKey(category), cur).apply()
    }

    /** Try to buy an item. Returns true on success, false if not enough coins or already owned. */
    fun buy(ctx: Context, category: String, id: String, price: Int): Boolean {
        if (getOwned(ctx, category).contains(id)) return false
        if (!spend(ctx, price)) return false
        addOwned(ctx, category, id)
        return true
    }

    // ---- Equipped items ----

    fun getEquipped(ctx: Context, category: String): String =
        prefs(ctx).getString("equipped_$category", defaultEquipped(category)) ?: defaultEquipped(category)

    fun setEquipped(ctx: Context, category: String, id: String) {
        // Only allow equipping owned items
        if (!getOwned(ctx, category).contains(id)) return
        prefs(ctx).edit().putString("equipped_$category", id).apply()
    }

    private fun defaultEquipped(category: String): String = when (category) {
        "fx" -> "classic"
        "theme" -> "classic"
        "voice" -> "raffina"
        else -> ""
    }
}
