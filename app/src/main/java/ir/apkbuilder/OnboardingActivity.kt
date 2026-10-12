package ir.apkbuilder

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * =========================================================
 * OnboardingActivity
 * Premium / App-Level Onboarding
 *
 * تمام مقادیر محتوایی از app.json / AppConfig خوانده می‌شوند.
 *
 * پشتیبانی:
 * - هر تعداد صفحه بر اساس onb.count
 * - محتوای صفحات از onb.pages
 * - Swipe طبیعی
 * - Transition نرم
 * - PageTransformer حرفه‌ای
 * - حالت 2 صفحه‌ای
 * - حالت 3 صفحه‌ای
 * - Swipe اضافه در آخرین صفحه → Welcome
 * - بدون نمایش صفحه جعلی
 * - بدون پرش
 * - بدون صفحه سوم لحظه‌ای در حالت count=2
 * =========================================================
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var dotsContainer: LinearLayout
    private lateinit var pager: ViewPager2
    private lateinit var root: FrameLayout

    private var totalPages: Int = 0
    private var currentPage: Int = 0

    private var touchStartX = 0f
    private var touchStartY = 0f

    private var isFinishingOnboarding = false

    private val swipeThreshold =
        SWIPE_THRESHOLD

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = ConfigLoader.load(this)

        val onb = config.onb

        /* =====================================================
           ROOT
           ===================================================== */

        root = FrameLayout(this).apply {
            background = SvgHelper.makeBackground(
                onb.bgMode,
                onb.bg,
                onb.bg2,
                onb.bgAngle
            )
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(col)

        /* =====================================================
           PAGES
           ===================================================== */

        val requestedCount =
            onb.count.toIntOrNull()
                ?: onb.pages.size

        /*
         * تعداد صفحات فقط از کانفیگ می‌آید.
         * اگر count بیشتر از صفحات موجود باشد،
         * فقط صفحات واقعی استفاده می‌شوند.
         */
        val visiblePages =
            onb.pages.take(
                requestedCount.coerceAtLeast(0)
            )

        totalPages = visiblePages.size

        /* =====================================================
           VIEWPAGER
           ===================================================== */

        pager = ViewPager2(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            /*
             * فقط صفحات واقعی کانفیگ.
             *
             * هیچ صفحه جعلی ساخته نمی‌شود.
             */
            adapter = OnbAdapter(
                visiblePages,
                onb.txt
            )

            /*
             * یک صفحه مجاور برای Transition نرم.
             */
            offscreenPageLimit = 1

            /*
             * جلوگیری از افکت Glow/Edge قدیمی RecyclerView.
             *
             * در آخرین صفحه نباید کشش سفید/خالی دیده شود.
             */
            overScrollMode = View.OVER_SCROLL_NEVER

            clipToPadding = false
            clipChildren = false

            /*
             * حساسیت طبیعی ViewPager2.
             */
            isUserInputEnabled = true
        }

        /*
         * Transition اصلی صفحات
         */
        pager.setPageTransformer(
            PremiumPageTransformer()
        )

        col.addView(pager)

        /* =====================================================
           DOTS
           ===================================================== */

        dotsContainer = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    SvgHelper.dp(
                        this@OnboardingActivity,
                        DOT_BOTTOM_MARGIN
                    )

                topMargin =
                    SvgHelper.dp(
                        this@OnboardingActivity,
                        DOT_TOP_MARGIN
                    )
            }
        }

        col.addView(dotsContainer)

        buildDots(
            onb.txt,
            0
        )

        /* =====================================================
           PAGE CHANGE
           ===================================================== */

        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(
                    position: Int
                ) {

                    currentPage = position

                    buildDots(
                        onb.txt,
                        position
                    )
                }
            }
        )

        /* =====================================================
           EXTRA SWIPE
           ===================================================== */

        /*
         * در آخرین صفحه:
         *
         * اگر کاربر دوباره به چپ Swipe کند،
         * ViewPager2 صفحه جدیدی ندارد.
         *
         * بنابراین ما Transition اختصاصی خودمان را اجرا می‌کنیم
         * و مستقیم Welcome را باز می‌کنیم.
         *
         * این باعث می‌شود:
         *
         * count = 2
         * page 1 → page 2 → Welcome
         *
         * count = 3
         * page 1 → page 2 → page 3 → Welcome
         *
         * بدون هیچ صفحه جعلی.
         */
        pager.getChildAt(0).setOnTouchListener { _, event ->

            handlePagerTouch(event)

            /*
             * false:
             * اجازه می‌دهیم ViewPager2 خودش
             * Swipe صفحات واقعی را انجام دهد.
             */
            false
        }

        /* =====================================================
           CONTENT
           ===================================================== */

        setContentView(root)

        ThemeHelper.applySystemBars(
            this,
            config
        )
    }

    /* =========================================================
       TOUCH HANDLER
       ========================================================= */

    private fun handlePagerTouch(
        event: MotionEvent
    ) {

        if (isFinishingOnboarding) {
            return
        }

        if (totalPages <= 0) {
            return
        }

        /*
         * فقط آخرین صفحه.
         */
        if (currentPage != totalPages - 1) {
            return
        }

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                touchStartX = event.x
                touchStartY = event.y
            }

            MotionEvent.ACTION_UP -> {

                val dx =
                    event.x - touchStartX

                val dy =
                    event.y - touchStartY

                /*
                 * فقط Swipe افقی واقعی.
                 */
                val horizontal =
                    kotlin.math.abs(dx) >
                        kotlin.math.abs(dy)

                /*
                 * در ViewPager:
                 * Swipe به چپ = dx منفی
                 */
                if (
                    dx < -swipeThreshold &&
                    horizontal
                ) {

                    finishOnboardingPremium()
                }
            }
        }
    }

    /* =========================================================
       PREMIUM FINISH TRANSITION
       ========================================================= */

    private fun finishOnboardingPremium() {

        if (isFinishingOnboarding) {
            return
        }

        isFinishingOnboarding = true

        /*
         * دیگر اجازه Swipe نده.
         */
        pager.isUserInputEnabled = false

        /*
         * View فعلی.
         */
        val currentView =
            pager.getChildAt(0)

        /*
         * انیمیشن بسیار نرم:
         *
         * صفحه کمی به چپ می‌رود
         * همزمان کمی Fade می‌شود
         * و کمی Scale می‌گیرد.
         *
         * حس Transition اپلیکیشن می‌دهد.
         */
        currentView?.animate()
            ?.translationX(
                -currentView.width * FINISH_TRANSLATION
            )
            ?.alpha(0f)
            ?.scaleX(FINISH_SCALE)
            ?.scaleY(FINISH_SCALE)
            ?.setDuration(FINISH_DURATION)
            ?.setInterpolator(
                android.view.animation.PathInterpolator(
                    0.16f,
                    1.0f,
                    0.3f,
                    1.0f
                )
            )
            ?.withEndAction {

                openWelcome()
            }
            ?.start()

        /*
         * خود پس‌زمینه تقریباً ثابت می‌ماند.
         * این موضوع باعث می‌شود Transition
         * حس اپلیکیشن داشته باشد نه صفحه وب.
         */
        root.animate()
            .alpha(FINISH_ROOT_ALPHA)
            .setDuration(FINISH_DURATION)
            .setInterpolator(
                android.view.animation.PathInterpolator(
                    0.16f,
                    1.0f,
                    0.3f,
                    1.0f
                )
            )
            .start()
    }

    /* =========================================================
       OPEN WELCOME
       ========================================================= */

    private fun openWelcome() {

        /*
         * ثبت اینکه Onboarding دیده شده.
         */
        getSharedPreferences(
            PREFS,
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                SplashActivity.KEY_ONB,
                true
            )
            .apply()

        val next =
            if (config.welcome.active) {
                WelcomeActivity::class.java
            } else {
                WebActivity::class.java
            }

        startActivity(
            Intent(
                this,
                next
            )
        )

        /*
         * جلوگیری از Transition پیش‌فرض
         * سیستم که ممکن است با انیمیشن ما تداخل کند.
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

    /* =========================================================
       DOTS
       ========================================================= */

    private fun buildDots(
        txtColor: String,
        current: Int
    ) {

        dotsContainer.removeAllViews()

        val baseColor =
            SvgHelper.parseColor(txtColor)

        val glowColor =
            withAlpha(
                baseColor,
                DOT_GLOW_ALPHA
            )

        val dotColor =
            withAlpha(
                baseColor,
                DOT_ALPHA
            )

        val dotSize =
            SvgHelper.dp(
                this,
                DOT_SIZE
            )

        val activeW =
            SvgHelper.dp(
                this,
                ACTIVE_DOT_WIDTH
            )

        val activeH =
            SvgHelper.dp(
                this,
                ACTIVE_DOT_HEIGHT
            )

        val gap =
            SvgHelper.dp(
                this,
                DOT_GAP
            )

        for (i in 0 until totalPages) {

            val isActive =
                i == current

            val wrap =
                FrameLayout(this).apply {

                    layoutParams =
                        LinearLayout.LayoutParams(
                            if (isActive)
                                activeW
                            else
                                dotSize,

                            if (isActive)
                                activeH
                            else
                                dotSize
                        ).apply {

                            marginStart =
                                if (i == 0)
                                    0
                                else
                                    gap
                        }
                }

            /* =================================================
               GLOW
               ================================================= */

            val glow =
                View(this).apply {

                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {

                            leftMargin =
                                -SvgHelper.dp(
                                    this@OnboardingActivity,
                                    3
                                )

                            rightMargin =
                                -SvgHelper.dp(
                                    this@OnboardingActivity,
                                    3
                                )

                            topMargin =
                                -SvgHelper.dp(
                                    this@OnboardingActivity,
                                    3
                                )

                            bottomMargin =
                                -SvgHelper.dp(
                                    this@OnboardingActivity,
                                    3
                                )
                        }

                    background =
                        android.graphics.drawable.GradientDrawable()
                            .apply {

                                shape =
                                    android.graphics.drawable.GradientDrawable.RECTANGLE

                                cornerRadius =
                                    if (isActive)
                                        (activeH + 6) / 2f
                                    else
                                        (dotSize + 6) / 2f

                                setColor(
                                    if (isActive)
                                        glowColor
                                    else
                                        withAlpha(
                                            baseColor,
                                            0.15f
                                        )
                                )
                            }

                    alpha =
                        if (isActive)
                            0.85f
                        else
                            0.4f
                }

            wrap.addView(glow)

            /* =================================================
               MAIN DOT
               ================================================= */

            val dot =
                View(this).apply {

                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )

                    background =
                        android.graphics.drawable.GradientDrawable()
                            .apply {

                                shape =
                                    android.graphics.drawable.GradientDrawable.RECTANGLE

                                cornerRadius =
                                    if (isActive)
                                        activeH / 2f
                                    else
                                        dotSize / 2f

                                setColor(
                                    dotColor
                                )
                            }

                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.P
                    ) {

                        outlineAmbientShadowColor =
                            glowColor

                        outlineSpotShadowColor =
                            glowColor
                    }

                    elevation =
                        SvgHelper.dp(
                            this@OnboardingActivity,
                            6
                        ).toFloat()
                }

            wrap.addView(dot)

            dotsContainer.addView(wrap)
        }
    }

    /* =========================================================
       PAGE TRANSFORMER
       ========================================================= */

    /**
     * Transition صفحات واقعی.
     *
     * خروج:
     * - کمی Fade
     * - کمی Scale
     * - حرکت افقی
     *
     * ورود:
     * - Fade in
     * - Scale up
     * - حرکت نرم
     *
     * هیچ مقدار محتوایی از JSON اینجا هاردکد نشده.
     */
    private class PremiumPageTransformer :
        ViewPager2.PageTransformer {

        override fun transformPage(
            page: View,
            position: Float
        ) {

            when {

                /*
                 * صفحه خارج‌شونده به چپ
                 */
                position <= -1f -> {

                    page.alpha = 0f

                    page.translationX = 0f

                    page.scaleX =
                        MIN_SCALE

                    page.scaleY =
                        MIN_SCALE
                }

                /*
                 * صفحه در حال حرکت
                 */
                position < 0f -> {

                    val p =
                        kotlin.math.abs(position)

                    page.alpha =
                        1f - (
                            p * FADE_AMOUNT
                        )

                    page.translationX =
                        -page.width *
                        position *
                        TRANSLATION_FACTOR

                    val scale =
                        MAX_SCALE -
                            (
                                p *
                                (MAX_SCALE - MIN_SCALE)
                            )

                    page.scaleX = scale
                    page.scaleY = scale
                }

                /*
                 * صفحه فعال
                 */
                position == 0f -> {

                    page.alpha = 1f

                    page.translationX = 0f

                    page.scaleX = 1f
                    page.scaleY = 1f
                }

                /*
                 * صفحه‌ای که از سمت راست وارد می‌شود
                 */
                position <= 1f -> {

                    val p = position

                    page.alpha =
                        1f - (
                            p * FADE_AMOUNT
                        )

                    page.translationX =
                        -page.width *
                        p *
                        TRANSLATION_FACTOR

                    val scale =
                        MAX_SCALE -
                            (
                                p *
                                (MAX_SCALE - MIN_SCALE)
                            )

                    page.scaleX = scale
                    page.scaleY = scale
                }

                /*
                 * خارج از محدوده
                 */
                else -> {

                    page.alpha = 0f
                }
            }
        }
    }

    /* =========================================================
       UTILS
       ========================================================= */

    private fun withAlpha(
        color: Int,
        alpha: Float
    ): Int {

        val a =
            (
                alpha.coerceIn(
                    0f,
                    1f
                ) * 255
            ).toInt()

        return Color.argb(
            a,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun loadFont(): Typeface? =
        try {

            ResourcesCompat.getFont(
                this,
                R.font.lalezar
            )

        } catch (
            e: Exception
        ) {

            null
        }

    /* =========================================================
       ADAPTER
       ========================================================= */

    inner class OnbAdapter(
        private val pages: List<OnbPage>,
        private val txtColor: String
    ) : RecyclerView.Adapter<OnbAdapter.VH>() {

        inner class VH(
            v: View
        ) : RecyclerView.ViewHolder(v) {

            val boxTop: LinearLayout =
                v.findViewWithTag("top")

            val boxCenter: LinearLayout =
                v.findViewWithTag("center")

            val boxBottom: LinearLayout =
                v.findViewWithTag("bottom")
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VH {

            val ctx =
                parent.context

            val col =
                LinearLayout(ctx).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                    setPadding(
                        SvgHelper.dp(
                            ctx,
                            PAD
                        ),
                        SvgHelper.dp(
                            ctx,
                            PAD
                        ),
                        SvgHelper.dp(
                            ctx,
                            PAD
                        ),
                        SvgHelper.dp(
                            ctx,
                            PAD
                        )
                    )
                }

            val top =
                makeBox(ctx).apply {
                    tag = "top"
                }

            val center =
                makeBox(ctx).apply {
                    tag = "center"
                }

            val bottom =
                makeBox(ctx).apply {
                    tag = "bottom"
                }

            col.addView(top)
            col.addView(center)
            col.addView(bottom)

            return VH(col)
        }

        override fun onBindViewHolder(
            holder: VH,
            position: Int
        ) {

            val page =
                pages[position]

            val ctx =
                holder.itemView.context

            holder.boxTop.removeAllViews()
            holder.boxCenter.removeAllViews()
            holder.boxBottom.removeAllViews()

            /* =================================================
               ICON
               ================================================= */

            if (
                page.iconPos != "hidden" &&
                page.iconSvg.isNotBlank()
            ) {

                val holderBox =
                    FrameLayout(ctx)

                SvgHelper.buildIconView(
                    ctx,
                    holderBox,
                    page.iconSvg,
                    page.iconSize,
                    page.iconBoxSize,
                    page.iconBgOn,
                    page.iconBg,
                    page.iconShape,
                    page.iconColorOn,
                    page.iconColor
                )

                pickBox(
                    holder,
                    page.iconPos
                ).addView(
                    holderBox
                )
            }

            /* =================================================
               TEXT
               ================================================= */

            if (
                page.textPos != "hidden"
            ) {

                val textBox =
                    LinearLayout(ctx).apply {

                        orientation =
                            LinearLayout.VERTICAL

                        gravity =
                            Gravity.CENTER
                    }

                val tvTitle =
                    TextView(ctx).apply {

                        text =
                            page.t

                        textSize =
                            page.tSize.toFloat()

                        setTextColor(
                            SvgHelper.parseColor(
                                txtColor
                            )
                        )

                        gravity =
                            Gravity.CENTER

                        typeface =
                            loadFont()
                    }

                textBox.addView(
                    tvTitle
                )

                val tvDesc =
                    TextView(ctx).apply {

                        text =
                            page.d

                        textSize =
                            page.dSize.toFloat()

                        setTextColor(
                            SvgHelper.parseColor(
                                txtColor
                            )
                        )

                        gravity =
                            Gravity.CENTER

                        typeface =
                            loadFont()

                        setPadding(
                            0,
                            SvgHelper.dp(
                                ctx,
                                8
                            ),
                            0,
                            0
                        )
                    }

                textBox.addView(
                    tvDesc
                )

                pickBox(
                    holder,
                    page.textPos
                ).addView(
                    textBox
                )
            }
        }

        override fun getItemCount(): Int =
            pages.size

        private fun makeBox(
            ctx: android.content.Context
        ): LinearLayout =
            LinearLayout(ctx).apply {

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
            }

        private fun pickBox(
            holder: VH,
            pos: String
        ): LinearLayout =
            when (pos) {

                "top" ->
                    holder.boxTop

                "center" ->
                    holder.boxCenter

                "bottom" ->
                    holder.boxBottom

                else ->
                    holder.boxCenter
            }
    }

    /* =========================================================
       CONSTANTS
       ========================================================= */

    companion object {

        /*
         * اینها تنظیمات انیمیشن هستند،
         * نه تنظیمات محتوای Onboarding.
         * بنابراین وابسته به app.json نیستند.
         */

        private const val PAD = 24

        private const val SWIPE_THRESHOLD = 80f

        private const val PREFS = "app_prefs"

        /* Dots */
        private const val DOT_BOTTOM_MARGIN = 40
        private const val DOT_TOP_MARGIN = 20

        private const val DOT_SIZE = 8
        private const val ACTIVE_DOT_WIDTH = 26
        private const val ACTIVE_DOT_HEIGHT = 8
        private const val DOT_GAP = 8

        private const val DOT_GLOW_ALPHA = 0.45f
        private const val DOT_ALPHA = 0.95f

        /* Page Transition */
        private const val MIN_SCALE = 0.94f
        private const val MAX_SCALE = 1.0f

        private const val FADE_AMOUNT = 0.25f
        private const val TRANSLATION_FACTOR = 0.12f

        /* Final → Welcome */
        private const val FINISH_DURATION = 420L
        private const val FINISH_TRANSLATION = 0.18f
        private const val FINISH_SCALE = 0.985f
        private const val FINISH_ROOT_ALPHA = 0.98f
    }
}
