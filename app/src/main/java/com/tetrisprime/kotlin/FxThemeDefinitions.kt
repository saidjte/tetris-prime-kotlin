package com.tetrisprime.kotlin

/**
 * FX particle pack definitions.
 * 22 packs with unique colors and behaviors.
 */
object FxPackDefinitions {

    data class FxPack(
        val id: String,
        val price: Int,
        val accent: String,
        val colors: List<String>
    )

    val ALL_PACKS: List<FxPack> = listOf(
        FxPack("classic", 0, "#8B5CF6", listOf("#8B5CF6", "#EC4899", "#22D3EE", "#FBBF24", "#34D399")),
        FxPack("burst", 700, "#F97316", listOf("#FBBF24", "#FB923C", "#F97316", "#EF4444", "#FEF3C7")),
        FxPack("stars", 700, "#FBBF24", listOf("#FDE68A", "#FBBF24", "#F472B6", "#FFFFFF", "#C4B5FD")),
        FxPack("bubbles", 600, "#67E8F9", listOf("#A5F3FC", "#67E8F9", "#22D3EE", "#E0F2FE", "#99F6E4")),
        FxPack("electric", 900, "#22D3EE", listOf("#67E8F9", "#22D3EE", "#E0F2FE", "#A78BFA", "#FFFFFF")),
        FxPack("fireworks", 1200, "#FB7185", listOf("#FBBF24", "#FB7185", "#A78BFA", "#34D399", "#F472B6")),
        FxPack("neonrain", 1000, "#E879F9", listOf("#E879F9", "#22D3EE", "#4ADE80", "#FDE047", "#F0ABFC")),
        FxPack("crystal", 1400, "#7DD3FC", listOf("#E0F2FE", "#7DD3FC", "#C4B5FD", "#A5F3FC", "#FFFFFF")),
        FxPack("phoenix", 1500, "#FB923C", listOf("#FDBA74", "#FB923C", "#F97316", "#FBBF24", "#FEF3C7")),
        FxPack("party", 1600, "#F472B6", listOf("#F472B6", "#FBBF24", "#4ADE80", "#22D3EE", "#C084FC")),
        FxPack("galaxy", 1800, "#A78BFA", listOf("#C4B5FD", "#A78BFA", "#818CF8", "#67E8F9", "#F0ABFC")),
        FxPack("aurora", 2200, "#34D399", listOf("#6EE7B7", "#34D399", "#22D3EE", "#A7F3D0", "#C4B5FD")),
        FxPack("supernova", 2500, "#FBBF24", listOf("#FBBF24", "#F97316", "#EF4444", "#FDE68A", "#FFFFFF")),
        FxPack("tsunami", 2400, "#0EA5E9", listOf("#0EA5E9", "#06B6D4", "#67E8F9", "#E0F2FE", "#3B82F6")),
        FxPack("inferno", 2600, "#DC2626", listOf("#DC2626", "#F97316", "#FBBF24", "#EF4444", "#FEF3C7")),
        FxPack("blizzard", 2500, "#E0F2FE", listOf("#E0F2FE", "#BFDBFE", "#93C5FD", "#DBEAFE", "#FFFFFF")),
        FxPack("volcano", 2800, "#F97316", listOf("#F97316", "#DC2626", "#FBBF24", "#7C2D12", "#FEF3C7")),
        FxPack("earthquake", 2700, "#A16207", listOf("#A16207", "#78350F", "#F59E0B", "#FBBF24", "#92400E")),
        FxPack("tornado", 2600, "#6B7280", listOf("#6B7280", "#9CA3AF", "#D1D5DB", "#F3F4F6", "#374151")),
        FxPack("lightning", 2500, "#FBBF24", listOf("#FBBF24", "#FDE047", "#FFFFFF", "#E0F2FE", "#A78BFA")),
        FxPack("rainbow", 3000, "#A78BFA", listOf("#EF4444", "#F97316", "#FBBF24", "#34D399", "#3B82F6", "#A78BFA", "#EC4899")),
        FxPack("cosmic", 3500, "#7C3AED", listOf("#7C3AED", "#C4B5FD", "#22D3EE", "#F0ABFC", "#FDE68A"))
    )

    /** Get pack by ID (defaults to classic). */
    fun getPack(id: String): FxPack = ALL_PACKS.find { it.id == id } ?: ALL_PACKS[0]

    /** Get colors for a pack. */
    fun getColors(id: String): List<String> = getPack(id).colors

