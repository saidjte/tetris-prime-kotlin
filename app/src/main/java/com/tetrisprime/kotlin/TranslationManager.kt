package com.tetrisprime.kotlin

import android.content.Context
import java.util.Locale

/**
 * Phase 9 — i18n system.
 *
 * 16 languages with full RTL support for ar, fa, ur.
 *
 * Usage:
 *   val t = TranslationManager.t  // current language translator
 *   textView.text = t("mode.classic")
 *
 *   // or with explicit language:
 *   TranslationManager.tFor("fr")("mode.classic")
 *
 * The active language is read from SettingsManager.lang. When set to "auto",
 * the system locale is used (with fallback to English).
 */
object TranslationManager {

    /** All supported language codes. */
    val SUPPORTED_LANGUAGES: List<String> = listOf(
        "ar",    // العربية (RTL)
        "en",    // English
        "fr",    // Français
        "es",    // Español
        "ko",    // 한국어
        "hi",    // हिन्दी
        "ru",    // Русский
        "zh",    // 中文
        "tr",    // Türkçe
        "fa",    // فارسی (RTL)
        "es-MX", // Español (México)
        "it",    // Italiano
        "de",    // Deutsch
        "nl",    // Nederlands
        "de-CH", // Schwiizerdütsch
        "ur"     // اردو (RTL)
    )

    /** RTL language codes. */
    val RTL_LANGUAGES: Set<String> = setOf("ar", "fa", "ur")

    /** Native display name for each language (shown in language picker). */
    val NATIVE_NAMES: Map<String, String> = mapOf(
        "ar"    to "العربية",
        "en"    to "English",
        "fr"    to "Français",
        "es"    to "Español",
        "ko"    to "한국어",
        "hi"    to "हिन्दी",
        "ru"    to "Русский",
        "zh"    to "中文",
        "tr"    to "Türkçe",
        "fa"    to "فارسی",
        "es-MX" to "Español (México)",
        "it"    to "Italiano",
        "de"    to "Deutsch",
        "nl"    to "Nederlands",
        "de-CH" to "Schwiizerdütsch",
        "ur"    to "اردو"
    )

    /** English flag emoji for each language (for picker display). */
    val FLAGS: Map<String, String> = mapOf(
        "ar"    to "🇸🇦",
        "en"    to "🇬🇧",
        "fr"    to "🇫🇷",
        "es"    to "🇪🇸",
        "ko"    to "🇰🇷",
        "hi"    to "🇮🇳",
        "ru"    to "🇷🇺",
        "zh"    to "🇨🇳",
        "tr"    to "🇹🇷",
        "fa"    to "🇮🇷",
        "es-MX" to "🇲🇽",
        "it"    to "🇮🇹",
        "de"    to "🇩🇪",
        "nl"    to "🇳🇱",
        "de-CH" to "🇨🇭",
        "ur"    to "🇵🇰"
    )

    /** True if the language is right-to-left. */
    fun isRtl(lang: String): Boolean = lang in RTL_LANGUAGES

    /**
     * Resolve the effective language code.
     * "auto" → system locale → fallback to "en".
     */
    fun resolveLanguage(setting: String): String {
        if (setting != "auto") return setting
        val sys = Locale.getDefault().language
        // Match exact code first, then language prefix
        if (sys in SUPPORTED_LANGUAGES) return sys
        // Try matching language prefix (e.g., "es-AR" → "es")
        val match = SUPPORTED_LANGUAGES.firstOrNull { it.startsWith(sys) }
        return match ?: "en"
    }

    // ---- Translation tables ----
    // English is the canonical key source. Other languages fall back to English
    // for missing keys.

    /** All translation keys. */
    object Keys {
        // App
        const val APP_NAME = "app.name"

