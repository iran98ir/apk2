package ir.apkbuilder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

/**
 * =========================================================
 * SplashActivity
 * Premium / App-Level Splash Renderer
 *
 * تمام اطلاعات محتوایی از app.json / AppConfig می‌آید.
 *
 * قانون:
 *  - پس‌زمینه از splash.bgMode / bg / bg2 / bgAngle
 *  - آیکون از splash.hasIcon / iconSvg / iconSize / ...
 *  - متن از splash.hasText / title / tSize / txt
 *  - محل قرارگیری از splash.iconPos / textPos
 *  - "hidden" → عنصر ساخته نمی‌شود
 *
 * این فایل فقط Renderer و Animation Engine است.
 * هیچ محتوای اختصاصی یک اپ در آن قرار نگرفته.
 * =========================================================
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout

    private var isLeaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val sp = config.splash

        /* =====================================================
           ROOT
           ===================================================== */

        root = FrameLayout(this).apply {

            background =
                SvgHelper.makeBackground(
                    sp.bgMode,
                    sp.bg,
                    sp.bg2,
                    sp.bgAngle
                )

            /*
             * صفحه از ابتدا کاملاً مخفی نیست؛
             * با Fade بسیار کوتاه وارد می‌شود.
             */
            alpha = 0f

            layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
        }

        /* =====================================================
           MAIN CONTENT
           ===================================================== */

        content = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

            setPadding(
                SvgHelper.dp(
                    this@SplashActivity,
                    PAD
                ),
                SvgHelper.dp(
                    this@SplashActivity,
                    PAD
                ),
                SvgHelper.dp(
                    this@SplashActivity,
                    PAD
                ),
                SvgHelper.dp(
                    this@SplashActivity,
                    PAD
                )
            )

            clipChildren = false
            clipToPadding = false
        }

        /*
         * سه بخش واقعی:
         *
         * TOP
         * CENTER
         * BOTTOM
         *
         * این روش باعث می‌شود iconPos/textPos
         * واقعاً قابل پیش‌بینی باشند.
         */
        val topBox = makeBox()
        val centerBox = makeBox()
        val bottomBox = makeBox()

        content.addView(topBox)
        content.addView(centerBox)
        content.addView(bottomBox)

        root.addView(content)

        /* =====================================================
           ICON
           ===================================================== */

        if (
            sp.hasIcon &&
            sp.iconPos != "hidden"
        ) {

            val iconHolder =
                FrameLayout(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER
                        }

                    /*
                     * حالت اولیه برای Animation
                     */
                    alpha = 0f

                    scaleX = INITIAL_ICON_SCALE
                    scaleY = INITIAL_ICON_SCALE

                    translationY =
                        SvgHelper.dp(
                            this@SplashActivity,
                            ICON_START_Y
                        ).toFloat()
                }

            SvgHelper.buildIconView(
                this,
                iconHolder,
                sp.iconSvg,
                sp.iconSize,
                sp.iconBoxSize,
                sp.iconBgOn,
                sp.iconBg,
                sp.iconShape,
                sp.iconColorOn,
                sp.iconColor
            )

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                sp.iconPos
            ).addView(
                iconHolder
            )

            /*
             * Animation ورود آیکون
             */
            animateIconIn(iconHolder)
        }

        /* =====================================================
           TEXT
           ===================================================== */

        if (
            sp.hasText &&
            sp.textPos != "hidden"
        ) {

            val tv =
                TextView(this).apply {

                    text =
                        sp.title

                    textSize =
                        sp.tSize.toFloat()

                    setTextColor(
                        SvgHelper.parseColor(
                            sp.txt
                        )
                    )

                    gravity =
                        Gravity.CENTER

                    typeface =
                        loadFont()

                    includeFontPadding =
                        true

                    alpha = 0f

                    translationY =
                        SvgHelper.dp(
                            this@SplashActivity,
                            TEXT_START_Y
                        ).toFloat()

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                }

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                sp.textPos
            ).addView(
                tv
            )

            /*
             * Animation ورود متن
             */
            animateTextIn(tv)
        }

        /* =====================================================
           CONTENT
           ===================================================== */

        setContentView(root)

        ThemeHelper.applySystemBars(
            this,
            config
        )

        /*
         * ابتدا کل صفحه وارد می‌شود.
         */
        playRootEntrance()

        /*
         * زمان اصلی Splash همان مقدار قبلی حفظ شده.
         *
         * این مقدار بخشی از رفتار فعلی اپ است
         * و چون در JSON فعلی فیلدی برای آن وجود ندارد،
         * آن را به JSON تحمیل نمی‌کنیم.
         */
        root.postDelayed(
            {
                goNext(config)
            },
            SPLASH_DELAY
        )
    }

    /* =========================================================
       BOXES
       ========================================================= */

    private fun makeBox(): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )

            clipChildren = false
            clipToPadding = false
        }
    }

    /* =========================================================
       PICK POSITION
       ========================================================= */

    private fun pickBox(
        top: LinearLayout,
        center: LinearLayout,
        bottom: LinearLayout,
        pos: String
    ): LinearLayout {

        return when (pos) {

            "top" ->
                top

            "bottom" ->
                bottom

            "center" ->
                center

            else ->
                center
        }
    }

    /* =========================================================
       ROOT ENTRANCE
       ========================================================= */

    private fun playRootEntrance() {

        root.animate()
            .alpha(1f)
            .setDuration(
                ROOT_ENTER_DURATION
            )
            .setInterpolator(
                DecelerateInterpolator(1.5f)
            )
            .start()
    }

    /* =========================================================
       ICON ENTRANCE
       ========================================================= */

    private fun animateIconIn(
        view: View
    ) {

        val alpha =
            ObjectAnimator.ofFloat(
                view,
                View.ALPHA,
                0f,
                1f
            )

        val scaleX =
            ObjectAnimator.ofFloat(
                view,
                View.SCALE_X,
                INITIAL_ICON_SCALE,
                1f
            )

        val scaleY =
            ObjectAnimator.ofFloat(
                view,
                View.SCALE_Y,
                INITIAL_ICON_SCALE,
                1f
            )

        val translation =
            ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_Y,
                view.translationY,
                0f
            )

        AnimatorSet().apply {

            playTogether(
                alpha,
                scaleX,
                scaleY,
                translation
            )

            duration =
                ICON_ENTER_DURATION

            startDelay =
                ICON_ENTER_DELAY

            interpolator =
                OvershootInterpolator(
                    0.75f
                )

            start()
        }
    }

    /* =========================================================
       TEXT ENTRANCE
       ========================================================= */

    private fun animateTextIn(
        view: View
    ) {

        val alpha =
            ObjectAnimator.ofFloat(
                view,
                View.ALPHA,
                0f,
                1f
            )

        val translation =
            ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_Y,
                view.translationY,
                0f
            )

        AnimatorSet().apply {

            playTogether(
                alpha,
                translation
            )

            duration =
                TEXT_ENTER_DURATION

            startDelay =
                TEXT_ENTER_DELAY

            interpolator =
                DecelerateInterpolator(
                    1.35f
                )

            start()
        }
    }

    /* =========================================================
       EXIT ANIMATION
       ========================================================= */

    private fun playExitAnimation(
        onFinished: () -> Unit
    ) {

        if (isFinishing) {
            return
        }

        /*
         * کل محتوای Splash با هم حرکت می‌کند،
         * اما Root Background تقریباً ثابت می‌ماند.
         *
         * نتیجه:
         *
         * Splash ناگهان غیب نمی‌شود.
         */
        val contentExit =
            content.animate()
                .translationY(
                    -SvgHelper.dp(
                        this,
                        EXIT_TRANSLATION_Y
                    ).toFloat()
                )
                .alpha(0f)
                .scaleX(
                    EXIT_SCALE
                )
                .scaleY(
                    EXIT_SCALE
                )
                .setDuration(
                    EXIT_DURATION
                )
                .setInterpolator(
                    DecelerateInterpolator(
                        1.4f
                    )
                )

        contentExit.withEndAction {

            /*
             * بعد از خروج محتوا،
             * Activity بعدی باز می‌شود.
             */
            onFinished()
        }

        contentExit.start()

        /*
         * پس‌زمینه فقط کمی Fade می‌شود.
         *
         * این باعث می‌شود کل صفحه
         * ناگهان سیاه/سفید نشود.
         */
        root.animate()
            .alpha(
                EXIT_ROOT_ALPHA
            )
            .setDuration(
                EXIT_DURATION
            )
            .setInterpolator(
                DecelerateInterpolator(
                    1.2f
                )
            )
            .start()
    }

    /* =========================================================
       NEXT SCREEN
       ========================================================= */

    private fun goNext(
        config: AppConfig
    ) {

        if (isLeaving) {
            return
        }

        isLeaving = true

        val prefs =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        /*
         * منطق فعلی دقیقاً حفظ شده.
         */
        val showOnb =
            config.onb.active &&
                config.onb.pages.isNotEmpty() &&
                (
                    !config.onb.firstRunOnly ||
                        !prefs.getBoolean(
                            KEY_ONB,
                            false
                        )
                    )

        val next =
            when {

                showOnb ->
                    OnboardingActivity::class.java

                config.welcome.active ->
                    WelcomeActivity::class.java

                else ->
                    WebActivity::class.java
            }

        /*
         * اول خروج نرم Splash.
         */
        playExitAnimation {

            /*
             * صفحه بعدی را بعد از خروج نرم باز می‌کنیم.
             */
            startActivity(
                Intent(
                    this,
                    next
                )
            )

            /*
             * Transition پیش‌فرض Android
             * حذف می‌شود تا با Animation ما تداخل نداشته باشد.
             */
            overridePendingTransition(
                0,
                0
            )

            finish()

            overridePendingTransition(
                0,
                0
            )
        }
    }

    /* =========================================================
       FONT
       ========================================================= */

    private fun loadFont(): Typeface? {

        return try {

            ResourcesCompat.getFont(
                this,
                R.font.lalezar
            )

        } catch (
            e: Exception
        ) {

            null
        }
    }

    /* =========================================================
       CONSTANTS
       ========================================================= */

    companion object {

        /*
         * فاصله داخلی.
         *
         * این مقدار Renderer است و
         * هیچ محتوایی از app.json را تغییر نمی‌دهد.
         */
        private const val PAD = 24

        /*
         * زمان Splash فعلی حفظ شده.
         *
         * چون در کانفیگ فعلی تو فیلد duration وجود ندارد،
         * متغیر جدیدی به JSON تحمیل نشده.
         */
        private const val SPLASH_DELAY = 2000L

        private const val PREFS = "app_prefs"

        const val KEY_ONB = "onb_shown"

        /* =====================================================
           ENTRANCE
           ===================================================== */

        private const val ROOT_ENTER_DURATION = 260L

        private const val ICON_ENTER_DELAY = 60L
        private const val ICON_ENTER_DURATION = 650L

        private const val TEXT_ENTER_DELAY = 180L
        private const val TEXT_ENTER_DURATION = 480L

        /*
         * شدت اولیه آیکون.
         *
         * فقط Animation است،
         * مقدار app.json نیست.
         */
        private const val INITIAL_ICON_SCALE = 0.78f

        /*
         * مقدار حرکت اولیه.
         */
        private const val ICON_START_Y = 28
        private const val TEXT_START_Y = 24

        /* =====================================================
           EXIT
           ===================================================== */

        private const val EXIT_DURATION = 430L

        private const val EXIT_TRANSLATION_Y = 22

        private const val EXIT_SCALE = 0.965f

        /*
         * Background تقریباً ثابت می‌ماند.
         */
        private const val EXIT_ROOT_ALPHA = 0.96f
    }
}
