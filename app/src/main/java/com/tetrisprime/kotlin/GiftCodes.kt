package com.tetrisprime.kotlin

import android.content.Context

/**
 * Phase 8.8 — Gift code system.
 *
 * Codes are validated locally (no server) using a built-in registry.
 * Each code can be redeemed once per device. Codes grant coins, items,
 * or unlockables.
 *
 * Code format: 8-12 alphanumeric characters, case-insensitive.
 */
object GiftCodes {

    private const val PREFS = "tetris_prime_giftcodes"
    private const val KEY_REDEEMED = "redeemed_set"

    /** Reward types. */
    sealed class Reward {
        data class Coins(val amount: Int) : Reward()
        data class FxPack(val id: String) : Reward()
        data class Theme(val id: String) : Reward()
        data class VoicePack(val id: String) : Reward()
    }

    /** A gift code definition. */
    data class GiftCode(
        val code: String,
        val description: String,
        val reward: Reward,
        val expiresOn: String? = null  // YYYY-MM-DD, null = never expires
    )

    /** Built-in registry — loaded from assets/www/gift-codes.json (mirrors original app). */
    private val REGISTRY: List<GiftCode> by lazy { loadBundledCodes() }

    private fun loadBundledCodes(): List<GiftCode> {
        // We can't access Context from a lazy singleton init, so we use a
        // thread-local app-context holder set by Application.onCreate.
        // Fallback: empty list — GiftBridge will provide codes via network.
        return try {
            val ctx = AppContextHolder.context
            if (ctx == null) emptyList() else parseBundled(ctx)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBundled(ctx: Context): List<GiftCode> {
        val json = ctx.assets.open("www/gift-codes.json").bufferedReader().use { it.readText() }
        val root = org.json.JSONObject(json)
        val arr = root.optJSONArray("codes") ?: return emptyList()
        val out = mutableListOf<GiftCode>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val code = c.getString("code")
            val coins = c.getInt("coins")
            val note = c.optString("note", "هدية")
            out.add(GiftCode(code, note, Reward.Coins(coins)))
        }
        return out
    }

    /** Refresh registry from network (called by GiftBridge.httpGet callback). */
    fun updateRegistry(codes: List<GiftCode>) {
        // Network codes supplement bundled ones — caller should call redeem() after refresh
        synchronized(this) {
            _networkRegistry = codes
        }
    }
    @Volatile private var _networkRegistry: List<GiftCode> = emptyList()

    /** All known codes (bundled + network-refreshed). */
    fun allCodes(): List<GiftCode> = REGISTRY + _networkRegistry

    /** Result of a redeem attempt. */
    sealed class RedeemResult {
        data class Success(val code: GiftCode, val reward: Reward) : RedeemResult()
        data class Failure(val reason: String) : RedeemResult()
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Load the set of already-redeemed codes (uppercase). */
    private fun loadRedeemed(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_REDEEMED, emptySet()) ?: emptySet()

    /** Mark a code as redeemed. */
    private fun markRedeemed(ctx: Context, code: String) {
        val cur = loadRedeemed(ctx).toMutableSet()
        cur.add(code.uppercase())
        prefs(ctx).edit().putStringSet(KEY_REDEEMED, cur).apply()
    }

    /**
     * Try to redeem a code.
     * Returns Success (with reward applied) or Failure (with reason).
     */
    fun redeem(ctx: Context, input: String): RedeemResult {
        val normalized = input.trim().uppercase()
        if (normalized.length !in 6..16) {
            return RedeemResult.Failure("الطول غير صحيح")
        }
        if (!normalized.all { it.isLetterOrDigit() }) {
            return RedeemResult.Failure("رموز غير صالحة")
        }

        val match = allCodes().find { it.code == normalized }
            ?: return RedeemResult.Failure("الرمز غير معروف")

        // Check expiry
        if (match.expiresOn != null) {
            try {
                val expiry = java.time.LocalDate.parse(match.expiresOn)
                if (java.time.LocalDate.now().isAfter(expiry)) {
                    return RedeemResult.Failure("انتهت صلاحية الرمز")
                }
            } catch (e: Exception) {
                // ignore parse errors
            }
        }

        // Check if already redeemed
        if (loadRedeemed(ctx).contains(normalized)) {
            return RedeemResult.Failure("تم استخدام هذا الرمز من قبل")
        }

        // Apply reward
        when (match.reward) {
            is Reward.Coins -> StoreManager.addCoins(ctx, match.reward.amount)
            is Reward.FxPack -> {
                StoreManager.addOwned(ctx, "fx", match.reward.id)
                StoreManager.setEquipped(ctx, "fx", match.reward.id)
            }
            is Reward.Theme -> {
                StoreManager.addOwned(ctx, "theme", match.reward.id)
                StoreManager.setEquipped(ctx, "theme", match.reward.id)
            }
            is Reward.VoicePack -> {
                StoreManager.addOwned(ctx, "voice", match.reward.id)
                StoreManager.setEquipped(ctx, "voice", match.reward.id)
            }
        }

        markRedeemed(ctx, normalized)
        return RedeemResult.Success(match, match.reward)
    }

    /** Number of codes redeemed so far. */
    fun redeemedCount(ctx: Context): Int = loadRedeemed(ctx).size

    /** Friendly description of a reward. */
    fun rewardLabel(reward: Reward): String = when (reward) {
        is Reward.Coins -> "+${reward.amount} عملة"
        is Reward.FxPack -> "حزمة جسيمات: ${reward.id}"
        is Reward.Theme -> "ثيم: ${reward.id}"
        is Reward.VoicePack -> "صوت: ${reward.id}"
    }
}
