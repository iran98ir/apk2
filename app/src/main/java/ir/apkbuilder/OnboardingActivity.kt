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
import kotlin.math.abs

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
 * - Transition محسوس و Premium
 * - PageTransformer حرفه‌ای
 * - حالت 2 صفحه‌ای
 * - حالت 3 صفحه‌ای
 * - Swipe اضافه در آخرین صفحه → Welcome
 * - بدون صفحه جعلی
 * - بدون هاردکد محتوای اپ
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

            /*
             * شروع بسیار کوتاه با Fade
             * برای جلوگیری از Flash اولیه.
             */
            alpha = 0f
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            clipChildren = false
            clipToPadding = false
        }

        root.addView(col)

        /* =====================================================
           PAGES
           ===================================================== */

        val requestedCount =
            onb.count.toIntOrNull()
                ?: onb.pages.size

        /*
         * فقط صفحات واقعی کانفیگ.
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

            adapter = OnbAdapter(
                visiblePages,
                onb.txt
            )

            offscreenPageLimit = 1

            overScrollMode =
                View.OVER_SCROLL_NEVER

            clipToPadding = false
            clipChildren = false

            isUserInputEnabled = true

            /*
             * اجازه می‌دهیم صفحات در محدوده
             * طبیعی خودشان آزادانه Transform شوند.
             */
            (getChildAt(0) as? RecyclerView)?.apply {
                clipToPadding = false
                clipChildren = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }
        }

        /*
         * Transition اصلی و محسوس.
         */
        pager.setPageTransformer(
            PremiumPageTransformer()
        )

        col.addView(pager)

        /* =====================================================
           DOTS
           ===================================================== */

        dotsContainer = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER

            layoutParams =
                LinearLayout.LayoutParams(
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

        if (totalPages > 0) {
            buildDots(
                onb.txt,
                0
            )
        }

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

        pager.getChildAt(0).setOnTouchListener { _, event ->

            handlePagerTouch(event)

            /*
             * false:
             * خود ViewPager2 همچنان Swipe طبیعی را مدیریت می‌کند.
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

        /*
         * Fade اولیه بسیار کوتاه.
         */
        root.post {
            root.animate()
                .alpha(1f)
                .setDuration(INITIAL_FADE_DURATION)
                .setInterpolator(
                    android.view.animation.DecelerateInterpolator(
                        1.4f
                    )
                )
                .start()
        }
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
         * فقط روی آخرین صفحه،
         * Swipe اضافه را برای Welcome بررسی می‌کنیم.
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

                val horizontal =
                    abs(dx) > abs(dy)

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

        pager.isUserInputEnabled = false

        val currentView =
            pager.getChildAt(0)

        /*
         * Transition محسوس‌تر:
         *
         * صفحه:
         * - به چپ می‌رود
         * - کوچک می‌شود
         * - محو می‌شود
         *
         * این حرکت باید واضح دیده شود،
         * اما همچنان Premium باقی بماند.
         */
        currentView?.animate()
            ?.translationX(
                -currentView.width *
                    FINISH_TRANSLATION
            )
            ?.translationY(
                -SvgHelper.dp(
                    this,
                    10
                ).toFloat()
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
         * پس‌زمینه تقریباً ثابت می‌ماند.
         * این کار باعث می‌شود حرکت اصلی متعلق به محتوا باشد.
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
         * هیچ Transition پیش‌فرض اندروید
         * با Transition خودمان قاطی نمی‌شود.
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

        if (totalPages <= 0) {
            return
        }

        val baseColor =
            SvgHelper.parseColor(
                txtColor
            )

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

                    /*
                     * تغییر وضعیت Dot با Scale کوچک
                     * باعث می‌شود تغییر صفحه محسوس‌تر شود.
                     */
                    scaleX =
                        if (isActive)
                            1f
                        else
                            DOT_INACTIVE_SCALE

                    scaleY =
                        if (isActive)
                            1f
                        else
                            DOT_INACTIVE_SCALE
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

                    elevation =
                        SvgHelper.dp(
                            this@OnboardingActivity,
                            6
                        ).toFloat()
                }

            wrap.addView(dot)

            dotsContainer.addView(wrap)

            /*
             * ورود Dot فعال.
             */
            if (isActive) {

                wrap.scaleX =
                    DOT_ACTIVE_START_SCALE

                wrap.scaleY =
                    DOT_ACTIVE_START_SCALE

                wrap.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(DOT_ANIMATION_DURATION)
                    .setInterpolator(
                        android.view.animation.OvershootInterpolator(
                            1.15f
                        )
                    )
                    .start()
            }
        }
    }

    /* =========================================================
       PAGE TRANSFORMER
       ========================================================= */

    /**
     * =========================================================
     * محسوس‌تر از نسخه قبلی
     *
     * نسخه قبلی:
     * MIN_SCALE = 0.94
     * FADE = 0.25
     * TRANSLATION = 0.12
     *
     * نسخه جدید:
     * MIN_SCALE = 0.84
     * FADE = 0.55
     * TRANSLATION = 0.34
     *
     * بنابراین هنگام Swipe واقعاً دیده می‌شود.
     * =========================================================
     */
    private class PremiumPageTransformer :
        ViewPager2.PageTransformer {

        override fun transformPage(
            page: View,
            position: Float
        ) {

            when {

                /*
                 * کاملاً خارج از سمت چپ
                 */
                position <= -1f -> {

                    page.alpha = 0f

                    page.translationX = 0f

                    page.translationY = 0f

                    page.scaleX =
                        MIN_SCALE

                    page.scaleY =
                        MIN_SCALE
                }

                /*
                 * صفحه‌ای که در حال خروج است
                 */
                position < 0f -> {

                    val p =
                        abs(position)
                            .coerceIn(
                                0f,
                                1f
                            )

                    /*
                     * Fade واضح‌تر.
                     */
                    page.alpha =
                        1f -
                            (
                                p *
                                    FADE_AMOUNT
                            )

                    /*
                     * حرکت افقی محسوس.
                     */
                    page.translationX =
                        page.width *
                            position *
                            TRANSLATION_FACTOR

                    /*
                     * حرکت عمودی بسیار کم.
                     */
                    page.translationY =
                        page.height *
                            p *
                            VERTICAL_FACTOR

                    /*
                     * Scale محسوس ولی Premium.
                     */
                    val scale =
                        1f -
                            (
                                p *
                                    (1f - MIN_SCALE)
                            )

                    page.scaleX = scale
                    page.scaleY = scale
                }

                /*
                 * صفحه کاملاً فعال
                 */
                position == 0f -> {

                    page.alpha = 1f

                    page.translationX = 0f

                    page.translationY = 0f

                    page.scaleX = 1f
                    page.scaleY = 1f
                }

                /*
                 * صفحه جدید که از سمت راست وارد می‌شود
                 */
                position <= 1f -> {

                    val p =
                        position.coerceIn(
                            0f,
                            1f
                        )

                    /*
                     * صفحه جدید ابتدا کمی محو است
                     * و همزمان وارد می‌شود.
                     */
                    page.alpha =
                        1f -
                            (
                                p *
                                    FADE_AMOUNT
                            )

                    /*
                     * حرکت افقی واضح.
                     */
                    page.translationX =
                        -page.width *
                            p *
                            TRANSLATION_FACTOR

                    /*
                     * حرکت عمودی مخالف صفحه قبلی.
                     */
                    page.translationY =
                        -page.height *
                            p *
                            VERTICAL_FACTOR

                    /*
                     * Scale از 0.84 به 1.
                     */
                    val scale =
                        MIN_SCALE +
                            (
                                (1f - p) *
                                    (1f - MIN_SCALE)
                            )

                    page.scaleX = scale
                    page.scaleY = scale
                }

                /*
                 * خارج از محدوده
                 */
                else -> {

                    page.alpha = 0f

                    page.scaleX =
                        MIN_SCALE

                    page.scaleY =
                        MIN_SCALE
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

                    clipChildren = false
                    clipToPadding = false
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

                        includeFontPadding = true

                        setLineSpacing(
                            0f,
                            1.05f
                        )
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

                        includeFontPadding = true

                        setPadding(
                            0,
                            SvgHelper.dp(
                                ctx,
                                8
                            ),
                            0,
                            0
                        )

                        setLineSpacing(
                            0f,
                            1.10f
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

                clipChildren = false
                clipToPadding = false
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
         * Layout
         */
        private const val PAD = 24

        /*
         * Swipe
         */
        private const val SWIPE_THRESHOLD = 80f

        /*
         * SharedPreferences
         */
        private const val PREFS = "app_prefs"

        /*
         * Initial appearance
         */
        private const val INITIAL_FADE_DURATION = 240L

        /*
         * Dots
         */
        private const val DOT_BOTTOM_MARGIN = 40
        private const val DOT_TOP_MARGIN = 20

        private const val DOT_SIZE = 8
        private const val ACTIVE_DOT_WIDTH = 26
        private const val ACTIVE_DOT_HEIGHT = 8
        private const val DOT_GAP = 8

        private const val DOT_GLOW_ALPHA = 0.45f
        private const val DOT_ALPHA = 0.95f

        private const val DOT_INACTIVE_SCALE = 0.92f
        private const val DOT_ACTIVE_START_SCALE = 0.72f
        private const val DOT_ANIMATION_DURATION = 260L

        /*
         * =====================================================
         * PAGE TRANSITION
         * =====================================================
         *
         * عمداً محسوس‌تر از نسخه قبلی.
         */

        private const val MIN_SCALE = 0.84f

        /*
         * Fade بیشتر،
         * اما هنوز صفحه قبلی کاملاً ناگهانی محو نمی‌شود.
         */
        private const val FADE_AMOUNT = 0.55f

        /*
         * حرکت افقی واضح‌تر.
         */
        private const val TRANSLATION_FACTOR = 0.34f

        /*
         * حرکت عمودی بسیار کم برای حس عمق.
         */
        private const val VERTICAL_FACTOR = 0.035f

        /*
         * =====================================================
         * FINAL → WELCOME
         * =====================================================
         */

        private const val FINISH_DURATION = 460L

        private const val FINISH_TRANSLATION = 0.24f

        private const val FINISH_SCALE = 0.94f

        private const val FINISH_ROOT_ALPHA = 0.98f
    }
                        }
