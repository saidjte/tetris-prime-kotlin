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

## المرحلة 2: أنماط اللعب (v1.2.0)

### 9 أنماط لعب كاملة
- **CLASSIC**: ماراثون بلا نهاية بتسارع تدريجي
- **SPRINT**: 40 خط بأسرع وقت (فوز عند 40 خط)
- **ULTRA**: دقيقتان لتحقيق أعلى نتيجة (إعلانات 60s/30s/10s)
- **ZEN**: استرخاء بلا تسارع (مستوى ثابت 2)
- **CHEESE**: مسح خلايا الجبن (8 صفوف ابتدائية)
- **MISSION**: 100 مهمة متدرجة (8 أنواع)
- **ADVENTURE**: 36 مرحلة عبر 6 عوالم (8 أنواع رموز)
- **BATTLE**: مواجهة AI (3 مستويات صعوبة)
- **DAILY**: تحدي يومي محدد (FNV-1a seed)

### نظام فتح الأنماط
- SPRINT: 5 ألعاب
- ZEN: 10 ألعاب
- ADVENTURE: 150 خط
- ULTRA: 250 خط
- CHEESE: 500 خط

### Battle AI
- 3 مستويات: EASY (380ms, 30% خطأ), NORMAL (240ms, 12%), HARD (140ms, 3%)
- AI board مستقل مع تقييم الموضع (ارتفاع، ثقوب، نتوءات)
- نظام قمامة متبادل

### Mission System
- 100 مهمة: lines, score, tetrises, tspins, combo, pc, survive, level
- 8 أنواع رموز في المغامرة: ⭐🔶❤️⚡💎👑🌸🛡️
