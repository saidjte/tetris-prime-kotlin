package com.tetrisprime.kotlin

import kotlin.math.max

/**
 * Extended game engine with mode-specific logic.
 * Handles win/lose conditions for each game mode.
 *
 * Mode flags (mirrors original app's mode configs):
 *   - fixedLevel: pin the level (sprint=1, zen=2, cheese=3, daily=4)
 *   - levelUp: allow level progression (classic, ultra, mission, adventure, battle)
 *   - timed: track elapsed time (sprint, ultra, cheese)
 *   - countdownTimer: show countdown (ultra only)
 *   - scoreBased: score is the primary metric
 *   - timeRecord: track best time (sprint, cheese)
 *   - cheeseStart: spawn 10 garbage rows at start (cheese)
 */
class ModeGameEngine(
    val mode: GameMode = GameMode.CLASSIC,
    private val seededRandom: SeededRandom? = null,
    private val missionGoal: MissionGoal? = null,
    private val dailyDateStr: String? = null,
    /** Adventure tokens collected during this run. */
    private val advTokensCollected: MutableSet<String> = mutableSetOf()
) : GameEngine(mode, seededRandom) {

    // Mode-specific state
    private var elapsedTimeMs = 0L
    private var ultra60Announced = false
    private var ultra30Announced = false
    private var hurryAnnounced = false
    /** Sprint: track if best-time record was beaten (set by caller). */
    private var sprintFinishTimeMs: Long? = null

    // Callbacks for UI events
    var onWin: (() -> Unit)? = null
    var onTimeUp: (() -> Unit)? = null
    var onUltra60: (() -> Unit)? = null
    var onUltra30: (() -> Unit)? = null
    var onHurry: (() -> Unit)? = null
    var onCheeseProgress: ((remaining: Int) -> Unit)? = null
    var onMissionProgress: ((current: Int, goal: Int) -> Unit)? = null
    var onAdventureTokens: ((collected: Int, goal: Int) -> Unit)? = null

    /** Get the game result when game ends. */
    fun getGameResult(): GameResult {
        val isWin = when (mode) {
            GameMode.SPRINT -> lines >= ModeConfig.SPRINT_GOAL_LINES
            GameMode.ULTRA -> true // ultra always "wins" (score-based)
            GameMode.CHEESE -> board.countCheeseCells() == 0
            GameMode.MISSION -> isMissionComplete()
            GameMode.ADVENTURE -> isMissionComplete()
            GameMode.BATTLE -> false // set externally
            GameMode.DAILY -> score >= getDailyGoal()
            else -> false
        }

        val reason = when {
            mode == GameMode.ULTRA && elapsedTimeMs >= ModeConfig.ULTRA_DURATION_MS -> GameOverReason.TIME
            isWin && mode == GameMode.BATTLE -> GameOverReason.BATTLE_WIN
            !isWin && mode == GameMode.BATTLE -> GameOverReason.BATTLE_LOSS
            isWin -> GameOverReason.GOAL
            else -> GameOverReason.TOPOUT
        }

        return GameResult(
            mode = mode,
            score = score,
            lines = lines,
            timeMs = elapsedTimeMs,
            isWin = isWin,
            isNewRecord = false, // set by caller
            overReason = reason
        )
    }

    /** Get the daily challenge goal.
     *  Returns FNV-1a hash of "goal/YYYY-MM-DD" clamped to ContentLoader range. */
    fun getDailyGoal(): Int {
        val dateStr = dailyDateStr ?: java.time.LocalDate.now().toString()
        return Fnv1aHash.dailyGoal(dateStr)
    }

    /** Check if mission/adventure is complete. */
    private fun isMissionComplete(): Boolean {
        val goal = missionGoal ?: return false
        return when (goal.kind) {
            MissionKind.LINES -> lines >= goal.n
            MissionKind.SCORE -> score >= goal.n
            MissionKind.TETRISES -> tetrises >= goal.n
            MissionKind.TSPINS -> tspins >= goal.n
            MissionKind.COMBO -> maxCombo >= goal.n
            MissionKind.PERFECT_CLEAR -> perfectClears >= goal.n
            MissionKind.SURVIVE -> elapsedTimeMs >= goal.n * 1000L
            MissionKind.LEVEL -> level >= goal.n
            MissionKind.TOKENS -> advTokensCollected.size >= goal.n
        }
    }

    /** Phase 3 — called from start() to apply mode-specific board init.
     *  - Cheese: spawn 10 garbage rows (cheeseStart=true). Already handled in GameEngine.start().
     *  - Daily: ensure seeded RNG is wired (passed via constructor).
     *  - Mission level pin: enforced by GameEngine via mode.fixedLevel + levelUp flag. */
    fun applyModeInit() {
        // No-op: cheese init is handled by GameEngine.start() (board.fillCheese).
        // Mission level pinning happens via the engine's mode.fixedLevel + levelUp guard.
        // Daily seeded RNG is passed via the constructor.
    }

    /** Adventure: collect a token (called when a marked cell is cleared). */
    fun collectAdventureToken(tokenId: String) {
        advTokensCollected.add(tokenId)
        missionGoal?.let { goal ->
            if (goal.kind == MissionKind.TOKENS) {
                onAdventureTokens?.invoke(advTokensCollected.size, goal.n)
                if (advTokensCollected.size >= goal.n) {
                    isGameOver = true
                    status = GameStatus.OVER
                    onWin?.invoke()
                }
            }
        }
    }

    /** Update with mode-specific logic. */
    fun updateMode(deltaMs: Long) {
        if (isGameOver || isPaused || status != GameStatus.PLAYING) return

        elapsedTimeMs += deltaMs

        // Ultra mode: time-based with countdown
        if (mode == GameMode.ULTRA) {
            val remaining = ModeConfig.ULTRA_DURATION_MS - elapsedTimeMs
            if (!ultra60Announced && remaining <= ModeConfig.ULTRA_60S_ANNOUNCE) {
                ultra60Announced = true
                onUltra60?.invoke()
            }
            if (!ultra30Announced && remaining <= ModeConfig.ULTRA_30S_ANNOUNCE) {
                ultra30Announced = true
                onUltra30?.invoke()
            }
            if (!hurryAnnounced && remaining <= ModeConfig.ULTRA_HURRY_ANNOUNCE) {
                hurryAnnounced = true
                onHurry?.invoke()
            }
            if (elapsedTimeMs >= ModeConfig.ULTRA_DURATION_MS) {
                isGameOver = true
                status = GameStatus.OVER
                onTimeUp?.invoke()
                return
            }
        }

        // Sprint mode: win at 40 lines
        if (mode == GameMode.SPRINT && lines >= ModeConfig.SPRINT_GOAL_LINES) {
            sprintFinishTimeMs = elapsedTimeMs
            isGameOver = true
            status = GameStatus.OVER
            onWin?.invoke()
            return
        }

        // Cheese mode: win when all cheese cleared
        if (mode == GameMode.CHEESE) {
            val remaining = board.countCheeseCells()
            onCheeseProgress?.invoke(remaining)
            if (remaining == 0) {
                isGameOver = true
                status = GameStatus.OVER
                onWin?.invoke()
                return
            }
        }

        // Mission/Adventure mode: check goal
        if (mode == GameMode.MISSION || mode == GameMode.ADVENTURE) {
            missionGoal?.let { goal ->
                val current = when (goal.kind) {
                    MissionKind.LINES -> lines
                    MissionKind.SCORE -> score.toInt()
                    MissionKind.TETRISES -> tetrises
                    MissionKind.TSPINS -> tspins
                    MissionKind.COMBO -> maxCombo
                    MissionKind.PERFECT_CLEAR -> perfectClears
                    MissionKind.SURVIVE -> (elapsedTimeMs / 1000).toInt()
                    MissionKind.LEVEL -> level
                    MissionKind.TOKENS -> advTokensCollected.size
                }
                onMissionProgress?.invoke(current, goal.n)
                if (current >= goal.n) {
                    isGameOver = true
                    status = GameStatus.OVER
                    onWin?.invoke()
                    return
                }
            }
        }

        // Daily mode: win at goal score
        if (mode == GameMode.DAILY && score >= getDailyGoal()) {
            isGameOver = true
            status = GameStatus.OVER
            onWin?.invoke()
            return
        }

        // Call parent update for gravity/lock
        update(deltaMs)
    }

    /** Get elapsed time in seconds. */
    fun getElapsedSeconds(): Int = (elapsedTimeMs / 1000).toInt()

    /** Get remaining time for ultra mode. */
    fun getUltraRemainingMs(): Long {
        return if (mode == GameMode.ULTRA) {
            max(0, ModeConfig.ULTRA_DURATION_MS - elapsedTimeMs)
        } else 0
    }

    /** Sprint finish time (null if not finished). */
    fun getSprintFinishTimeMs(): Long? = sprintFinishTimeMs
}

/** Mission/adventure goal definition. */
data class MissionGoal(
    val id: Int,
    val kind: MissionKind,
    val n: Int,
    val level: Int? = null,
    val garbage: String? = null, // "off", "light", "heavy"
    val title: String? = null,
    val desc: String? = null
)

/** Mission kinds. */
enum class MissionKind {
    LINES,
    SCORE,
    TETRISES,
    TSPINS,
    COMBO,
    PERFECT_CLEAR,
    SURVIVE,
    LEVEL,
    TOKENS  // Adventure-only: collect marked cells (8 token types)
}