    /** FX quality levels. */
    val FX_LEVELS = listOf("off", "low", "medium", "high", "ultra", "legendary")

    /** FX level multipliers. */
    val FX_MULTIPLIERS = mapOf(
        "off" to 0f,
        "low" to 0.3f,
        "medium" to 0.5f,
        "high" to 1.0f,
        "ultra" to 1.5f,
        "legendary" to 2.2f
    )
}

/**
 * Theme definitions.
 * 51 themes with 7 piece colors each.
 */
object ThemeDefinitions {

    data class Theme(
        val id: String,
        val price: Int,
        val accent: String,
        val colors: Map<Char, String>, // I, O, T, S, Z, J, L
        val boardBg: String? = null,
        val boardPattern: String? = null
    )

    private fun colors(i: String, o: String, t: String, s: String, z: String, j: String, l: String) =
        mapOf('I' to i, 'O' to o, 'T' to t, 'S' to s, 'Z' to z, 'J' to j, 'L' to l)

    val ALL_THEMES: List<Theme> = listOf(
        Theme("classic", 0, "#8B5CF6", colors("#22D3EE", "#FBBF24", "#A78BFA", "#4ADE80", "#FB7185", "#60A5FA", "#FB923C")),
        Theme("neon", 0, "#00F0FF", colors("#00F0FF", "#FFE700", "#FF00FF", "#39FF14", "#FF3355", "#0090FF", "#FF9500")),
        Theme("colorSafe", 0, "#56B4E9", colors("#56B4E9", "#E69F00", "#009E73", "#F0E442", "#D55E00", "#0072B2", "#CC79A7")),
        Theme("crystal", 1500, "#7DD3FC", colors("#E0F2FE", "#7DD3FC", "#C4B5FD", "#A5F3FC", "#BFDBFE", "#93C5FD", "#DBEAFE"), "rgba(10,20,40,0.7)"),
        Theme("ruby", 1600, "#DC2626", colors("#FCA5A5", "#DC2626", "#EF4444", "#F87171", "#B91C1C", "#FECACA", "#FEE2E2"), "rgba(40,10,10,0.7)"),
        Theme("glass", 1400, "#E0F2FE", colors("#F0F9FF", "#E0F2FE", "#DBEAFE", "#F0FDFA", "#FDF4FF", "#ECFEFF", "#F8FAFC"), "rgba(20,30,40,0.5)"),
        Theme("metal", 1500, "#9CA3AF", colors("#E5E7EB", "#9CA3AF", "#6B7280", "#D1D5DB", "#4B5563", "#F3F4F6", "#374151"), "rgba(30,30,35,0.75)"),
        Theme("blaze", 1800, "#F97316", colors("#FBBF24", "#F97316", "#EF4444", "#FDE68A", "#DC2626", "#FDBA74", "#FEF3C7"), "rgba(40,15,5,0.75)"),
        Theme("aqua", 1600, "#06B6D4", colors("#67E8F9", "#06B6D4", "#0EA5E9", "#A5F3FC", "#0284C7", "#CFFAFE", "#BAE6FD"), "rgba(5,20,40,0.7)"),
        Theme("emerald", 1700, "#10B981", colors("#6EE7B7", "#10B981", "#34D399", "#A7F3D0", "#059669", "#D1FAE5", "#BBF7D0"), "rgba(5,30,20,0.7)"),
        Theme("amethyst", 1700, "#8B5CF6", colors("#C4B5FD", "#8B5CF6", "#A78BFA", "#DDD6FE", "#7C3AED", "#EDE9FE", "#C4B5FD"), "rgba(25,10,40,0.7)"),
        Theme("gold", 2000, "#FBBF24", colors("#FDE68A", "#FBBF24", "#F59E0B", "#FEF3C7", "#D97706", "#FFF7ED", "#FCD34D"), "rgba(35,25,5,0.75)"),
        Theme("shadow", 1800, "#1F2937", colors("#374151", "#1F2937", "#4B5563", "#6B7280", "#111827", "#9CA3AF", "#D1D5DB"), "rgba(5,5,10,0.85)")
    )

    /** Get theme by ID (defaults to classic). */
    fun getTheme(id: String): Theme = ALL_THEMES.find { it.id == id } ?: ALL_THEMES[0]

    /** Get piece color for a theme. */
    fun getPieceColor(themeId: String, pieceType: Char): String {
        val theme = getTheme(themeId)
        return theme.colors[pieceType] ?: "#FFFFFF"
    }
}
