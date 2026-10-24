---
Task ID: phase-7
Agent: main
Task: أكمل المرحلة 7 — Input system (touch + keyboard + gamepad + haptics) + secure keystore backup

Work Log:
- Verified Phase 6 release v1.6.0 at commit a7baf01 (already on GitHub, tagged)
- === Secure signing ===
- Encoded debug.keystore as base64 (2772 bytes → 3696 chars)
- Created /home/z/my-project/scripts/push_signing_secrets.py using pynacl sealed box
- Pushed 4 GitHub Actions secrets (HTTP 201 each):
  * SIGNING_KEYSTORE_BASE64 — base64-encoded keystore binary
  * SIGNING_KEYSTORE_PASSWORD=android
  * SIGNING_KEY_ALIAS=tetris-prime
  * SIGNING_KEY_PASSWORD=android
- Updated .github/workflows/build.yml — added "Restore keystore" step that decodes secret to debug.keystore at build time (falls back to repo copy if secret absent)
- Updated app/build.gradle — signingConfigs now reads System.getenv('SIGNING_KEYSTORE_PASSWORD') etc. with literal fallback for local builds
- Created KEYSTORE_BACKUP.md documenting: 3 redundancy layers (secret + repo copy + local backup), recovery procedure, rotation guidance, signature verification commands

- === Phase 7: Input system ===
- Created InputManager.kt — unified InputSystem object with:
  * Action enum (10 actions: MOVE_LEFT/RIGHT, ROTATE_CW/CCW, SOFT_DROP_START/STOP, HARD_DROP, HOLD, PAUSE, RESTART)
  * Haptic enum (5 patterns: LIGHT/MEDIUM/HEAVY/SUCCESS/WARN with waveforms)
  * ButtonLayout data class (buttonId, xN, yN, sizeN, visible)
  * InputButton enum (8 buttons: LEFT, RIGHT, ROTATE_CW, ROTATE_CCW, SOFT_DROP, HARD_DROP, HOLD, PAUSE)
  * DefaultLayouts — RIGHT_HANDED and LEFT_HANDED (mirror)
  * LayoutStore — persistent JSON serialization to SharedPreferences (load/save/reset)
  * HapticController — uses VibratorManager on SDK 31+, falls back to Vibrator on older
  * KeyboardGamepadDispatcher — DAS=150ms, ARR=35ms for left/right repeat; handles DPAD/ABXY/LB/RB/Start/Select + keyboard arrows/X/Z/Space/C/P/Enter
  * GestureDispatcher — swipe left/right=move, swipe down=soft drop, swipe up=hold, tap=rotate, long-press=hard-drop
  * TouchButtonOverlay — renders 8 customizable buttons on a FrameLayout parent (drag-to-move + DAS/ARR repeat)
- Created InputSettingsActivity.kt — UI to customize the 8 buttons:
  * Drag-to-move preview area (16:9)
  * Size slider (5%-40%)
  * Per-button visibility toggle
  * Button selector chips (4×2 grid of 8 buttons)
  * "Reset to default" + "Done" bottom actions
  * Persists changes immediately to LayoutStore
- Modified GameActivity.kt:
  * Added haptic + keyboardDispatcher + gestureDispatcher fields
  * onCreate: load Settings, init HapticController, init KeyboardGamepadDispatcher with dispatchAction lambda
  * If settings.gestures: init GestureDispatcher + setOnTouchListener on gameView
  * If settings.hideCtlButtons: hide controlsLayout
  * dispatchAction() — translates InputSystem.Action → GameEngine method calls
  * onKeyDown/onKeyUp overrides — delegate to keyboardDispatcher
  * setupControls() — added haptic feedback to all 8 existing XML buttons (light for move/rotate/soft/hold, medium for hard-drop)
- Modified SettingsActivity.kt — added "🎮 تخصيص الأزرار" button in اللعب section that opens InputSettingsActivity
- Modified AndroidManifest.xml — registered .InputSettingsActivity
- Bumped app/build.gradle versionCode 7→8, versionName 1.6.0→1.7.0
- Updated .github/workflows/build.yml VERSION 1.6.0→1.7.0

Stage Summary:
- Phase 7 COMPLETE.
- Keystore now redundantly stored: GitHub Actions secrets (encrypted, recoverable) + repo copy + documented backup procedure
- CI uses secret-based signing by default; repo copy is fallback
- 8 customizable buttons + gestures + keyboard + gamepad + 5 haptic patterns all wired through single InputSystem.Action funnel
- Same signing key preserved → users can install v1.7.0 over v1.6.0 without uninstalling
- Next: Phase 8 (Special features — daily/missions/adventure)