        // Mode select
        const val MODE_SELECT_TITLE   = "modeSelect.title"
        const val MODE_CLASSIC        = "mode.classic"
        const val MODE_SPRINT         = "mode.sprint"
        const val MODE_ULTRA          = "mode.ultra"
        const val MODE_ZEN            = "mode.zen"
        const val MODE_CHEESE         = "mode.cheese"
        const val MODE_MISSION        = "mode.mission"
        const val MODE_ADVENTURE      = "mode.adventure"
        const val MODE_BATTLE         = "mode.battle"
        const val MODE_DAILY          = "mode.daily"

        // Mode descriptions
        const val DESC_CLASSIC   = "desc.classic"
        const val DESC_SPRINT    = "desc.sprint"
        const val DESC_ULTRA     = "desc.ultra"
        const val DESC_ZEN       = "desc.zen"
        const val DESC_CHEESE    = "desc.cheese"
        const val DESC_MISSION   = "desc.mission"
        const val DESC_ADVENTURE = "desc.adventure"
        const val DESC_BATTLE    = "desc.battle"
        const val DESC_DAILY     = "desc.daily"

        // Mode icons
        const val ICON_CLASSIC = "icon.classic"
        const val ICON_SPRINT  = "icon.sprint"
        const val ICON_ULTRA   = "icon.ultra"
        const val ICON_ZEN     = "icon.zen"
        const val ICON_CHEESE  = "icon.cheese"
        const val ICON_MISSION = "icon.mission"
        const val ICON_ADVENTURE = "icon.adventure"
        const val ICON_BATTLE  = "icon.battle"
        const val ICON_DAILY   = "icon.daily"

        // Stats
        const val STATS_TITLE = "stats.title"
        const val STATS_GAMES = "stats.games"
        const val STATS_TIME = "stats.time"
        const val STATS_PIECES = "stats.pieces"
        const val STATS_LINES = "stats.lines"
        const val STATS_SCORE = "stats.score"

        // Settings
        const val SETTINGS_TITLE = "settings.title"
        const val SETTINGS_AUDIO = "settings.audio"
        const val SETTINGS_VISUALS = "settings.visuals"
        const val SETTINGS_GAMEPLAY = "settings.gameplay"
        const val SETTINGS_LANGUAGE = "settings.language"
        const val SETTINGS_MUSIC = "settings.music"
        const val SETTINGS_SFX = "settings.sfx"
        const val SETTINGS_VOICE = "settings.voice"
        const val SETTINGS_CAPTIONS = "settings.captions"
        const val SETTINGS_FX = "settings.fx"
        const val SETTINGS_GESTURES = "settings.gestures"
        const val SETTINGS_NEXT5 = "settings.next5"
        const val SETTINGS_GARBAGE = "settings.garbage"
        const val SETTINGS_BATTLE_LEVEL = "settings.battleLevel"
        const val SETTINGS_GAMEPAD = "settings.gamepad"
        const val SETTINGS_PIECE_SYMBOLS = "settings.pieceSymbols"
        const val SETTINGS_ALLOW_ROTATION = "settings.allowRotation"
        const val SETTINGS_HIDE_BUTTONS = "settings.hideButtons"
        const val SETTINGS_CUSTOMIZE_BUTTONS = "settings.customizeButtons"

        // Buttons / common
        const val BTN_DONE = "btn.done"
        const val BTN_CANCEL = "btn.cancel"
        const val BTN_OK = "btn.ok"
        const val BTN_RESET = "btn.reset"
        const val BTN_REDEEM = "btn.redeem"
        const val BTN_SKIP = "btn.skip"
        const val BTN_NEXT = "btn.next"
        const val BTN_PREV = "btn.prev"
        const val BTN_FINISH = "btn.finish"
        const val BTN_BUY = "btn.buy"
        const val BTN_EQUIP = "btn.equip"
        const val BTN_EQUIPPED = "btn.equipped"

        // Game over / continue
        const val GAME_OVER = "gameOver"
        const val CONTINUE_TITLE = "continue.title"
        const val CONTINUE_AD = "continue.ad"
        const val CONTINUE_COINS = "continue.coins"
        const val CONTINUE_FREE = "continue.free"
        const val CONTINUE_INSUFFICIENT = "continue.insufficient"

