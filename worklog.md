---
Task ID: phase-6
Agent: main
Task: أكمل المرحلة 6 — UI Screens (Settings/Stats/Achievements/Store/Tutorial/Leaderboard + ModeSelect improvements)

Work Log:
- Cloned repo from https://github.com/saidjte/tetris-prime-kotlin.git (verified at v1.5.0, versionCode=6)
- Confirmed Phase 0-5 files all present (GameEngine, ModeGameEngine, AudioEngine, ParticleSystem, FxThemeDefinitions, BackgroundRenderer, GameView, ModeSelectActivity, etc.)
- Created 13 new Kotlin source files implementing Phase 6 UI:
  * SettingsManager.kt — 18 settings (audio, FX, gameplay, language), JSON-encoded, persisted to SharedPreferences
  * SettingsActivity.kt — sectioned UI (toggles, sliders, spinners) with live-save
  * StatsManager.kt — 21 player metrics (PlayerMetrics data class), load/save/applyGameResult
  * StatsActivity.kt — 4 grouped cards (aggregate, clear types, records, completions)
  * AchievementDefinitions.kt — 18 achievements with currentValue lambdas + loadUnlocked/updateUnlocked
  * AchievementsActivity.kt — progress bars per achievement with locked/unlocked styling
  * StoreManager.kt — currency (initial 200, computeReward formula), owned items per category, equipped items
  * StoreActivity.kt — 3-tab UI (FX/Themes/Voice) with buy/equip actions and color swatches
  * TutorialDefinitions.kt — 12-step walkthrough with icon, title, body, optional highlight
  * TutorialActivity.kt — prev/next navigation + skip button
  * LeaderboardManager.kt — per-mode top 10, time-based vs score-based sorting, wouldQualify check
  * LeaderboardActivity.kt — mode picker + ranked table with 🥇🥈🥉 medals
- Modified ModeSelectActivity.kt — added 3×2 navigation rail (Settings/Stats/Achievements/Store/Tutorial/Leaderboard) below mode grid
- Modified ModeConfig.kt — extended GameResult data class with optional detailed stats fields (piecesPlaced, singleClears, doubleClears, tripleClears, tetrises, tspinMini, tspinFull, perfectClears, maxCombo, maxB2b)
- Modified VoiceManager.kt — moved voicePacks map from instance property to companion object so StoreActivity can access statically as VoiceManager.voicePacks
- Modified AndroidManifest.xml — registered 6 new activities (Settings/Stats/Achievements/Store/Tutorial/Leaderboard)
- Modified app/build.gradle — versionCode 6→7, versionName 1.5.0→1.6.0
- Modified .github/workflows/build.yml — VERSION 1.5.0→1.6.0
- Committed as `440aa10 Phase 6: UI Screens (Settings/Stats/Achievements/Store/Tutor...`
- Pushed to main; CI run #40046674017 PASSED
- Downloaded APK artifact (4,766,615 bytes) + source zip (148,902 bytes)
- Created GitHub Release v1.6.0 (release_id=422429720) with title "Tetris Prime Kotlin v1.6.0 — Phase 6: UI Screens"
- Uploaded both APK and source zip as release assets
- Verified both download URLs return HTTP 200

Stage Summary:
- Phase 6 COMPLETE. 6 new UI screens wired up with persistent state and currency system.
- Same debug.keystore used → install over v1.5.0 to preserve save data.
- Release URL: https://github.com/saidjte/tetris-prime-kotlin/releases/tag/v1.6.0
- APK: https://github.com/saidjte/tetris-prime-kotlin/releases/download/v1.6.0/tetris-prime-kotlin-1.6.0.apk
- Source: https://github.com/saidjte/tetris-prime-kotlin/releases/download/v1.6.0/tetris-prime-kotlin-src-1.6.0.zip
- Next phases (7-11): Input, Special features, Translation, Storage, Performance
