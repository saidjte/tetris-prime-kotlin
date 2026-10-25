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
    /** FX quality levels.
     *  Phase 9 — Updated to match original game's 5 particle intensity levels
     *  (extracted from https://w1p1p97jy0j0-d.space-z.ai):
     *    off         0×    no particles
     *    weak        0.6×  ~42 small particles
     *    medium      1.2×  ~50 moderate particles
     *    strong      2.2×  ~70 dense particles (DEFAULT)
     *    veryStrong  3.3×  ~90 dense and large particles
     *    epic        4.4×  ~110 very large and dense particles
     */
    val FX_LEVELS = listOf("off", "weak", "medium", "strong", "veryStrong", "epic")

    /** FX level multipliers. */
    val FX_MULTIPLIERS = mapOf(
        "off" to 0f,
        "weak" to 0.6f,
        "medium" to 1.2f,
        "strong" to 2.2f,
        "veryStrong" to 3.3f,
        "epic" to 4.4f
    )

    /** Default FX level — matches original game's "strong" default. */
    const val DEFAULT_FX_LEVEL = "strong"
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
        // Free themes (price=0)
        Theme("classic", 0, "#8B5CF6", colors("#22D3EE", "#FBBF24", "#A78BFA", "#4ADE80", "#FB7185", "#60A5FA", "#FB923C")),
        Theme("neon", 0, "#00F0FF", colors("#00F0FF", "#FFE700", "#FF00FF", "#39FF14", "#FF3355", "#0090FF", "#FF9500")),
        Theme("colorSafe", 0, "#56B4E9", colors("#56B4E9", "#E69F00", "#009E73", "#F0E442", "#D55E00", "#0072B2", "#CC79A7")),
        // Early unlock (price=1)
        Theme("gold", 1, "#FBBF24", colors("#FDE68A", "#FBBF24", "#F59E0B", "#FEF3C7", "#D97706", "#FFF7ED", "#FCD34D")),
        Theme("sunset", 1, "#F97316", colors("#FDBA74", "#F97316", "#FB923C", "#FED7AA", "#EA580C", "#FFEDD5", "#FDBA74")),
        Theme("aurora", 1, "#34D399", colors("#6EE7B7", "#34D399", "#22D3EE", "#A7F3D0", "#10B981", "#D1FAE5", "#BBF7D0")),
        Theme("forest", 1, "#15803D", colors("#86EFAC", "#22C55E", "#15803D", "#4ADE80", "#166534", "#BBF7D0", "#86EFAC")),
        Theme("midnight", 1, "#1E3A8A", colors("#93C5FD", "#3B82F6", "#1E40AF", "#60A5FA", "#1E3A8A", "#DBEAFE", "#BFDBFE")),
        Theme("matcha", 1, "#84CC16", colors("#A3E635", "#84CC16", "#65A30D", "#BEF264", "#4D7C0F", "#ECFCCB", "#D9F99D")),
        Theme("bamboo", 1, "#4D7C0F", colors("#84CC16", "#65A30D", "#4D7C0F", "#A3E635", "#3F6212", "#D9F99D", "#BEF264")),
        Theme("pitch", 1, "#0F172A", colors("#334155", "#1E293B", "#475569", "#64748B", "#0F172A", "#94A3B8", "#CBD5E1")),
        Theme("court", 1, "#7C3AED", colors("#C4B5FD", "#8B5CF6", "#7C3AED", "#A78BFA", "#6D28D9", "#DDD6FE", "#C4B5FD")),
        // Budget themes (price=300-800)
        Theme("candy", 300, "#F9A8D4", colors("#7DD3FC", "#FDE68A", "#D8B4FE", "#86EFAC", "#FDA4AF", "#A5C8FF", "#FDBA74"), "rgba(34,14,34,0.72)"),
        Theme("retro", 400, "#8BAC0F", colors("#CADC9F", "#9BBC0F", "#8BAC0F", "#0F380F", "#306230", "#9BBC0F", "#CADC9F"), "rgba(15,56,15,0.8)"),
        Theme("fire", 600, "#F97316", colors("#FDBA74", "#F97316", "#FB923C", "#FED7AA", "#EA580C", "#FFEDD5", "#FDBA74"), "rgba(40,15,5,0.75)"),
        Theme("ice", 600, "#7DD3FC", colors("#BAE6FD", "#7DD3FC", "#38BDF8", "#E0F2FE", "#0284C7", "#F0F9FF", "#BAE6FD"), "rgba(10,30,50,0.7)"),
        Theme("mono", 600, "#E5E7EB", colors("#F3F4F6", "#E5E7EB", "#D1D5DB", "#9CA3AF", "#6B7280", "#F9FAFB", "#D1D5DB"), "rgba(10,10,12,0.85)"),
        Theme("sakura", 800, "#F9A8D4", colors("#FBCFE8", "#F9A8D4", "#F472B6", "#FCE7F3", "#EC4899", "#FDF2F8", "#FBCFE8"), "rgba(40,20,30,0.7)"),
        Theme("stone", 800, "#8B7BB8", colors("#A8A29E", "#8B7BB8", "#78716C", "#D6D3D1", "#57534E", "#E7E5E4", "#A8A29E"), "rgba(25,22,28,0.78)"),
        Theme("ocean", 800, "#38BDF8", colors("#7DD3FC", "#38BDF8", "#0EA5E9", "#BAE6FD", "#0284C7", "#E0F2FE", "#7DD3FC"), "rgba(5,20,45,0.72)"),
        Theme("desert", 800, "#F59E0B", colors("#FCD34D", "#F59E0B", "#D97706", "#FDE68A", "#B45309", "#FEF3C7", "#FCD34D"), "rgba(45,30,10,0.72)"),
        Theme("meadow", 800, "#4ADE80", colors("#86EFAC", "#4ADE80", "#22C55E", "#BBF7D0", "#16A34A", "#DCFCE7", "#86EFAC"), "rgba(10,30,15,0.7)"),
        // Mid-tier themes (price=900-1200)
        Theme("popArt", 900, "#EF4444", colors("#FCA5A5", "#EF4444", "#FCD34D", "#86EFAC", "#3B82F6", "#FED7AA", "#F9A8D4"), "rgba(30,15,20,0.75)"),
        Theme("duoCoral", 900, "#F97C6D", colors("#FDA4AF", "#F97C6D", "#FCA5A5", "#FECDD3", "#FB7185", "#FFE4E6", "#FECDD3"), "rgba(35,15,15,0.72)"),
        Theme("duoMint", 900, "#4FD1A5", colors("#A7F3D0", "#4FD1A5", "#6EE7B7", "#D1FAE5", "#10B981", "#ECFDF5", "#D1FAE5"), "rgba(10,30,22,0.72)"),
        Theme("fez", 1100, "#3E9C77", colors("#6EE7B7", "#3E9C77", "#10B981", "#A7F3D0", "#047857", "#D1FAE5", "#6EE7B7"), "rgba(10,30,20,0.74)"),
        Theme("toxic", 1200, "#A3E635", colors("#D9F99D", "#A3E635", "#84CC16", "#ECFCCB", "#65A30D", "#F7FEE7", "#D9F99D"), "rgba(20,30,5,0.75)"),
        Theme("matrix", 1200, "#22C55E", colors("#4ADE80", "#22C55E", "#16A34A", "#86EFAC", "#15803D", "#BBF7D0", "#4ADE80"), "rgba(0,20,0,0.85)"),
        Theme("neonpunk", 1200, "#E879F9", colors("#F0ABFC", "#E879F9", "#D946EF", "#FAE8FF", "#C026D3", "#FDF4FF", "#F0ABFC"), "rgba(30,10,35,0.75)"),
        Theme("vaporwave", 1200, "#FF71CE", colors("#FF71CE", "#01CDFE", "#05FFA1", "#B967FF", "#FFFB96", "#01CDFE", "#FF71CE"), "rgba(25,10,40,0.72)"),
        Theme("royal", 1200, "#DC2626", colors("#FCA5A5", "#DC2626", "#EF4444", "#FECACA", "#991B1B", "#FEE2E2", "#FCA5A5"), "rgba(40,10,10,0.75)"),
        Theme("marrakesh", 1200, "#F97316", colors("#FDBA74", "#F97316", "#FB923C", "#FED7AA", "#C2410C", "#FFEDD5", "#FDBA74"), "rgba(40,20,5,0.74)"),
        Theme("chaouen", 1200, "#93C5FD", colors("#EFF6FF", "#93C5FD", "#60A5FA", "#BFDBFE", "#1E3A8A", "#1D4ED8", "#38BDF8"), "rgba(8,14,34,0.94)", "zellige"),
        // High-tier themes (price=1300-1600)
        Theme("zellige", 1300, "#10B981", colors("#6EE7B7", "#10B981", "#34D399", "#A7F3D0", "#059669", "#D1FAE5", "#BBF7D0"), "rgba(6,9,19,0.92)", "zellige"),
        Theme("lava", 1400, "#F87171", colors("#FCA5A5", "#F87171", "#EF4444", "#FECACA", "#DC2626", "#FEE2E2", "#FCA5A5"), "rgba(40,8,5,0.78)"),
        Theme("tropical", 1400, "#2DD4BF", colors("#5EEAD4", "#2DD4BF", "#14B8A6", "#99F6E4", "#0D9488", "#CCFBF1", "#5EEAD4"), "rgba(5,35,30,0.7)"),
        Theme("cube3d", 1400, "#38BDF8", colors("#7DD3FC", "#38BDF8", "#0EA5E9", "#BAE6FD", "#0284C7", "#E0F2FE", "#7DD3FC"), "rgba(10,20,40,0.72)"),
        Theme("gem3d", 1400, "#C084FC", colors("#DDD6FE", "#C084FC", "#A78BFA", "#EDE9FE", "#8B5CF6", "#F5F3FF", "#DDD6FE"), "rgba(25,10,40,0.72)"),
        Theme("glass", 1400, "#E0F2FE", colors("#F0F9FF", "#E0F2FE", "#DBEAFE", "#F0FDFA", "#FDF4FF", "#ECFEFF", "#F8FAFC"), "rgba(20,30,40,0.5)"),
        Theme("crystal", 1500, "#7DD3FC", colors("#E0F2FE", "#7DD3FC", "#C4B5FD", "#A5F3FC", "#BFDBFE", "#93C5FD", "#DBEAFE"), "rgba(10,20,40,0.7)"),
        Theme("metal", 1500, "#9CA3AF", colors("#E5E7EB", "#9CA3AF", "#6B7280", "#D1D5DB", "#4B5563", "#F3F4F6", "#374151"), "rgba(30,30,35,0.75)"),
        Theme("ruby", 1600, "#DC2626", colors("#FCA5A5", "#DC2626", "#EF4444", "#F87171", "#B91C1C", "#FECACA", "#FEE2E2"), "rgba(40,10,10,0.7)"),
        Theme("aqua", 1600, "#06B6D4", colors("#67E8F9", "#06B6D4", "#0EA5E9", "#A5F3FC", "#0284C7", "#CFFAFE", "#BAE6FD"), "rgba(5,20,40,0.7)"),
        // Premium themes (price=1700-2000)
        Theme("emerald", 1700, "#10B981", colors("#6EE7B7", "#10B981", "#34D399", "#A7F3D0", "#059669", "#D1FAE5", "#BBF7D0"), "rgba(5,30,20,0.7)"),
        Theme("amethyst", 1700, "#8B5CF6", colors("#C4B5FD", "#8B5CF6", "#A78BFA", "#DDD6FE", "#7C3AED", "#EDE9FE", "#C4B5FD"), "rgba(25,10,40,0.7)"),
        Theme("galaxy", 1500, "#A78BFA", colors("#C4B5FD", "#A78BFA", "#818CF8", "#67E8F9", "#F0ABFC", "#DDD6FE", "#C4B5FD"), "rgba(15,8,30,0.78)"),
        Theme("deepSpace", 1500, "#7DD3FC", colors("#BAE6FD", "#7DD3FC", "#38BDF8", "#E0F2FE", "#0284C7", "#F0F9FF", "#BAE6FD"), "rgba(5,8,20,0.85)"),
        Theme("nebula", 1500, "#E879F9", colors("#F0ABFC", "#E879F9", "#D946EF", "#FAE8FF", "#C026D3", "#FDF4FF", "#F0ABFC"), "rgba(20,8,30,0.78)"),
        Theme("blaze", 1800, "#F97316", colors("#FBBF24", "#F97316", "#EF4444", "#FDE68A", "#DC2626", "#FDBA74", "#FEF3C7"), "rgba(40,15,5,0.75)"),
        Theme("shadow", 1800, "#1F2937", colors("#374151", "#1F2937", "#4B5563", "#6B7280", "#111827", "#9CA3AF", "#D1D5DB"), "rgba(5,5,10,0.85)"),
        Theme("gold", 2000, "#FBBF24", colors("#FDE68A", "#FBBF24", "#F59E0B", "#FEF3C7", "#D97706", "#FFF7ED", "#FCD34D"), "rgba(35,25,5,0.75)")
    )

    /** Get theme by ID (defaults to classic). */
    fun getTheme(id: String): Theme = ALL_THEMES.find { it.id == id } ?: ALL_THEMES[0]

    /** Get piece color for a theme. */
    fun getPieceColor(themeId: String, pieceType: Char): String {
        val theme = getTheme(themeId)
        return theme.colors[pieceType] ?: "#FFFFFF"
    }
}
