# خطة ترحيل Tetris Prime من JavaScript إلى Kotlin Native

> **الهدف**: إعادة إنتاج اللعبة بمواصفات الأصل، محوّلة من JavaScript/React (WebView) إلى Kotlin Native (Android SDK).

---

## نظرة عامة

| الخاصية | الأصل (JS) | الهدف (Kotlin) |
|---------|-----------|----------------|
| اللغة | JavaScript (React/Next.js) | Kotlin |
| العرض | HTML5 Canvas + DOM | Android Canvas / Jetpack Compose |
| الصوت | Web Audio API | Android AudioTrack / Oboe |
| التخزين | localStorage | Room / DataStore |
| الحجم | 818 KB (JS bundle) | ~5-8 MB (APK) |
| المعمارية | Zustand store | StateFlow + Repository |

---

## المرحلة 0: البنية التحتية (أسبوع 1)

### 0.1 مشروع Gradle
- [ ] إعداد `build.gradle.kts` مع Kotlin DSL
- [ ] تبعيات: AndroidX, Compose, Room, Coroutines, Lifecycle
- [ ] `minSdk=26`, `targetSdk=34`
- [ ] ProGuard rules للحفاظ على data classes

### 0.2 بنية الحزم
```
com.tetrisprime.kotlin/
├── data/           # Models + persistence
├── domain/         # Game logic (pure Kotlin)
├── audio/          # Audio engine
├── ui/             # Compose UI
├── input/          # Touch/keyboard/gamepad
└── util/           # Helpers
```

### 0.3 CI/CD
- [ ] GitHub Actions workflow (`build-kotlin.yml`) ✅ (مُنجز)
- [ ] توقيع Release APK
- [ ] إنشاء Release يدوي عبر REST API

---

## المرحلة 1: محرك اللعبة الأساسي (أسبوع 1-2)

### 1.1 نماذج القطع (Piece.kt) ✅ مُنجز
- [x] 7 أنواع: I, O, T, S, Z, J, L
- [x] 4 حالات دوران لكل قطعة (SRS)
- [x] Wall kicks (JLSTZ + I-piece)
- [ ] **TODO**: التحقق من تطابق بيانات SRS مع الأصل

### 1.2 اللوحة (Board.kt) ✅ مُنجز
- [x] شبكة 10×20 (الأصل 10×22 — **TODO**: تصحيح إلى 22 صف)
- [x] كشف/مسح الخطوط
- [x] Ghost piece
- [ ] **TODO**: دعم علامات خاصة (`"G"` للقمامة, `"C"` للجبن)

### 1.3 مولّد 7-Bag (BagRandomizer.kt) ✅ مُنجز
- [x] Fisher-Yates shuffle
- [ ] **TODO**: دعم seeded RNG للتحدي اليومي (FNV-1a hash)

### 1.4 محرك اللعبة (GameEngine.kt) ✅ مُنجز جزئياً
- [x] الحركة، الدوران، الإسقاط
- [x] Hold piece
- [x] نظام النقاط الأساسي
- [ ] **TODO**: صيغة الجاذبية الرسمية: `max(30, round(1000 * (0.8 - (level-1)*0.007)^(level-1)))`
- [ ] **TODO**: Lock delay (500ms) + lock resets (max 15)
- [ ] **TODO**: T-Spin detection (قاعدة الـ3 أركان)
- [ ] **TODO**: Perfect Clear detection
- [ ] **TODO**: Back-to-Back (1.5× multiplier)
- [ ] **TODO**: Combo system (+50 × combo × level)
- [ ] **TODO**: جداول النقاط الكاملة:
  - عادي: `[0, 100, 300, 500, 800]`
  - T-Spin: `[0, 800, 1200, 1600]`
  - Perfect Clear: `+2000 × level`

---

## المرحلة 2: أنماط اللعب (أسبوع 3)

### 2.1 9 أنماط لعب
| النمط | الحالة | المتطلبات |
|-------|--------|-----------|
| classic | TODO | مستوى متصاعد، كل 10 خطوط |
| sprint | TODO | 40 خط بأسرع وقت |
| ultra | TODO | دقيقتان، أعلى نتيجة |
| zen | TODO | بدون جاذبية متسارعة |
| cheese | TODO | مسح خلايا "C" |
| mission | TODO | 100 مهمة (8 أنواع) |
| adventure | TODO | 36 مرحلة، 6 عوالم، 8 رموز |
| battle | TODO | خصم AI (3 مستويات) |
| daily | TODO | تحدي يومي محدد (FNV-1a seed) |

