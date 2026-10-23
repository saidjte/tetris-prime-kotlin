# Tetris Prime — Kotlin Native

لعبة تتريس مكتوبة بلغة Kotlin Native لأندرويد (بدون WebView).

## البنية

```
app/src/main/java/com/tetrisprime/kotlin/
├── Piece.kt           # SRS rotation + wall kicks
├── Board.kt           # 10×22 board, T-Spin, Perfect Clear, garbage
├── BagRandomizer.kt   # 7-bag + FNV-1a hash + SeededRandom
├── GameEngine.kt      # Full game logic (gravity, lock, B2B, combo, scoring)
├── GameView.kt        # Canvas rendering
└── MainActivity.kt    # Touch controls + game loop
```

## المميزات المنفّذة (المرحلة 1)

- ✅ SRS rotation مع wall kicks
- ✅ 7-bag randomizer
- ✅ صيغة الجاذبية الرسمية
- ✅ Lock delay (500ms, max 15 resets)
- ✅ T-Spin detection (3-corner rule)
- ✅ Perfect Clear detection
- ✅ Back-to-Back (1.5×)
- ✅ Combo system
- ✅ جداول النقاط الكاملة
- ✅ 9 أنماط لعب
- ✅ نظام العد التنازلي
- ✅ Seeded RNG للتحدي اليومي (FNV-1a)
- ✅ Danger zone detection

## البناء

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## CI

GitHub Actions يبني APK تلقائياً عند كل push إلى `main`.

## خطة الترحيل

راجع [MIGRATION_PLAN.md](MIGRATION_PLAN.md) للخطة الكاملة (11 مرحلة).