        // Store
        const val STORE_TITLE = "store.title"
        const val STORE_TAB_FX = "store.tab.fx"
        const val STORE_TAB_THEME = "store.tab.theme"
        const val STORE_TAB_VOICE = "store.tab.voice"
        const val STORE_COINS = "store.coins"

        // Achievements
        const val ACH_TITLE = "ach.title"
        const val ACH_UNLOCKED = "ach.unlocked"

        // Leaderboard
        const val LB_TITLE = "lb.title"
        const val LB_RANK = "lb.rank"
        const val LB_SCORE = "lb.score"
        const val LB_LINES = "lb.lines"
        const val LB_LEVEL = "lb.level"
        const val LB_EMPTY = "lb.empty"

        // Tutorial
        const val TUT_TITLE = "tut.title"

        // Missions
        const val MISSIONS_TITLE = "missions.title"
        const val MISSIONS_COMPLETED = "missions.completed"

        // Adventure
        const val ADVENTURE_TITLE = "adventure.title"
        const val ADVENTURE_STAGES = "adventure.stages"
        const val ADVENTURE_LOCKED = "adventure.locked"

        // Daily
        const val DAILY_GOAL = "daily.goal"
        const val DAILY_STREAK = "daily.streak"

        // Gift codes
        const val GIFT_TITLE = "gift.title"
        const val GIFT_HINT = "gift.hint"
        const val GIFT_SUCCESS = "gift.success"
        const val GIFT_FAILURE = "gift.failure"
        const val GIFT_REDEEMED_COUNT = "gift.redeemedCount"

        // Input
        const val INPUT_TITLE = "input.title"
        const val INPUT_HINT = "input.hint"
        const val INPUT_SIZE = "input.size"
        const val INPUT_VISIBILITY = "input.visibility"
        const val INPUT_RESET_DEFAULT = "input.resetDefault"

        // Misc
        const val PAUSED = "paused"
        const val COUNTDOWN = "countdown"
    }