### 2.2 فتح الأنماط
- [ ] sprint: 5 ألعاب
- [ ] zen: 10 ألعاب
- [ ] adventure: 150 خط
- [ ] ultra: 250 خط
- [ ] cheese: 500 خط

---

## المرحلة 3: نظام الصوت (أسبوع 4)

### 3.1 مؤثرات صوتية (22 عينة)
```
sfx_move, sfx_rotate, sfx_soft, sfx_hard, sfx_lock, sfx_hold, sfx_click, sfx_deny,
sfx_clear1, sfx_clear2, sfx_clear3, sfx_tetris, sfx_tspin, sfx_tspin2, sfx_tspin3,
sfx_levelup, sfx_danger, sfx_save, sfx_record, sfx_tick, sfx_go, sfx_over
```
- [ ] تحميل ملفات MP3 من `assets/sfx/`
- [ ] AudioTrack أو SoundPool للتشغيل منخفض الكمون
- [ ] تحكم في الصوت (`sfxVol`)

### 3.2 التعليق الصوتي (38 حدث، 13 حزمة)
- [ ] تحميل مقاطع MP3 من `assets/voices/`
- [ ] 13 حزمة صوتية (raffina, giant, chipmunk, cyber, stadium, echo, radio, twin, alien, calm, whisper, hero, sage)
- [ ] تأثيرات الصوت per-pack (تغيير النغمة، صدى، إلخ)
- [ ] Cooldown per event (2200ms افتراضي، 1200ms للـ high priority)
- [ ] Captions على الشاشة

### 3.3 الموسيقى (3 مسارات مُركّبة)
- [ ] **menu**: 84 BPM, 4 sub-beats
- [ ] **tetris**: 149 BPM, 2 sub-beats
- [ ] **drive**: 118 BPM, 4 sub-beats
- [ ] تركيب باستخدام oscillator + gain envelope
- [ ] 4-chord progression + melody (38 note) + bass (8 note)

---

## المرحلة 4: نظام الجسيمات والتأثيرات (أسبوع 5)

### 4.1 22 حزمة جسيمات
| الحزمة | السلوك |
|--------|--------|
| classic | انفجار افتراضي + 2 دوائر توهج |
| burst | انفجار شعاعي (260-560 px/s) |
| stars | نجوم خماسية تصاعدية |
| bubbles | فقاعات بطيئة تصاعدية |
| electric | جسيمات أفقية سريعة |
| fireworks | متعدد المراحل |
| neonrain | خطوط عمودية هابطة |
| crystal | شظايا هندسية دوارة |
| phoenix | آثار لهب تصاعدية |
| party | انفجار ملوّن |
| galaxy | دوامة منحنية |
| aurora | تيارات متموجة |
| supernova | انفجار نجمي ضخم |
| tsunami | موجة أفقية |
| inferno | لهب أحمر/برتقالي |
| blizzard | ثلج أبيض/أزرق |
| volcano | ثوران تصاعدي |
| earthquake | اهتزاز بني/ذهبي |
| tornado | حركة حلزونية |
| lightning | بروق متعرجة |
| rainbow | طيف 7 ألوان |
| cosmic | سديم بنفسجي/سيان |

### 4.2 مستويات الجسيمات (6 مستويات)
- [ ] off (0×), low (0.3×), medium (0.5×), high (1.0×), ultra (1.5×), legendary (2.2×)
- [ ] تطبيق المضاعف على: العدد، الحجم، السرعة، العمر، التوهج، القصاصات

### 4.3 تأثيرات أخرى
- [ ] Bloom (تدرج شعاعي)
- [ ] Confetti (6 ألوان، ميلان عشوائي)
- [ ] Floating text (14 عبارة احتفالية)
- [ ] Danger zone (توهج أحمر عند امتلاء اللوحة)

---

## المرحلة 5: الثيمات والخلفيات (أسبوع 6)

### 5.1 51 ثيم
- [ ] كل ثيم: 7 ألوان (I, O, T, S, Z, J, L)
- [ ] خصائص إضافية: `boardBg`, `boardPattern`
- [ ] الثيمات: classic, neon, candy, retro, fire, ice, sakura, mono, stone, ocean, desert, meadow, popArt, duoCoral, duoMint, gold, sunset, aurora, forest, midnight, matcha, bamboo, pitch, court, toxic, matrix, neonpunk, vaporwave, royal, zellige, marrakesh, chaouen, fez, lava, tropical, cube3d, gem3d, glass, galaxy, deepSpace, nebula, crystal, metal, ruby, aqua, emerald, amethyst, blaze, shadow, colorSafe

