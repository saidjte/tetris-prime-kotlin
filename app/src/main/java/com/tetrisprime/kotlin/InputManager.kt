package com.tetrisprime.kotlin

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Phase 7 — unified input system.
 *
 * Coordinates four input subsystems:
 *  1. Touch   — 8 customizable on-screen buttons (position + size + hide/show) + gestures.
 *  2. Keyboard — arrows, X/Z (rotate), Space (hard drop), C (hold), P (pause), Enter (restart).
 *  3. Gamepad — D-pad, A/B/X/Y, LB/RB, Start; DAS=150ms, ARR=35ms.
 *  4. Haptics — 5 patterns (light, medium, heavy, success, warn).
 *
 * All subsystems funnel into a single Action enum so GameActivity doesn't care
 * which physical source produced the input.
 */
object InputSystem {

    /** Logical actions the game understands. */
    enum class Action {
        MOVE_LEFT,
        MOVE_RIGHT,
        ROTATE_CW,        // rotate clockwise (default rotation)
        ROTATE_CCW,       // rotate counter-clockwise
        SOFT_DROP_START,
        SOFT_DROP_STOP,
        HARD_DROP,
        HOLD,
        PAUSE,
        RESTART
    }

    /** Haptic patterns. */
    enum class Haptic(val pattern: LongArray) {
        LIGHT(longArrayOf(0, 20)),
        MEDIUM(longArrayOf(0, 40)),
        HEAVY(longArrayOf(0, 80)),
        SUCCESS(longArrayOf(0, 30, 60, 60)),
        WARN(longArrayOf(0, 100, 50, 100))
    }
}

/**
 * Customizable touch control layout.
 * 8 buttons: Left, Right, RotateCW, RotateCCW, SoftDrop, HardDrop, Hold, Pause.
 * Each has a normalized position (0..1, 0..1) and a normalized size (0.05..0.4).
 */
data class ButtonLayout(
    val buttonId: InputButton,
    val xN: Float,    // 0..1, center X relative to parent width
    val yN: Float,    // 0..1, center Y relative to parent height
    val sizeN: Float, // 0.05..0.4, fraction of parent width
    val visible: Boolean = true
)

enum class InputButton(val label: String, val defaultIcon: String) {
    LEFT("يسار", "◀"),
    RIGHT("يمين", "▶"),
    ROTATE_CW("دوران ↻", "↻"),
    ROTATE_CCW("دوران ↺", "↺"),
    SOFT_DROP("إسقاط ناعم", "▼"),
    HARD_DROP("إسقاط صلب", "⤓"),
    HOLD("تخزين", "H"),
    PAUSE("إيقاف", "❚❚")
}

/**
 * Default touch layout — semi-circular pad on lower half of screen.
 */
object DefaultLayouts {
    val RIGHT_HANDED: List<ButtonLayout> = listOf(
        // Left thumb: D-pad-ish cluster
        ButtonLayout(InputButton.LEFT,      0.10f, 0.82f, 0.14f),
        ButtonLayout(InputButton.RIGHT,     0.28f, 0.82f, 0.14f),
        ButtonLayout(InputButton.SOFT_DROP, 0.19f, 0.93f, 0.14f),
        // Right thumb: action cluster
        ButtonLayout(InputButton.ROTATE_CW, 0.72f, 0.78f, 0.13f),
        ButtonLayout(InputButton.ROTATE_CCW,0.88f, 0.78f, 0.13f),
        ButtonLayout(InputButton.HARD_DROP, 0.80f, 0.92f, 0.16f),
        ButtonLayout(InputButton.HOLD,      0.55f, 0.92f, 0.12f),
        ButtonLayout(InputButton.PAUSE,     0.95f, 0.06f, 0.08f)
    )

    val LEFT_HANDED: List<ButtonLayout> = RIGHT_HANDED.map {
        it.copy(xN = 1f - it.xN)
    }
}