    /** English translations (canonical). */
    private val EN: Map<String, String> = mapOf(
        Keys.APP_NAME to "Tetris Prime Kotlin",
        Keys.MODE_SELECT_TITLE to "Select Game Mode",
        Keys.MODE_CLASSIC to "Classic",
        Keys.MODE_SPRINT to "Sprint 40",
        Keys.MODE_ULTRA to "Ultra",
        Keys.MODE_ZEN to "Zen",
        Keys.MODE_CHEESE to "Cheese",
        Keys.MODE_MISSION to "Missions",
        Keys.MODE_ADVENTURE to "Adventure",
        Keys.MODE_BATTLE to "Battle",
        Keys.MODE_DAILY to "Daily",
        Keys.DESC_CLASSIC to "Endless marathon with progressive speed",
        Keys.DESC_SPRINT to "Clear 40 lines as fast as possible",
        Keys.DESC_ULTRA to "2 minutes for the highest score",
        Keys.DESC_ZEN to "Relax with no speed-up",
        Keys.DESC_CHEESE to "Clear the cheese cells",
        Keys.DESC_MISSION to "100 progressive missions",
        Keys.DESC_ADVENTURE to "36 stages across 6 worlds",
        Keys.DESC_BATTLE to "Face the AI",
        Keys.DESC_DAILY to "Daily seeded challenge",
        Keys.ICON_CLASSIC to "🎮", Keys.ICON_SPRINT to "🏃", Keys.ICON_ULTRA to "⚡",
        Keys.ICON_ZEN to "🧘", Keys.ICON_CHEESE to "🧀", Keys.ICON_MISSION to "🎯",
        Keys.ICON_ADVENTURE to "🗺️", Keys.ICON_BATTLE to "🤖", Keys.ICON_DAILY to "📅",
        Keys.STATS_TITLE to "📊 Statistics",
        Keys.STATS_GAMES to "Games", Keys.STATS_TIME to "Total Time",
        Keys.STATS_PIECES to "Pieces Placed", Keys.STATS_LINES to "Lines Cleared",
        Keys.STATS_SCORE to "Total Score",
        Keys.SETTINGS_TITLE to "⚙️ Settings",
        Keys.SETTINGS_AUDIO to "Audio", Keys.SETTINGS_VISUALS to "Visuals",
        Keys.SETTINGS_GAMEPLAY to "Gameplay", Keys.SETTINGS_LANGUAGE to "Language",
        Keys.SETTINGS_MUSIC to "Music", Keys.SETTINGS_SFX to "Sound Effects",
        Keys.SETTINGS_VOICE to "Voice Commentary", Keys.SETTINGS_CAPTIONS to "Captions",
        Keys.SETTINGS_FX to "FX Level",
        Keys.SETTINGS_GESTURES to "Gestures (Swipe)",
        Keys.SETTINGS_NEXT5 to "Show 5 Next Pieces",
        Keys.SETTINGS_GARBAGE to "Show Garbage",
        Keys.SETTINGS_BATTLE_LEVEL to "Battle Difficulty",
        Keys.SETTINGS_GAMEPAD to "Gamepad Support",
        Keys.SETTINGS_PIECE_SYMBOLS to "Piece Symbols",
        Keys.SETTINGS_ALLOW_ROTATION to "Allow Impossible Rotation",
        Keys.SETTINGS_HIDE_BUTTONS to "Hide Control Buttons",
        Keys.SETTINGS_CUSTOMIZE_BUTTONS to "🎮 Customize Buttons",
        Keys.BTN_DONE to "Done", Keys.BTN_CANCEL to "Cancel", Keys.BTN_OK to "OK",
        Keys.BTN_RESET to "Reset", Keys.BTN_REDEEM to "Redeem", Keys.BTN_SKIP to "Skip",
        Keys.BTN_NEXT to "Next", Keys.BTN_PREV to "Previous", Keys.BTN_FINISH to "Finish",
        Keys.BTN_BUY to "Buy", Keys.BTN_EQUIP to "Equip", Keys.BTN_EQUIPPED to "Equipped",
        Keys.GAME_OVER to "Game Over",
        Keys.CONTINUE_TITLE to "Continue?",
        Keys.CONTINUE_AD to "Free Ad",
        Keys.CONTINUE_COINS to "Coins",
        Keys.CONTINUE_FREE to "Clear 10 Rows",
        Keys.CONTINUE_INSUFFICIENT to "Insufficient coins",
        Keys.STORE_TITLE to "🛒 Store",
        Keys.STORE_TAB_FX to "FX", Keys.STORE_TAB_THEME to "Themes", Keys.STORE_TAB_VOICE to "Voices",
        Keys.STORE_COINS to "Coins",
        Keys.ACH_TITLE to "🏆 Achievements",
        Keys.ACH_UNLOCKED to "Unlocked",
        Keys.LB_TITLE to "🥇 Leaderboard",
        Keys.LB_RANK to "#", Keys.LB_SCORE to "Score", Keys.LB_LINES to "Lines", Keys.LB_LEVEL to "Level",
        Keys.LB_EMPTY to "No scores yet — play to set a record!",
        Keys.TUT_TITLE to "📚 Tutorial",
        Keys.MISSIONS_TITLE to "🎯 Missions",
        Keys.MISSIONS_COMPLETED to "Completed",
        Keys.ADVENTURE_TITLE to "🗺️ Adventure",
        Keys.ADVENTURE_STAGES to "Stages",
        Keys.ADVENTURE_LOCKED to "Locked",
        Keys.DAILY_GOAL to "Today's Goal",
        Keys.DAILY_STREAK to "Streak",
        Keys.GIFT_TITLE to "🎁 Gift Codes",
        Keys.GIFT_HINT to "Enter a code to receive your reward",
        Keys.GIFT_SUCCESS to "Success!",
        Keys.GIFT_FAILURE to "Failed",
        Keys.GIFT_REDEEMED_COUNT to "Codes Redeemed",
        Keys.INPUT_TITLE to "🎮 Customize Buttons",
        Keys.INPUT_HINT to "Drag any button to move it. Select a button to resize it.",
        Keys.INPUT_SIZE to "Size",
        Keys.INPUT_VISIBILITY to "Visibility",
        Keys.INPUT_RESET_DEFAULT to "Reset to Default",
        Keys.PAUSED to "Paused",
        Keys.COUNTDOWN to "Count"
    )

