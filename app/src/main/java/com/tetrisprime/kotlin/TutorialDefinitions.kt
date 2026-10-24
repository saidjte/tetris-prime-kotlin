package com.tetrisprime.kotlin

/**
 * 12-step tutorial covering controls, scoring, and special techniques.
 */
data class TutorialStep(
    val id: Int,
    val title: String,
    val body: String,
    val icon: String,
    val highlight: String? = null  // optional control hint to highlight
)

object TutorialDefinitions {

    val ALL: List<TutorialStep> = listOf(
        TutorialStep(
            1, "مرحباً بك في Tetris Prime",
            "هذا الشرح سيعلمك أساسيات اللعبة في 12 خطوة. اضغط التالي للمتابعة.",
            "👋"
        ),
        TutorialStep(
            2, "القطع (Tetrominoes)",
            "هناك 7 قطع بأشكال مختلفة: I, O, T, S, Z, J, L. كل قطعة تتكون من 4 مربعات.",
            "🧩"
        ),
        TutorialStep(
            3, "التحكم: الحركة",
            "اسحب يساراً أو يميناً لتحريك القطعة. استخدم أزرار الأسهم للتحكم الدقيق.",
            "👈", highlight = "move"
        ),
        TutorialStep(
            4, "التحكم: الدوران",
            "اضغط على زر الدوران لتدوير القطعة 90°. النظام الرسمي SRS يسمح بدوران سلس.",
            "🔄", highlight = "rotate"
        ),
        TutorialStep(
            5, "التحكم: الإسقاط الناعم والصلب",
            "السحب لأسفل = إسقاط ناعم (أسرع). زر الإسقاط الصلب = إسقاط فوري للقاع.",
            "⬇️", highlight = "drop"
        ),
        TutorialStep(
            6, "تعبئة الخطوط",
            "املأ صفّاً كاملاً أفقياً ليُمحى ويمنحك نقاطاً. كلما محوت أكثر في حركة واحدة، زادت النقاط.",
            "📊"
        ),
        TutorialStep(
            7, "Tetris (4 خطوط)",
            "محو 4 خطوط دفعة واحدة يُسمى Tetris ويمنحك 800 نقطة — الأفضل باستخدام قطعة I.",
            "🎯"
        ),
        TutorialStep(
            8, "T-Spin",
            "تدوير قطعة T في فتحة ضيقة بحيث تستحيل رؤوسها الثلاثة = T-Spin. هناك Mini و Full.",
            "💫"
        ),
        TutorialStep(
            9, "Back-to-Back (B2B)",
            "تنفيذ Tetris أو T-Spin متتالي (بدون مسح عادي بينهما) يمنحك مضاعف 1.5×.",
            "🔗"
        ),
        TutorialStep(
            10, "Combo (الكومبو)",
            "محو الخطوط في حركات متتالية بلا توقف يبني كومبو — كل خطوة تمنحك +50×كومبو×المستوى.",
            "⚡"
        ),
        TutorialStep(
            11, "Perfect Clear",
            "إفراغ اللوحة بالكامل بعد وضع قطعة يمنحك +2000×المستوى. مكافأة ضخمة!",
            "✨"
        ),
        TutorialStep(
            12, "أنت جاهز!",
            "تذكر: التدريب يصنع الإتقان. جرّب أنماطاً مختلفة من قائمة الأنماط. حظاً موفقاً!",
            "🏆"
        )
    )
}
