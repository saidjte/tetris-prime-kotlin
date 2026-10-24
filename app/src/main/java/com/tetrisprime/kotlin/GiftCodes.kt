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

    /** Built-in registry of valid gift codes. */
    private val REGISTRY: List<GiftCode> = listOf(
        GiftCode("WELCOME200", "ترحيب — 200 عملة", Reward.Coins(200)),
        GiftCode("TETRIS500", "تيتريس — 500 عملة", Reward.Coins(500)),
        GiftCode("PRIME1000", "برايم — 1000 عملة", Reward.Coins(1000)),
        GiftCode("DAILY100", "يومي — 100 عملة", Reward.Coins(100)),
        GiftCode("STORM1500", "عاصفة — 1500 عملة", Reward.Coins(1500)),
        GiftCode("PHOENIXFX", "حزمة جسيمات العنقاء", Reward.FxPack("phoenix")),
        GiftCode("AURORAFREE", "ثيم الشفق مجاناً", Reward.Theme("aurora")),
        GiftCode("HEROVOICE", "صوت البطل", Reward.VoicePack("hero")),
        GiftCode("SAGEVOICE", "صوت الحكيم", Reward.VoicePack("sage")),
        GiftCode("RAINBOWFX", "حزمة قوس قزح", Reward.FxPack("rainbow"))
    )

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

        val match = REGISTRY.find { it.code == normalized }
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