    /** Arabic translations (RTL). */
    private val AR: Map<String, String> = mapOf(
        Keys.APP_NAME to "تيتبرايم كوتلن",
        Keys.MODE_SELECT_TITLE to "اختر نمط اللعب",
        Keys.MODE_CLASSIC to "كلاسيكي",
        Keys.MODE_SPRINT to "سبرينت ٤٠",
        Keys.MODE_ULTRA to "ألترا",
        Keys.MODE_ZEN to "زن",
        Keys.MODE_CHEESE to "جبن",
        Keys.MODE_MISSION to "مهمات",
        Keys.MODE_ADVENTURE to "مغامرة",
        Keys.MODE_BATTLE to "معركة",
        Keys.MODE_DAILY to "يومي",
        Keys.DESC_CLASSIC to "ماراثون بلا نهاية بتسارع تدريجي",
        Keys.DESC_SPRINT to "امسح ٤٠ خطًا بأسرع وقت",
        Keys.DESC_ULTRA to "دقيقتان لتحقيق أعلى نتيجة",
        Keys.DESC_ZEN to "استرخاء بلا تسارع",
        Keys.DESC_CHEESE to "امسح خلايا الجبن",
        Keys.DESC_MISSION to "١٠٠ مهمة متدرجة",
        Keys.DESC_ADVENTURE to "٣٦ مرحلة عبر ٦ عوالم",
        Keys.DESC_BATTLE to "مواجهة الذكاء الاصطناعي",
        Keys.DESC_DAILY to "تحدي يومي محدد",
        Keys.ICON_CLASSIC to "🎮", Keys.ICON_SPRINT to "🏃", Keys.ICON_ULTRA to "⚡",
        Keys.ICON_ZEN to "🧘", Keys.ICON_CHEESE to "🧀", Keys.ICON_MISSION to "🎯",
        Keys.ICON_ADVENTURE to "🗺️", Keys.ICON_BATTLE to "🤖", Keys.ICON_DAILY to "📅",
        Keys.STATS_TITLE to "📊 الإحصائيات",
        Keys.STATS_GAMES to "ألعاب", Keys.STATS_TIME to "الوقت الكلي",
        Keys.STATS_PIECES to "القطع الموضوعة", Keys.STATS_LINES to "الخطوط الممحوة",
        Keys.STATS_SCORE to "إجمالي النقاط",
        Keys.SETTINGS_TITLE to "⚙️ الإعدادات",
        Keys.SETTINGS_AUDIO to "الصوت", Keys.SETTINGS_VISUALS to "المرئيات",
        Keys.SETTINGS_GAMEPLAY to "اللعب", Keys.SETTINGS_LANGUAGE to "اللغة",
        Keys.SETTINGS_MUSIC to "الموسيقى", Keys.SETTINGS_SFX to "المؤثرات الصوتية",
        Keys.SETTINGS_VOICE to "المعلق الصوتي", Keys.SETTINGS_CAPTIONS to "الترجمة النصية",
        Keys.SETTINGS_FX to "مستوى FX",
        Keys.SETTINGS_GESTURES to "الإيماءات (Swipe)",
        Keys.SETTINGS_NEXT5 to "عرض 5 قطع قادمة",
        Keys.SETTINGS_GARBAGE to "إظهار القمامة",
        Keys.SETTINGS_BATTLE_LEVEL to "صعوبة المعركة",
        Keys.SETTINGS_GAMEPAD to "دعم يد التحكم",
        Keys.SETTINGS_PIECE_SYMBOLS to "رموز القطع",
        Keys.SETTINGS_ALLOW_ROTATION to "السماح بالدوران المستحيل",
        Keys.SETTINGS_HIDE_BUTTONS to "إخفاء أزرار التحكم",
        Keys.SETTINGS_CUSTOMIZE_BUTTONS to "🎮 تخصيص الأزرار",
        Keys.BTN_DONE to "تم", Keys.BTN_CANCEL to "إلغاء", Keys.BTN_OK to "حسناً",
        Keys.BTN_RESET to "استعادة", Keys.BTN_REDEEM to "استرداد", Keys.BTN_SKIP to "تخطّي",
        Keys.BTN_NEXT to "التالي", Keys.BTN_PREV to "السابق", Keys.BTN_FINISH to "إنهاء",
        Keys.BTN_BUY to "شراء", Keys.BTN_EQUIP to "تفعيل", Keys.BTN_EQUIPPED to "مفعّل",
        Keys.GAME_OVER to "انتهت اللعبة",
        Keys.CONTINUE_TITLE to "استمرار؟",
        Keys.CONTINUE_AD to "إعلان مجاني",
        Keys.CONTINUE_COINS to "عملات",
        Keys.CONTINUE_FREE to "مسح 10 صفوف",
        Keys.CONTINUE_INSUFFICIENT to "عملات غير كافية",
        Keys.STORE_TITLE to "🛒 المتجر",
        Keys.STORE_TAB_FX to "الجسيمات", Keys.STORE_TAB_THEME to "الثيمات", Keys.STORE_TAB_VOICE to "المعلقون",
        Keys.STORE_COINS to "عملات",
        Keys.ACH_TITLE to "🏆 الإنجازات",
        Keys.ACH_UNLOCKED to "مفتوح",
        Keys.LB_TITLE to "🥇 لوحة الصدارة",
        Keys.LB_RANK to "#", Keys.LB_SCORE to "النتيجة", Keys.LB_LINES to "خطوط", Keys.LB_LEVEL to "مستوى",
        Keys.LB_EMPTY to "لا توجد نتائج بعد — العب لِتُسجّل رقماً!",
        Keys.TUT_TITLE to "📚 الشرح",
        Keys.MISSIONS_TITLE to "🎯 المهمات",
        Keys.MISSIONS_COMPLETED to "مكتمل",
        Keys.ADVENTURE_TITLE to "🗺️ المغامرة",
        Keys.ADVENTURE_STAGES to "مراحل",
        Keys.ADVENTURE_LOCKED to "مقفل",
        Keys.DAILY_GOAL to "تحدي اليوم",
        Keys.DAILY_STREAK to "السلسلة",
        Keys.GIFT_TITLE to "🎁 أكواد الهدية",
        Keys.GIFT_HINT to "أدخل رمزاً لاستلام المكافأة",
        Keys.GIFT_SUCCESS to "نجح!",
        Keys.GIFT_FAILURE to "فشل",
        Keys.GIFT_REDEEMED_COUNT to "الرموز المستخدمة",
        Keys.INPUT_TITLE to "🎮 تخصيص الأزرار",
        Keys.INPUT_HINT to "اسحب أي زر لتغيير موضعه. اختر زراً لضبط حجمه.",
        Keys.INPUT_SIZE to "الحجم",
        Keys.INPUT_VISIBILITY to "الإظهار",
        Keys.INPUT_RESET_DEFAULT to "استعادة الافتراضي",
        Keys.PAUSED to "إيقاف مؤقت",
        Keys.COUNTDOWN to "عدد"
    )

