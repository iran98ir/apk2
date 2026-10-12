package ir.apkbuilder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button

/**
 * صفحه Welcome
 *
 * تمام اطلاعات ظاهری و محتوایی از welcome در app.json می‌آید.
 *
 * قانون:
 *  - پس‌زمینه از welcome.bgMode / bg / bg2 / bgAngle
 *  - آیکون از welcome.hasIcon / iconSvg / ...
 *  - متن از welcome.t / welcome.d / ...
 *  - دکمه از welcome.btn / btnShape / btnStyle / ...
 *  - محل عناصر از welcome.iconPos / textPos / btnPos
 *  - مقدار "hidden" → عنصر ساخته نمی‌شود
 *
 * این فایل فقط Renderer و رفتار Native است.
 * هیچ محتوای مخصوص یک اپ در این فایل Hardcode نشده است.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout

    private val enterInterpolator = DecelerateInterpolator(1.45f)
    private val buttonInterpolator = OvershootInterpolator(1.15f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val w = config.welcome

        /*
         * Root
         */
        root = FrameLayout(this).apply {
            background = SvgHelper.makeBackground(
                w.bgMode,
                w.bg,
                w.bg2,
                w.bgAngle
            )

            alpha = 0f
        }

        /*
         * محتوای اصلی
         */
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            setPadding(
                SvgHelper.dp(this@WelcomeActivity, PAD),
                SvgHelper.dp(this@WelcomeActivity, PAD),
                SvgHelper.dp(this@WelcomeActivity, PAD),
                SvgHelper.dp(this@WelcomeActivity, PAD)
            )
        }

        /*
         * سه ناحیه اصلی
         *
         * top
         * center
         * bottom
         *
         * این ساختار با سیستم فعلی app.json حفظ شده.
         */
        val boxTop = makeBox()
        val boxCenter = makeBox()
        val boxBottom = makeBox()

        col.addView(boxTop)
        col.addView(boxCenter)
        col.addView(boxBottom)

        /*
         * برای انیمیشن ورود،
         * Viewهایی که واقعاً ساخته می‌شوند را نگه می‌داریم.
         */
        val animatedViews = ArrayList<View>()

        // ============================================================
        // ICON
        // ============================================================

        if (
            w.hasIcon &&
            w.iconPos != "hidden" &&
            w.iconSvg.isNotBlank()
        ) {
            val holder = FrameLayout(this).apply {
                alpha = 0f
                scaleX = 0.88f
                scaleY = 0.88f
                translationY = SvgHelper.dp(
                    this@WelcomeActivity,
                    14
                ).toFloat()

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }

            SvgHelper.buildIconView(
                this,
                holder,
                w.iconSvg,
                w.iconSize,
                w.iconBoxSize,
                w.iconBgOn,
                w.iconBg,
                w.iconShape,
                w.iconColorOn,
                w.iconColor
            )

            pickBox(
                boxTop,
                boxCenter,
                boxBottom,
                w.iconPos
            ).addView(holder)

            animatedViews.add(holder)
        }

        // ============================================================
        // TEXT
        // ============================================================

        if (w.textPos != "hidden") {

            val textBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                alpha = 0f
                translationY = SvgHelper.dp(
                    this@WelcomeActivity,
                    18
                ).toFloat()

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }

            /*
             * عنوان
             */
            val tvTitle = TextView(this).apply {
                text = w.t
                textSize = w.tSize.toFloat()
                setTextColor(SvgHelper.parseColor(w.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()

                includeFontPadding = true

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                setLineSpacing(
                    0f,
                    1.05f
                )
            }

            textBox.addView(tvTitle)

            /*
             * توضیح
             */
            val tvDesc = TextView(this).apply {
                text = w.d
                textSize = w.dSize.toFloat()
                setTextColor(SvgHelper.parseColor(w.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()

                includeFontPadding = true

                setPadding(
                    0,
                    SvgHelper.dp(this@WelcomeActivity, 8),
                    0,
                    0
                )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                setLineSpacing(
                    0f,
                    1.12f
                )
            }

            textBox.addView(tvDesc)

            pickBox(
                boxTop,
                boxCenter,
                boxBottom,
                w.textPos
            ).addView(textBox)

            animatedViews.add(textBox)
        }

        // ============================================================
        // BUTTON
        // ============================================================

        if (w.btnPos != "hidden") {

            val buttonHolder = FrameLayout(this).apply {
                alpha = 0f
                translationY = SvgHelper.dp(
                    this@WelcomeActivity,
                    20
                ).toFloat()

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER

                    topMargin = SvgHelper.dp(
                        this@WelcomeActivity,
                        8
                    )
                }
            }

            val btn = Button(this).apply {

                text = w.btn

                textSize = w.btnSize.toFloat()

                setTextColor(
                    SvgHelper.parseColor(w.btnTxt)
                )

                typeface = loadFont()

                isAllCaps = false

                /*
                 * حذف padding و background پیش‌فرض Button
                 * تا ظاهر Native قدیمی اندروید وارد UI نشود.
                 */
                stateListAnimator = null

                background = buildButtonBackground(w)

                setPadding(
                    SvgHelper.dp(this@WelcomeActivity, 32),
                    SvgHelper.dp(this@WelcomeActivity, 14),
                    SvgHelper.dp(this@WelcomeActivity, 32),
                    SvgHelper.dp(this@WelcomeActivity, 14)
                )

                minHeight = 0
                minimumHeight = 0

                includeFontPadding = true

                gravity = Gravity.CENTER

                /*
                 * اندازه محتوا بر اساس متن و تنظیمات فعلی.
                 */
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )

                /*
                 * Ripple / Press feedback
                 */
                ViewCompat.setElevation(
                    this,
                    SvgHelper.dp(
                        this@WelcomeActivity,
                        3
                    ).toFloat()
                )

                /*
                 * لمس حرفه‌ای:
                 * هنگام لمس کمی کوچک می‌شود و بعد برمی‌گردد.
                 */
                setOnTouchListener { view, event ->

                    when (event.actionMasked) {

                        android.view.MotionEvent.ACTION_DOWN -> {
                            view.animate()
                                .scaleX(0.965f)
                                .scaleY(0.965f)
                                .setDuration(90L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }

                        android.view.MotionEvent.ACTION_UP -> {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(180L)
                                .setInterpolator(buttonInterpolator)
                                .start()
                        }

                        android.view.MotionEvent.ACTION_CANCEL -> {
                            view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(140L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                    }

                    false
                }

                setOnClickListener {
                    openWebActivity()
                }
            }

            buttonHolder.addView(btn)

            pickBox(
                boxTop,
                boxCenter,
                boxBottom,
                w.btnPos
            ).addView(buttonHolder)

            animatedViews.add(buttonHolder)
        }

        root.addView(col)

        setContentView(root)

        /*
         * جلوگیری از چسبیدن UI به system bars
         * بدون تغییر ساختار app.json.
         */
        applySafeInsets(col)

        ThemeHelper.applySystemBars(
            this,
            config
        )

        /*
         * شروع انیمیشن ورود
         */
        root.post {
            playEntranceAnimation(animatedViews)
        }
    }

    // ================================================================
    // ENTRANCE ANIMATION
    // ================================================================

    private fun playEntranceAnimation(
        views: List<View>
    ) {
        if (views.isEmpty()) {
            root.animate()
                .alpha(1f)
                .setDuration(260L)
                .setInterpolator(enterInterpolator)
                .start()

            return
        }

        /*
         * اول خود صفحه آرام ظاهر می‌شود.
         */
        root.animate()
            .alpha(1f)
            .setDuration(260L)
            .setInterpolator(enterInterpolator)
            .start()

        /*
         * هر عنصر با فاصله بسیار کم وارد می‌شود.
         *
         * این ترتیب وابسته به app.json نیست.
         * فقط ترتیب طبیعی Render شدن عناصر است.
         */
        views.forEachIndexed { index, view ->

            val delay = when (index) {
                0 -> 60L
                1 -> 135L
                2 -> 215L
                else -> 285L
            }

            val fade = ObjectAnimator.ofFloat(
                view,
                View.ALPHA,
                0f,
                1f
            )

            val translate = ObjectAnimator.ofFloat(
                view,
                View.TRANSLATION_Y,
                view.translationY,
                0f
            )

            val scaleX = ObjectAnimator.ofFloat(
                view,
                View.SCALE_X,
                view.scaleX,
                1f
            )

            val scaleY = ObjectAnimator.ofFloat(
                view,
                View.SCALE_Y,
                view.scaleY,
                1f
            )

            AnimatorSet().apply {
                playTogether(
                    fade,
                    translate,
                    scaleX,
                    scaleY
                )

                duration = when (index) {
                    0 -> 430L
                    1 -> 390L
                    else -> 360L
                }

                startDelay = delay

                interpolator = when (index) {
                    0 -> OvershootInterpolator(0.65f)
                    else -> enterInterpolator
                }

                start()
            }
        }
    }

    // ================================================================
    // BUTTON BACKGROUND
    // ================================================================

    /**
     * ساخت Background دکمه بر اساس:
     *
     * btnShape
     * btnStyle
     * btnBg
     * btnRadius
     *
     * تمام مقادیر از app.json می‌آیند.
     */
    private fun buildButtonBackground(
        w: WelcomeConfig
    ): StateListDrawable {

        val normal = GradientDrawable()

        val bgColor = SvgHelper.parseColor(
            w.btnBg
        )

        when (w.btnStyle) {

            "outline" -> {
                normal.setColor(
                    0x00000000
                )

                normal.setStroke(
                    SvgHelper.dp(this, 2),
                    bgColor
                )
            }

            else -> {
                normal.setColor(
                    bgColor
                )
            }
        }

        when (w.btnShape) {

            "pill" -> {
                normal.cornerRadius = 999f
            }

            "square" -> {
                normal.cornerRadius = 0f
            }

            "rounded" -> {
                normal.cornerRadius =
                    SvgHelper.dp(
                        this,
                        w.btnRadius
                    ).toFloat()
            }

            else -> {
                normal.cornerRadius =
                    SvgHelper.dp(
                        this,
                        w.btnRadius
                    ).toFloat()
            }
        }

        /*
         * حالت pressed
         *
         * برای outline:
         * همان ساختار با کمی تغییر در opacity.
         *
         * برای filled:
         * رنگ همان رنگ اصلی باقی می‌ماند ولی
         * Ripple توسط سیستم روی آن اعمال می‌شود.
         */
        val pressed = GradientDrawable()

        when (w.btnStyle) {

            "outline" -> {
                pressed.setColor(
                    0x12000000
                )

                pressed.setStroke(
                    SvgHelper.dp(this, 2),
                    bgColor
                )
            }

            else -> {
                pressed.setColor(
                    bgColor
                )
            }
        }

        when (w.btnShape) {

            "pill" -> {
                pressed.cornerRadius = 999f
            }

            "square" -> {
                pressed.cornerRadius = 0f
            }

            "rounded" -> {
                pressed.cornerRadius =
                    SvgHelper.dp(
                        this,
                        w.btnRadius
                    ).toFloat()
            }

            else -> {
                pressed.cornerRadius =
                    SvgHelper.dp(
                        this,
                        w.btnRadius
                    ).toFloat()
            }
        }

        return StateListDrawable().apply {

            addState(
                intArrayOf(
                    android.R.attr.state_pressed
                ),
                pressed
            )

            addState(
                intArrayOf(),
                normal
            )
        }
    }

    // ================================================================
    // BOX
    // ================================================================

    private fun makeBox(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            clipChildren = false
            clipToPadding = false
        }
    }

    // ================================================================
    // POSITION
    // ================================================================

    private fun pickBox(
        top: LinearLayout,
        center: LinearLayout,
        bottom: LinearLayout,
        pos: String
    ): LinearLayout {

        return when (pos) {

            "top" -> top

            "center" -> center

            "bottom" -> bottom

            /*
             * رفتار قبلی حفظ شده:
             * اگر مقدار ناشناخته بود،
             * مرکز انتخاب می‌شود.
             */
            else -> center
        }
    }

    // ================================================================
    // SAFE INSETS
    // ================================================================

    private fun applySafeInsets(
        content: View
    ) {

        ViewCompat.setOnApplyWindowInsetsListener(
            content
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            /*
             * فقط به padding موجود اضافه می‌کنیم.
             * بنابراین مقدار اصلی PAD از بین نمی‌رود.
             */
            view.setPadding(
                SvgHelper.dp(
                    this,
                    PAD
                ),
                SvgHelper.dp(
                    this,
                    PAD
                ) + systemBars.top,
                SvgHelper.dp(
                    this,
                    PAD
                ),
                SvgHelper.dp(
                    this,
                    PAD
                ) + systemBars.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(
            content
        )
    }

    // ================================================================
    // FONT
    // ================================================================

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

    // ================================================================
    // OPEN WEB
    // ================================================================

    private fun openWebActivity() {

        /*
         * رفتار فعلی برنامه حفظ شده:
         * Welcome → WebActivity
         */
        startActivity(
            Intent(
                this,
                WebActivity::class.java
            )
        )

        /*
         * جلوگیری از transition قدیمی اندروید
         * تا ورود به WebActivity حس یک صفحه جدید و
         * ناگهانی نداشته باشد.
         */
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        finish()
    }

    // ================================================================
    // CONSTANTS
    // ================================================================

    companion object {

        /*
         * این مقدار مربوط به Renderer است،
         * نه محتوای app.json.
         */
        private const val PAD = 24
    }
}
