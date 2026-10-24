package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Game activity that supports all game modes.
 * Replaces MainActivity with mode-aware gameplay.
 *
 * Phase 7 additions:
 *   - Keyboard + gamepad via onKeyDown/onKeyUp (DAS=150ms, ARR=35ms)
 *   - Gestures on the playfield (swipe = move/hold/soft-drop, tap = rotate, long-press = hard-drop)
 *   - Haptic feedback on every input action
 *   - Reads user Settings to enable/disable subsystems
 */
class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var levelText: TextView
    private lateinit var linesText: TextView
    private lateinit var comboText: TextView
    private lateinit var statusText: TextView
    private lateinit var modeText: TextView

    private var engine: ModeGameEngine? = null
    private var audioEngine: AudioEngine? = null
    private var battleAI: BattleAI? = null
    private var lastUpdateTime = 0L
    private var continuePromptShown = false
    private var missionIndex: Int = -1   // -1 = not a mission/adventure
    private var adventureIndex: Int = -1

    // Phase 7 input subsystems
    private lateinit var haptic: HapticController
    private lateinit var keyboardDispatcher: KeyboardGamepadDispatcher
    private var gestureDispatcher: GestureDispatcher? = null
    private var settings: Settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameView = findViewById(R.id.gameView)
        scoreText = findViewById(R.id.scoreText)
        levelText = findViewById(R.id.levelText)
        linesText = findViewById(R.id.linesText)
        comboText = findViewById(R.id.comboText)
        statusText = findViewById(R.id.statusText)
        modeText = findViewById(R.id.modeText)

        // Get mode from intent
        val modeName = intent.getStringExtra("mode") ?: GameMode.CLASSIC.name
        val mode = GameMode.valueOf(modeName)

        modeText.text = getModeDisplayName(mode)

        // Initialize audio engine
        audioEngine = AudioEngine(this)
        audioEngine?.init()

        // Create engine based on mode
        when (mode) {
            GameMode.DAILY -> {
                val dateStr = intent.getStringExtra("daily_date") ?: java.time.LocalDate.now().toString()
                engine = ModeGameEngine(mode = mode, dailyDateStr = dateStr)
            }
            GameMode.MISSION -> {
                val kind = MissionKind.valueOf(intent.getStringExtra("mission_kind") ?: MissionKind.LINES.name)
                val n = intent.getIntExtra("mission_n", 10)
                val level = intent.getIntExtra("mission_level", 1)
                engine = ModeGameEngine(mode = mode, missionGoal = MissionGoal(1, kind, n, level))
            }
            GameMode.BATTLE -> {
                val diffName = intent.getStringExtra("battle_difficulty") ?: BattleDifficulty.NORMAL.name
                val diff = BattleDifficulty.valueOf(diffName)
                engine = ModeGameEngine(mode = mode)
                battleAI = BattleAI(diff)
                battleAI?.reset()
            }
            else -> {
                engine = ModeGameEngine(mode = mode)
            }
        }

        // CRITICAL: start the engine so the first piece spawns, board is initialized,
        // and the countdown begins. Without this call, the engine sits in its initial
        // state (empty board, null currentPiece) and the game appears frozen.
        engine?.start()

        // Phase 8: capture mission/adventure index for completion tracking
        missionIndex = intent.getIntExtra("mission_index", -1)
        adventureIndex = intent.getIntExtra("adventure_index", -1)

        // Set audio callbacks
        engine?.onWin = {
            statusText.text = "🏆 فوز!"
            audioEngine?.voiceWin()
        }
        engine?.onTimeUp = {
            statusText.text = "⏰ انتهى الوقت!"
            audioEngine?.voiceTimeUp()
        }
        engine?.onUltra60 = {
            statusText.text = "⏱️ دقيقة متبقية"
            audioEngine?.voiceUltra60()
        }
        engine?.onUltra30 = {
            statusText.text = "⏱️ ٣٠ ثانية"
            audioEngine?.voiceUltra30()
        }
        engine?.onHurry = {
            statusText.text = "⚠️ عجّل!"
            audioEngine?.voiceHurry()
        }

        // Start game music
        audioEngine?.onGameStart(mode)

        // Set callbacks
        engine?.onWin = { statusText.text = "🏆 فوز!" }
        engine?.onTimeUp = { statusText.text = "⏰ انتهى الوقت!" }
        engine?.onUltra60 = { statusText.text = "⏱️ دقيقة متبقية" }
        engine?.onUltra30 = { statusText.text = "⏱️ ٣٠ ثانية" }
        engine?.onHurry = { statusText.text = "⚠️ عجّل!" }
        engine?.onCheeseProgress = { remaining ->
            statusText.text = "🧀 متبقي: $remaining"
        }
        engine?.onMissionProgress = { current, goal ->
            statusText.text = "🎯 $current / $goal"
        }

        // Replace the GameView's engine with our mode engine
        gameView.setEngine(engine!!)

        // Phase 7: initialize input subsystems
        settings = SettingsManager.load(this)
        // Haptics are always enabled at the controller level; the buzz() method
        // is fully exception-safe and silently no-ops if the device has no vibrator
        // or the VIBRATE permission is missing.
        haptic = HapticController(this)
        keyboardDispatcher = KeyboardGamepadDispatcher(haptic) { action -> dispatchAction(action) }
        if (settings.gestures) {
            gestureDispatcher = GestureDispatcher(haptic) { action -> dispatchAction(action) }
            gameView.setOnTouchListener { _, event ->
                gestureDispatcher?.onTouchEvent(event) ?: false
                // Return false so the view still receives invalidates
            }
        }

        // Hide on-screen buttons if user opted in
        if (settings.hideCtlButtons) {
            findViewById<android.view.View>(R.id.controlsLayout).visibility = android.view.View.GONE
        }

        setupControls()
    }

    /**
     * Phase 7 — single dispatch entry point.
     * Translates InputSystem.Action into GameEngine calls.
     */
    private fun dispatchAction(action: InputSystem.Action) {
        val eng = engine ?: return
        when (action) {
            InputSystem.Action.MOVE_LEFT       -> eng.move(-1)
            InputSystem.Action.MOVE_RIGHT      -> eng.move(1)
            InputSystem.Action.ROTATE_CW       -> eng.rotate(1)
            InputSystem.Action.ROTATE_CCW      -> eng.rotate(-1)
            InputSystem.Action.SOFT_DROP_START -> eng.setSoftDropping(true)
            InputSystem.Action.SOFT_DROP_STOP  -> eng.setSoftDropping(false)
            InputSystem.Action.HARD_DROP       -> eng.hardDrop()
            InputSystem.Action.HOLD            -> eng.hold()
            InputSystem.Action.PAUSE           -> eng.togglePause()
            InputSystem.Action.RESTART         -> {
                eng.start()
                battleAI?.reset()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyboardDispatcher.handleKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyboardDispatcher.handleKeyUp(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun getModeDisplayName(mode: GameMode): String = when (mode) {
        GameMode.CLASSIC -> "🎮 كلاسيكي"
        GameMode.SPRINT -> "🏃 سبرينت"
        GameMode.ULTRA -> "⚡ ألترا"
        GameMode.ZEN -> "🧘 زن"
        GameMode.CHEESE -> "🧀 جبن"
        GameMode.MISSION -> "🎯 مهمات"
        GameMode.ADVENTURE -> "🗺️ مغامرة"
        GameMode.BATTLE -> "🤖 معركة"
        GameMode.DAILY -> "📅 يومي"
    }

    private fun setupControls() {
        setupRepeatButton(R.id.btnLeft) { engine?.move(-1) ?: false; haptic.light(); true }
        setupRepeatButton(R.id.btnRight) { engine?.move(1) ?: false; haptic.light(); true }

        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            engine?.rotate(1); haptic.light()
        }

        val softDropBtn = findViewById<Button>(R.id.btnSoftDrop)
        softDropBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { engine?.setSoftDropping(true); haptic.light() }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> engine?.setSoftDropping(false)
            }
            false
        }

        findViewById<Button>(R.id.btnHardDrop).setOnClickListener {
            engine?.hardDrop(); haptic.medium()
        }
        findViewById<Button>(R.id.btnHold).setOnClickListener {
            engine?.hold(); haptic.light()
        }
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            engine?.togglePause()
        }
        findViewById<Button>(R.id.btnRestart).setOnClickListener {
            engine?.start()
            battleAI?.reset()
        }
    }

    private fun setupRepeatButton(buttonId: Int, action: () -> Boolean) {
        val btn = findViewById<Button>(buttonId)
        var isRepeating = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val initialDelay = 150L
        val repeatDelay = 35L

        val repeatRunnable = object : Runnable {
            override fun run() {
                if (isRepeating) {
                    action()
                    handler.postDelayed(this, repeatDelay)
                }
            }
        }

        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isRepeating = true
                    action()
                    handler.postDelayed(repeatRunnable, initialDelay)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isRepeating = false
                    handler.removeCallbacks(repeatRunnable)
                }
            }
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine?.release()
    }

    override fun onResume() {
        super.onResume()
        lastUpdateTime = System.currentTimeMillis()
        gameLoop()
    }

    private fun gameLoop() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val delta = now - lastUpdateTime
                lastUpdateTime = now

                // Update battle AI
                if (battleAI != null && engine?.status == GameStatus.PLAYING) {
                    val garbageFromAI = battleAI!!.update(delta)
                    if (garbageFromAI > 0) {
                        // Send garbage to player (simplified — add to board)
                        for (i in 0 until garbageFromAI) {
                            engine?.board?.addGarbageRow(kotlin.random.Random.nextInt(10))
                        }
                    }
                }

                // Update game with mode logic
                engine?.let { eng ->
                    if (eng.status == GameStatus.COUNTDOWN) {
                        eng.updateCountdown(delta)
                    } else if (eng.status == GameStatus.PLAYING) {
                        (eng as ModeGameEngine).updateMode(delta)
                    }

                    // Phase 8: detect game-over transition
                    if (eng.isGameOver && !continuePromptShown) {
                        continuePromptShown = true
                        onGameOverFirstDetected(eng)
                    }

                    gameView.invalidate()

                    scoreText.text = "Score: ${eng.score}"
                    levelText.text = "Level: ${eng.level}"
                    linesText.text = "Lines: ${eng.lines}"
                    comboText.text = if (eng.combo > 0) "Combo: ×${eng.combo}" else ""

                    if (statusText.text.isEmpty()) {
                        statusText.text = when {
                            eng.isGameOver -> "انتهت اللعبة"
                            eng.isPaused -> "إيقاف مؤقت"
                            eng.status == GameStatus.COUNTDOWN -> "عدد: ${eng.countdown}"
                            else -> ""
                        }
                    }
                }

                handler.postDelayed(this, 16)
            }
        }
        handler.post(runnable)
    }

    /**
     * Phase 8 — called once when the game-over state is first detected.
     *
     * 1. Records the result (stats, achievements, leaderboard).
     * 2. Records mission/adventure/daily completion if won.
     * 3. Awards coins via StoreManager.
     * 4. Shows a continue dialog (only for topout, not for wins).
     */
    private fun onGameOverFirstDetected(eng: ModeGameEngine) {
        val result = eng.getGameResult()
        val runtimeMs = System.currentTimeMillis() - lastUpdateTime + 1L

        // Update player stats
        StatsManager.applyGameResult(this, result.copy(
            piecesPlaced = eng.piecesPlaced,
            tetrises = eng.tetrises,
            tspinFull = eng.tspins,
            perfectClears = eng.perfectClears,
            maxCombo = eng.maxCombo,
            maxB2b = eng.maxB2b
        ), runtimeMs)

        // Update achievements
        val metrics = StatsManager.load(this)
        AchievementDefinitions.updateUnlocked(this, metrics)

        // Submit to leaderboard
        LeaderboardManager.submit(this, eng.mode, LeaderboardEntry(
            score = eng.score,
            lines = eng.lines,
            level = eng.level,
            timestampMs = System.currentTimeMillis()
        ))

        // Award coins (game-end reward formula)
        val reward = StoreManager.computeReward(eng.lines, eng.score)
        StoreManager.addCoins(this, reward)

        // Track mode-specific completions
        when (eng.mode) {
            GameMode.MISSION -> {
                if (result.isWin && missionIndex >= 0) {
                    MissionProgressManager.completeMission(this, missionIndex)
                }
            }
            GameMode.ADVENTURE -> {
                if (result.isWin && adventureIndex >= 0) {
                    val stage = AdventureProgressManager.getStage(this, adventureIndex)
                    val tokens = stage?.stage?.tokens?.toSet() ?: emptySet()
                    AdventureProgressManager.completeStage(this, adventureIndex, tokens)
                }
            }
            GameMode.DAILY -> {
                if (result.isWin) {
                    DailyChallengeManager.recordWin(this, eng.score)
                }
            }
            else -> {}
        }

        // Show continue dialog only on topout (not on win or time-up)
        if (result.overReason == GameOverReason.TOPOUT) {
            showContinueDialog(eng)
        }
    }

    /**
     * Phase 8.7 — show the continue dialog.
     * Options: free ad continue, coin continue, free "clear top 10", or quit.
     */
    private fun showContinueDialog(eng: ModeGameEngine) {
        val state = ContinueSystem.getState(this)
        val coins = StoreManager.getCoins(this)

        val message = buildString {
            append("انتهت اللعبة!\n")
            append("النقاط: ${eng.score}\n\n")
            append("استمرار بالإعلان: ${state.adContinuesRemaining}/3 مجاناً\n")
            append("استمرار بعملات: ${state.coinContinueCost} (لديك $coins)\n")
            append("أو مسح 10 صفوف علوية مجاناً (بدون حماية النقاط)")
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("استمرار؟")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("إعلان مجاني") { _, _ ->
                if (state.adContinuesRemaining > 0) {
                    // In a real app, show the ad here. For now, grant immediately.
                    ContinueSystem.recordAdContinue(this)
                    if (ContinueSystem.applyContinue(eng)) {
                        continuePromptShown = false
                        statusText.text = ""
                    }
                } else {
                    android.widget.Toast.makeText(this, "لا توجد استمرارات إعلانية متبقية اليوم", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("عملات (${state.coinContinueCost})") { _, _ ->
                if (ContinueSystem.recordCoinContinue(this)) {
                    if (ContinueSystem.applyContinue(eng)) {
                        continuePromptShown = false
                        statusText.text = ""
                    }
                } else {
                    android.widget.Toast.makeText(this, "عملات غير كافية", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("مسح 10 صفوف") { _, _ ->
                ContinueSystem.recordFreeContinue(this)
                if (ContinueSystem.applyContinue(eng)) {
                    continuePromptShown = false
                    statusText.text = ""
                }
            }
            .setOnCancelListener { /* do nothing — game stays over */ }
            .create()
        dialog.show()
    }
}