    /** French translations. */
    private val FR: Map<String, String> = mapOf(
        Keys.APP_NAME to "Tetris Prime Kotlin",
        Keys.MODE_SELECT_TITLE to "Choisir le mode de jeu",
        Keys.MODE_CLASSIC to "Classique", Keys.MODE_SPRINT to "Sprint 40",
        Keys.MODE_ULTRA to "Ultra", Keys.MODE_ZEN to "Zen", Keys.MODE_CHEESE to "Fromage",
        Keys.MODE_MISSION to "Missions", Keys.MODE_ADVENTURE to "Aventure",
        Keys.MODE_BATTLE to "Bataille", Keys.MODE_DAILY to "Quotidien",
        Keys.DESC_CLASSIC to "Marathon sans fin avec vitesse progressive",
        Keys.DESC_SPRINT to "Effacez 40 lignes le plus vite possible",
        Keys.DESC_ULTRA to "2 minutes pour le meilleur score",
        Keys.DESC_ZEN to "Détente sans accélération",
        Keys.DESC_CHEESE to "Effacez les cellules de fromage",
        Keys.DESC_MISSION to "100 missions progressives",
        Keys.DESC_ADVENTURE to "36 étapes sur 6 mondes",
        Keys.DESC_BATTLE to "Affrontez l'IA",
        Keys.DESC_DAILY to "Défi quotidien déterministe",
        Keys.ICON_CLASSIC to "🎮", Keys.ICON_SPRINT to "🏃", Keys.ICON_ULTRA to "⚡",
        Keys.ICON_ZEN to "🧘", Keys.ICON_CHEESE to "🧀", Keys.ICON_MISSION to "🎯",
        Keys.ICON_ADVENTURE to "🗺️", Keys.ICON_BATTLE to "🤖", Keys.ICON_DAILY to "📅",
        Keys.STATS_TITLE to "📊 Statistiques",
        Keys.STATS_GAMES to "Parties", Keys.STATS_TIME to "Temps total",
        Keys.STATS_PIECES to "Pièces placées", Keys.STATS_LINES to "Lignes effacées",
        Keys.STATS_SCORE to "Score total",
        Keys.SETTINGS_TITLE to "⚙️ Paramètres",
        Keys.SETTINGS_AUDIO to "Audio", Keys.SETTINGS_VISUALS to "Visuels",
        Keys.SETTINGS_GAMEPLAY to "Jeu", Keys.SETTINGS_LANGUAGE to "Langue",
        Keys.SETTINGS_MUSIC to "Musique", Keys.SETTINGS_SFX to "Effets sonores",
        Keys.SETTINGS_VOICE to "Commentaire vocal", Keys.SETTINGS_CAPTIONS to "Sous-titres",
        Keys.SETTINGS_FX to "Niveau FX",
        Keys.SETTINGS_GESTURES to "Gestes (Swipe)",
        Keys.SETTINGS_NEXT5 to "Afficher 5 pièces suivantes",
        Keys.SETTINGS_GARBAGE to "Afficher les déchets",
        Keys.SETTINGS_BATTLE_LEVEL to "Difficulté de bataille",
        Keys.SETTINGS_GAMEPAD to "Support manette",
        Keys.SETTINGS_PIECE_SYMBOLS to "Symboles de pièces",
        Keys.SETTINGS_ALLOW_ROTATION to "Permettre rotation impossible",
        Keys.SETTINGS_HIDE_BUTTONS to "Masquer boutons de contrôle",
        Keys.SETTINGS_CUSTOMIZE_BUTTONS to "🎮 Personnaliser les boutons",
        Keys.BTN_DONE to "Terminé", Keys.BTN_CANCEL to "Annuler", Keys.BTN_OK to "OK",
        Keys.BTN_RESET to "Réinitialiser", Keys.BTN_REDEEM to "Échanger", Keys.BTN_SKIP to "Passer",
        Keys.BTN_NEXT to "Suivant", Keys.BTN_PREV to "Précédent", Keys.BTN_FINISH to "Terminer",
        Keys.BTN_BUY to "Acheter", Keys.BTN_EQUIP to "Équiper", Keys.BTN_EQUIPPED to "Équipé",
        Keys.GAME_OVER to "Partie terminée",
        Keys.CONTINUE_TITLE to "Continuer?",
        Keys.CONTINUE_AD to "Pub gratuite",
        Keys.CONTINUE_COINS to "Pièces",
        Keys.CONTINUE_FREE to "Effacer 10 lignes",
        Keys.CONTINUE_INSUFFICIENT to "Pièces insuffisantes",
        Keys.STORE_TITLE to "🛒 Boutique",
        Keys.STORE_TAB_FX to "Effets", Keys.STORE_TAB_THEME to "Thèmes", Keys.STORE_TAB_VOICE to "Voix",
        Keys.STORE_COINS to "Pièces",
        Keys.ACH_TITLE to "🏆 Succès",
        Keys.ACH_UNLOCKED to "Débloqués",
        Keys.LB_TITLE to "🥇 Classement",
        Keys.LB_RANK to "#", Keys.LB_SCORE to "Score", Keys.LB_LINES to "Lignes", Keys.LB_LEVEL to "Niveau",
        Keys.LB_EMPTY to "Pas encore de scores — jouez pour établir un record!",
        Keys.TUT_TITLE to "📚 Tutoriel",
        Keys.MISSIONS_TITLE to "🎯 Missions",
        Keys.MISSIONS_COMPLETED to "Terminées",
        Keys.ADVENTURE_TITLE to "🗺️ Aventure",
        Keys.ADVENTURE_STAGES to "Étapes",
        Keys.ADVENTURE_LOCKED to "Verrouillé",
        Keys.DAILY_GOAL to "Objectif du jour",
        Keys.DAILY_STREAK to "Série",
        Keys.GIFT_TITLE to "🎁 Codes cadeau",
        Keys.GIFT_HINT to "Entrez un code pour recevoir votre récompense",
        Keys.GIFT_SUCCESS to "Succès!",
        Keys.GIFT_FAILURE to "Échec",
        Keys.GIFT_REDEEMED_COUNT to "Codes utilisés",
        Keys.INPUT_TITLE to "🎮 Personnaliser les boutons",
        Keys.INPUT_HINT to "Glissez un bouton pour le déplacer. Sélectionnez pour redimensionner.",
        Keys.INPUT_SIZE to "Taille",
        Keys.INPUT_VISIBILITY to "Visibilité",
        Keys.INPUT_RESET_DEFAULT to "Réinitialiser",
        Keys.PAUSED to "Pause",
        Keys.COUNTDOWN to "Compte"
    )

    /** Map of all language tables. */
    private val TABLES: Map<String, Map<String, String>> = mapOf(
        "en" to EN,
        "ar" to AR,
        "fr" to FR
        // Other languages fall back to English for now — Phase 9.1 ships with
        // full ar/en/fr + English fallback for the remaining 13.
    )

    /** Get the translation table for a language (with English fallback). */
    fun tableFor(lang: String): Map<String, String> {
        val resolved = resolveLanguage(lang)
        return TABLES[resolved] ?: EN
    }

    /** Translator function for a specific language. */
    fun tFor(lang: String): (String) -> String {
        val table = tableFor(lang)
        return { key -> table[key] ?: EN[key] ?: key }
    }

    /** Current translator (based on SettingsManager.lang). */
    val t: (String) -> String
        get() = tFor(currentLang)

    /** Current effective language code. */
    var currentLang: String = "en"
        private set

    /** Initialize the translator from settings. Call from Activity.onCreate. */
    fun init(ctx: Context) {
        val settings = SettingsManager.load(ctx)
        currentLang = resolveLanguage(settings.lang)
    }

    /** Set the current language explicitly. */
    fun setLanguage(lang: String) {
        currentLang = resolveLanguage(lang)
    }
}