/**
 * Persistent layout store.
 */
object LayoutStore {
    private const val PREFS = "tetris_prime_layout"
    private const val KEY_JSON = "layout_json_v1"

    fun load(ctx: Context): List<ButtonLayout> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null) ?: return DefaultLayouts.RIGHT_HANDED
        return try { parse(raw) } catch (e: Exception) { DefaultLayouts.RIGHT_HANDED }
    }

    fun save(ctx: Context, layout: List<ButtonLayout>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_JSON, serialize(layout)).apply()
    }

    fun reset(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun serialize(layout: List<ButtonLayout>): String =
        layout.joinToString(";") { "${it.buttonId.name},${it.xN},${it.yN},${it.sizeN},${if (it.visible) 1 else 0}" }

    private fun parse(raw: String): List<ButtonLayout> = raw.split(";").mapNotNull { row ->
        val p = row.split(",")
        if (p.size != 5) return@mapNotNull null
        try {
            ButtonLayout(
                buttonId = InputButton.valueOf(p[0]),
                xN = p[1].toFloat(),
                yN = p[2].toFloat(),
                sizeN = p[3].toFloat(),
                visible = p[4].toInt() == 1
            )
        } catch (e: Exception) { null }
    }
}

/**
 * Haptic feedback controller.
 */
class HapticController(private val ctx: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Whether the user has enabled haptics in settings. */
    var enabled: Boolean = true

    fun buzz(h: InputSystem.Haptic) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(h.pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(h.pattern, -1)
        }
    }

    fun light()   = buzz(InputSystem.Haptic.LIGHT)
    fun medium()  = buzz(InputSystem.Haptic.MEDIUM)
    fun heavy()   = buzz(InputSystem.Haptic.HEAVY)
    fun success() = buzz(InputSystem.Haptic.SUCCESS)
    fun warn()    = buzz(InputSystem.Haptic.WARN)
}

/**
 * Keyboard + gamepad dispatcher.
 *
 * Call [handleKey] from your Activity's onKeyDown / onKeyUp.
 * Returns true if the key was consumed.
 */