### 5.2 9 خلفيات متحركة
- [ ] synthwave (sun-grid), midnight (twinkle), starfield (twinkle), aurora (aurora), sunset (dunes), ocean (bubbles), forest (fireflies), zellige (mosaic), mono (none)

---

## المرحلة 6: واجهة المستخدم (أسبوع 7-8)

### 6.1 الشاشات
- [ ] القائمة الرئيسية (carousel الأنماط)
- [ ] شاشة اللعب (لوحة + أزرار + إحصائيات)
- [ ] الإعدادات (54 إعداد)
- [ ] الإحصائيات (21 مقياس)
- [ ] الإنجازات (18 إنجاز)
- [ ] المهمات (100 مهمة)
- [ ] المغامرة (36 مرحلة)
- [ ] المتجر (FX + ثيمات + معلقين)
- [ ] الشرح (12 خطوة)
- [ ] لوحة الصدارة (top 10 per mode)

### 6.2 أزرار التحكم
- [ ] 8 أزرار قابلة للتخصيص (موضع + حجم)
- [ ] إخفاء/إظهار الأزرار
- [ ] تخطيط افتراضي محفوظ

### 6.3 الإعدادات (18 إعداد)
```kotlin
data class Settings(
    val lang: String = "auto",
    val music: Boolean = true,
    val musicVol: Float = 0.6f,
    val sfx: Boolean = true,
    val sfxVol: Float = 0.8f,
    val voice: Boolean = true,
    val voiceVol: Float = 0.9f,
    val voiceFreq: String = "normal",
    val captions: Boolean = true,
    val fx: String = "high",
    val gestures: Boolean = true,
    val next5: Boolean = true,
    val garbage: String = "off",
    val battleLevel: String = "normal",
    val gamepad: Boolean = true,
    val pieceSymbols: Boolean = false,
    val allowRotation: Boolean = false,
    val hideCtlButtons: Boolean = false
)
```

---

## المرحلة 7: المدخلات (أسبوع 9)

### 7.1 اللمس
- [ ] 8 أزرار قابلة للتخصيص
- [ ] Gesture controls (سحب، نقرة، swipe)

### 7.2 لوحة المفاتيح
- [ ] الأسهم، X/Z، Space، C، P، Enter

### 7.3 Gamepad
- [ ] D-pad، A/B/X/Y، LB/RB، Start
- [ ] DAS=150ms، ARR=35ms

### 7.4 الاهتزاز
- [ ] 5 أنماط: light, medium, heavy, success, warn

---

## المرحلة 8: المميزات الخاصة (أسبوع 10-11)

### 8.1 التحدي اليومي
- [ ] FNV-1a hash لـ `"goal/YYYY-MM-DD"`
- [ ] هدف: 9000 + (hash % 25) × 250
- [ ] seeded RNG لتسلسل القطع
- [ ] نظام streak

### 8.2 نظام المهمات
- [ ] 100 مهمة (8 أنواع: lines, score, tetrises, tspins, combo, pc, survive, level)
- [ ] فتح تدريجي

### 8.3 نظام المغامرة
- [ ] 36 مرحلة، 6 عوالم
- [ ] 8 أنواع رموز (⭐🔶❤️⚡💎👑🌸🛡️)
- [ ] جمع الرموز

### 8.4 وضع المعركة (AI)
- [ ] 3 مستويات صعوبة (easy/normal/hard)
- [ ] AI board مستقل
- [ ] نظام القمامة المتبادلة

### 8.5 الإنجازات
- [ ] 18 إنجاز
- [ ] دعم إنجازات مخصصة من الخادم

### 8.6 المتجر
- [ ] 22 حزمة جسيمات
- [ ] 13 حزمة معلقين
- [ ] 51 ثيم
- [ ] 9 خلفيات
- [ ] نظام العملات (200 بداية)
- [ ] مكافآت الإعلانات (3 يومياً)

### 8.7 الاستمرارية
- [ ] متابعة بمشاهدة إعلان
- [ ] متابعة بعملات
- [ ] مسح 10 صفوف علوية

### 8.8 أكواد الهدية
- [ ] تحقق من الخادم
- [ ] تتبع المستخدمة

---

## المرحلة 9: الترجمة (أسبوع 12)

