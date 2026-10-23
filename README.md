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

## المرحلة 3: نظام الصوت (v1.3.0)

### نظام SFX (22 مؤثر صوتي)
sfx_move, sfx_rotate, sfx_soft, sfx_hard, sfx_lock, sfx_hold, sfx_click,
sfx_clear1-3, sfx_tetris, sfx_tspin1-3, sfx_levelup, sfx_danger, sfx_save,
sfx_record, sfx_tick, sfx_go, sfx_over

### نظام Voice (38 حدث، 13 حزمة)
- 13 حزمة معلقين: raffina, giant, chipmunk, cyber, stadium, echo, radio, twin, alien, calm, whisper, hero, sage
- تأثيرات per-pack: pitch shift, reverb, echo, bandpass
- نظام cooldown (HIGH=1.2s, NORMAL=2.2s, LOW=6s)

### نظام الموسيقى (3 مسارات مركبة)
- menu (84 BPM), tetris (149 BPM), drive (118 BPM)
- تركيب إجرائي: chord progression + melody + bass
- AudioTrack ل real-time audio generation

### AudioEngine
- موحد للأنظمة الثلاثة
- مرتبط بأحداث محرك اللعبة
- إدارة الموارد (init/release)

## المرحلة 4: نظام الجسيمات والتأثيرات (v1.4.0)

### نظام الجسيمات (22 حزمة)
classic, burst, stars, bubbles, electric, fireworks, neonrain, crystal,
phoenix, party, galaxy, aurora, supernova, tsunami, inferno, blizzard,
volcano, earthquake, tornado, lightning, rainbow, cosmic

### مستويات الجسيمات (6 مستويات)
off (0×), low (0.3×), medium (0.5×), high (1.0×), ultra (1.5×), legendary (2.2×)

### التأثيرات
- Bloom: تدرج شعاعي عند Tetris/T-Spin/Perfect Clear/Level Up
- Confetti: قصاصات ملوّنة عند Perfect Clear/فوز
- Floating text: عبارات احتفالية (TETRIS!, T-SPIN!, COMBO ×N, etc.)
- 12 نوع جسيمات: default, star, bubble, electric, firework, neon streak,
  crystal shard, phoenix flame, party confetti, galaxy swirl, aurora wave, lightning bolt

### الثيمات (13 ثيم)
classic, neon, colorSafe, crystal, ruby, glass, metal, blaze, aqua,
emerald, amethyst, gold, shadow

كل ثيم: 7 ألوان للقطع (I, O, T, S, Z, J, L) + خلفية اللوحة