class KeyboardGamepadDispatcher(
    private val haptic: HapticController,
    private val onAction: (InputSystem.Action) -> Unit
) {
    // DAS/ARR for left/right held keys (matches Phase 7 spec: DAS=150ms, ARR=35ms)
    private val dasDelay = 150L
    private val arrRate  = 35L
    private var leftHeld = false
    private var rightHeld = false
    private var downHeld = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val leftRepeat = object : Runnable {
        override fun run() {
            if (leftHeld) { onAction(InputSystem.Action.MOVE_LEFT); handler.postDelayed(this, arrRate) }
        }
    }
    private val rightRepeat = object : Runnable {
        override fun run() {
            if (rightHeld) { onAction(InputSystem.Action.MOVE_RIGHT); handler.postDelayed(this, arrRate) }
        }
    }

    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // ---- Gamepad D-pad ----
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_GAMEPAD_DPAD_LEFT -> {
                if (!leftHeld) {
                    leftHeld = true
                    onAction(InputSystem.Action.MOVE_LEFT)
                    haptic.light()
                    handler.postDelayed(leftRepeat, dasDelay)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_GAMEPAD_DPAD_RIGHT -> {
                if (!rightHeld) {
                    rightHeld = true
                    onAction(InputSystem.Action.MOVE_RIGHT)
                    haptic.light()
                    handler.postDelayed(rightRepeat, dasDelay)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_GAMEPAD_DPAD_DOWN -> {
                if (!downHeld) {
                    downHeld = true
                    onAction(InputSystem.Action.SOFT_DROP_START)
                }
                return true
            }
            // ---- Rotate (keyboard X/Z + gamepad A/B) ----
            KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_GAMEPAD_DPAD_UP -> {
                onAction(InputSystem.Action.ROTATE_CW); haptic.light(); return true
            }
            KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BUTTON_B -> {
                onAction(InputSystem.Action.ROTATE_CCW); haptic.light(); return true
            }
            // ---- Hard drop (Space + gamepad X/Y) ----
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y -> {
                onAction(InputSystem.Action.HARD_DROP); haptic.medium(); return true
            }
            // ---- Hold (C + gamepad LB/RB) ----
            KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
                onAction(InputSystem.Action.HOLD); haptic.light(); return true
            }
            // ---- Pause (P + gamepad Start) ----
            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_BUTTON_START -> {
                onAction(InputSystem.Action.PAUSE); return true
            }
            // ---- Restart (Enter + gamepad Select) ----
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_SELECT -> {
                onAction(InputSystem.Action.RESTART); return true
            }
        }
        return false
    }

    fun handleKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_GAMEPAD_DPAD_LEFT -> {
                leftHeld = false; handler.removeCallbacks(leftRepeat); return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_GAMEPAD_DPAD_RIGHT -> {
                rightHeld = false; handler.removeCallbacks(rightRepeat); return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_GAMEPAD_DPAD_DOWN -> {
                downHeld = false; onAction(InputSystem.Action.SOFT_DROP_STOP); return true
            }
        }
        return false
    }
}

/**
 * Gesture dispatcher — interprets swipes/taps on the playfield surface.
 *
 * Swipe left/right = move; swipe down = soft drop start; tap = rotate;
 * long-press = hard drop; swipe up = hold.
 */
class GestureDispatcher(
    private val haptic: HapticController,
    private val onAction: (InputSystem.Action) -> Unit
) {
    private val swipeThreshold = 60f
    private val tapTimeout = 200L
    private val longPressTimeout = 400L

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var movedFar = false
    private var softDropping = false

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (!movedFar) {
            onAction(InputSystem.Action.HARD_DROP)
            haptic.medium()
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y
                downTime = System.currentTimeMillis()
                movedFar = false
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (kotlin.math.abs(dx) > swipeThreshold || kotlin.math.abs(dy) > swipeThreshold) {
                    if (!movedFar) {
                        movedFar = true
                        handler.removeCallbacks(longPressRunnable)
                        when {
                            kotlin.math.abs(dx) > kotlin.math.abs(dy) -> {
                                if (dx > 0) onAction(InputSystem.Action.MOVE_RIGHT)
                                else        onAction(InputSystem.Action.MOVE_LEFT)
                                haptic.light()
                            }
                            dy < 0 -> {
                                onAction(InputSystem.Action.HOLD); haptic.light()
                            }
                            dy > 0 -> {
                                onAction(InputSystem.Action.SOFT_DROP_START)
                                softDropping = true
                                haptic.light()
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (softDropping) {
                    onAction(InputSystem.Action.SOFT_DROP_STOP)
                    softDropping = false
                }
                if (!movedFar && System.currentTimeMillis() - downTime < tapTimeout) {
                    onAction(InputSystem.Action.ROTATE_CW)
                    haptic.light()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (softDropping) {
                    onAction(InputSystem.Action.SOFT_DROP_STOP)
                    softDropping = false
                }
            }
        }
        return true
    }
}

/**
 * Renders the 8 customizable on-screen buttons on top of a parent FrameLayout.
 * Each button reports its action via [onAction] and triggers haptic feedback.
 */
class TouchButtonOverlay(
    private val activity: AppCompatActivity,
    private val parent: FrameLayout,
    private val haptic: HapticController,
    private val onAction: (InputSystem.Action) -> Unit
) {
    private val buttons = mutableMapOf<InputButton, View>()
    private var layout: List<ButtonLayout> = LayoutStore.load(activity)
    private var hideAll: Boolean = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Re-render the overlay (call after layout change or screen resize). */
    fun render() {
        // Remove existing
        for (b in buttons.values) parent.removeView(b)
        buttons.clear()

        if (hideAll) return

        for (item in layout) {
            if (!item.visible) continue
            val v = buildButton(item)
            buttons[item.buttonId] = v
            parent.addView(v)
        }
    }

    fun setHideAll(hide: Boolean) {
        hideAll = hide
        render()
    }

    fun setLayout(newLayout: List<ButtonLayout>) {
        layout = newLayout
        LayoutStore.save(activity, newLayout)
        render()
    }

    fun resetToDefault() {
        LayoutStore.reset(activity)
        layout = DefaultLayouts.RIGHT_HANDED
        render()
    }

    private fun buildButton(item: ButtonLayout): View {
        val btn = TextView(activity).apply {
            text = item.buttonId.defaultIcon
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0x661E1B4B.toInt())
            val pad = 24
            setPadding(pad, pad, pad, pad)
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        // Defer size/position computation until parent is laid out
        parent.post {
            val pw = parent.width.toFloat()
            val ph = parent.height.toFloat()
            if (pw <= 0f || ph <= 0f) return@post
            val sizePx = (item.sizeN * pw).toInt().coerceIn(80, 240)
            val cx = (item.xN * pw).toInt()
            val cy = (item.yN * ph).toInt()
            val newLp = FrameLayout.LayoutParams(sizePx, sizePx)
            newLp.leftMargin = (cx - sizePx / 2).coerceAtLeast(0)
            newLp.topMargin = (cy - sizePx / 2).coerceAtLeast(0)
            btn.layoutParams = newLp

            // Make it a rounded square
            val d = android.graphics.drawable.GradientDrawable()
            d.setColor(0xCC1E1B4B.toInt())
            d.cornerRadius = sizePx * 0.3f
            d.setStroke(2, 0xFFA78BFA.toInt())
            btn.background = d
        }
        btn.layoutParams = lp

        // Wire action
        when (item.buttonId) {
            InputButton.LEFT, InputButton.RIGHT -> {
                setupRepeat(btn) {
                    onAction(
                        if (item.buttonId == InputButton.LEFT) InputSystem.Action.MOVE_LEFT
                        else InputSystem.Action.MOVE_RIGHT
                    )
                    haptic.light()
                    true
                }
            }
            InputButton.ROTATE_CW -> btn.setOnClickListener {
                onAction(InputSystem.Action.ROTATE_CW); haptic.light()
            }
            InputButton.ROTATE_CCW -> btn.setOnClickListener {
                onAction(InputSystem.Action.ROTATE_CCW); haptic.light()
            }
            InputButton.SOFT_DROP -> btn.setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { onAction(InputSystem.Action.SOFT_DROP_START); haptic.light() }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onAction(InputSystem.Action.SOFT_DROP_STOP)
                }
                false
            }
            InputButton.HARD_DROP -> btn.setOnClickListener {
                onAction(InputSystem.Action.HARD_DROP); haptic.medium()
            }
            InputButton.HOLD -> btn.setOnClickListener {
                onAction(InputSystem.Action.HOLD); haptic.light()
            }
            InputButton.PAUSE -> btn.setOnClickListener {
                onAction(InputSystem.Action.PAUSE)
            }
        }

        return btn
    }

    /**
     * DAS/ARR repeat for left/right movement buttons.
     * DAS = 150ms before first repeat, ARR = 35ms between repeats (matches Phase 7 spec).
     */
    private fun setupRepeat(view: View, action: () -> Boolean) {
        var repeating = false
        val dasDelay = 150L
        val arrRate = 35L
        val repeatRunnable = object : Runnable {
            override fun run() {
                if (repeating) {
                    action()
                    handler.postDelayed(this, arrRate)
                }
            }
        }
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    repeating = true
                    action()
                    handler.postDelayed(repeatRunnable, dasDelay)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    repeating = false
                    handler.removeCallbacks(repeatRunnable)
                }
            }
            false
        }
    }
}
