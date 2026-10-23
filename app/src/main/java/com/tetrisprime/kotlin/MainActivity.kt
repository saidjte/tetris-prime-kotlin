package com.tetrisprime.kotlin

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var levelText: TextView
    private lateinit var linesText: TextView
    private lateinit var comboText: TextView
    private lateinit var statusText: TextView

    private var lastUpdateTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        scoreText = findViewById(R.id.scoreText)
        levelText = findViewById(R.id.levelText)
        linesText = findViewById(R.id.linesText)
        comboText = findViewById(R.id.comboText)
        statusText = findViewById(R.id.statusText)

        setupControls()
    }

    private fun setupControls() {
        // Left/Right with repeat on hold
        setupRepeatButton(R.id.btnLeft) { gameView.gameEngine.move(-1) }
        setupRepeatButton(R.id.btnRight) { gameView.gameEngine.move(1) }

        findViewById<Button>(R.id.btnRotate).setOnClickListener { gameView.gameEngine.rotate(1) }

        // Soft drop — hold to drop
        val softDropBtn = findViewById<Button>(R.id.btnSoftDrop)
        softDropBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    gameView.gameEngine.setSoftDropping(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    gameView.gameEngine.setSoftDropping(false)
                }
            }
            false
        }

        findViewById<Button>(R.id.btnHardDrop).setOnClickListener { gameView.gameEngine.hardDrop() }
        findViewById<Button>(R.id.btnHold).setOnClickListener { gameView.gameEngine.hold() }
        findViewById<Button>(R.id.btnPause).setOnClickListener { gameView.gameEngine.togglePause() }
        findViewById<Button>(R.id.btnRestart).setOnClickListener {
            gameView.gameEngine.start()
        }
    }

    private fun setupRepeatButton(buttonId: Int, action: () -> Boolean) {
        val btn = findViewById<Button>(buttonId)
        var isRepeating = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val initialDelay = 150L // DAS
        val repeatDelay = 35L   // ARR

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

                gameView.update(delta)

                val engine = gameView.gameEngine
                scoreText.text = "Score: ${engine.score}"
                levelText.text = "Level: ${engine.level}"
                linesText.text = "Lines: ${engine.lines}"
                comboText.text = if (engine.combo > 0) "Combo: ×${engine.combo}" else ""

                statusText.text = when {
                    engine.isGameOver -> "GAME OVER"
                    engine.isPaused -> "PAUSED"
                    engine.status == GameStatus.COUNTDOWN -> "Countdown: ${engine.countdown}"
                    engine.status == GameStatus.PLAYING -> ""
                    else -> ""
                }

                handler.postDelayed(this, 16) // ~60 FPS
            }
        }
        handler.post(runnable)
    }
}