### 9.1 16 لغة
| الكود | اللغة | الاتجاه |
|-------|-------|---------|
| ar | العربية | RTL |
| fr | Français | LTR |
| en | English | LTR |
| es | Español | LTR |
| ko | 한국어 | LTR |
| hi | हिन्दी | LTR |
| ru | Русский | LTR |
| zh | 中文 | LTR |
| tr | Türkçe | LTR |
| fa | فارسی | RTL |
| es-MX | Español (México) | LTR |
| it | Italiano | LTR |
| de | Deutsch | LTR |
| nl | Nederlands | LTR |
| de-CH | Schwiizerdütsch | LTR |
| ur | اردو | RTL |

- [ ] استخراج كل مفاتيح الترجمة من الأصل
- [ ] إنشاء `strings.xml` لكل لغة
- [ ] دعم RTL (ar, fa, ur)

---

## المرحلة 10: التخزين والاستمرارية (أسبوع 13)

### 10.1 Room Database
- [ ] جدول الإعدادات
- [ ] جدول الإحصائيات
- [ ] جدول السجلات (per mode)
- [ ] جدول لوحة الصدارة (top 10)
- [ ] جدول حالة المغامرة/المهمات
- [ ] جدول العناصر المملوكة (FX, themes, voices)

### 10.2 DataStore
- [ ] تفضيلات المستخدم
- [ ] حالة اللعبة السريعة

### 10.3 مخطط البيانات
- [ ] مطابقة `tetris-prime-v4` schema
- [ ] نظام backup (`tetris-prime-v4:bak`)

---

## المرحلة 11: الأداء والتحسين (أسبوع 14)

### 11.1 ملفات الأداء (4 مستويات)
- [ ] ultra: 72 FPS, 2.0× render, 44 particles
- [ ] high: 60 FPS, 1.5× render, 40 particles
- [ ] balanced: 46 FPS, 1.25× render, 28 particles
- [ ] eco: 32 FPS, 1.0× render, 18 particles
- [ ] كشف تلقائي بناءً على frame times

### 11.2 تحسينات
- [ ] SurfaceView للرسم (بدلاً من Compose Canvas للجسيمات الكثيفة)
- [ ] Object pooling للجسيمات
- [ ] Lazy loading للأصول الصوتية
- [ ] Compose stability للقوائم

---

## الجدول الزمني

| المرحلة | المدة | الأولوية |
|---------|------|----------|
| 0. البنية التحتية | 1 أسبوع | عالية |
| 1. محرك اللعبة | 2 أسبوع | عالية |
| 2. أنماط اللعب | 1 أسبوع | عالية |
| 3. الصوت | 1 أسبوع | متوسطة |
| 4. الجسيمات | 1 أسبوع | متوسطة |
| 5. الثيمات | 1 أسبوع | منخفضة |
| 6. واجهة المستخدم | 2 أسبوع | عالية |
| 7. المدخلات | 1 أسبوع | عالية |
| 8. مميزات خاصة | 2 أسبوع | متوسطة |
| 9. الترجمة | 1 أسبوع | متوسطة |
| 10. التخزين | 1 أسبوع | عالية |
| 11. الأداء | 1 أسبوع | متوسطة |
| **الإجمالي** | **~14 أسبوع** | |

---

## معايير القبول

- [ ] جميع الأنماط الـ9 تعمل
- [ ] T-Spin، Perfect Clear، B2B، Combo تعمل
- [ ] 22 حزمة جسيمات مع 6 مستويات
- [ ] 13 حزمة معلقين
- [ ] 51 ثيم + 9 خلفيات
- [ ] 16 لغة (3 RTL)
- [ ] 18 إنجاز + 100 مهمة + 36 مغامرة
- [ ] معركة AI (3 مستويات)
- [ ] تحدي يومي (seeded)
- [ ] لوحة صدارة محلية
- [ ] متجر كامل مع عملات
- [ ] إعدادات كاملة (18 إعداد)
- [ ] مدخلات: لمس + لوحة مفاتيح + gamepad
- [ ] حفظ/تحميل الحالة
- [ ] أداء ≥ 60 FPS على هاتف متوسط

---

## ملاحظات تقنية

1. **FNV-1a hash**: ضروري لتطابق التحدي اليومي عبر المنصات
2. **صيغة الجاذبية**: `max(30, round(1000 * (0.8 - (level-1)*0.007)^(level-1)))` — معتمدة عالمياً
3. **Lock delay**: 500ms مع max 15 resets — مطابق للـ Tetris Guideline
4. **T-Spin**: قاعدة الـ3 أركان + آخر حركة كانت دوران
5. **B2B**: Tetris أو T-Spin فقط يؤهل؛ المضاعف 1.5×
6. **Perfect Clear**: +2000 × level
7. **العملات**: `max(5, min(300, floor(2×lines + score/500)))` + مكافآت
