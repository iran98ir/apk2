package ir.apkbuilder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
 * WelcomeActivity
 *
 * Native Premium Welcome Renderer
 *
 * قانون اصلی:
 *
 * هیچ محتوای اختصاصی یک اپ در این فایل وجود ندارد.
 *
 * تمام اطلاعات Welcome از:
 *
 *     ConfigLoader
 *          ↓
 *     AppConfig
 *          ↓
 *     config.welcome
 *          ↓
 *     app.json
 *
 * خوانده می‌شود.
 *
 * این فایل فقط:
 * - Renderer
 * - Layout Engine
 * - Animation Engine
 *
 * است.
 * =========================================================
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout

    private var isLeaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * =====================================================
         * LOAD CONFIG
         * =====================================================
         */

        config = ConfigLoader.load(this)

        val welcome = config.welcome

        /*
         * =====================================================
         * ROOT
         * =====================================================
         */

        root = FrameLayout(this).apply {

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            /*
             * کاملاً از app.json
             */
            background = SvgHelper.makeBackground(
                welcome.bgMode,
                welcome.bg,
                welcome.bg2,
                welcome.bgAngle
            )

            /*
             * برای ورود نرم صفحه
             */
            alpha = 0f

            clipChildren = false
            clipToPadding = false
        }

        /*
         * =====================================================
         * MAIN CONTENT
         * =====================================================
         */

        content = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            gravity = Gravity.CENTER

            setPadding(
                SvgHelper.dp(
                    this@WelcomeActivity,
                    CONTENT_PADDING
                ),
                SvgHelper.dp(
                    this@WelcomeActivity,
                    CONTENT_PADDING
                ),
                SvgHelper.dp(
                    this@WelcomeActivity,
                    CONTENT_PADDING
                ),
                SvgHelper.dp(
                    this@WelcomeActivity,
                    CONTENT_PADDING
                )
            )

            clipChildren = false
            clipToPadding = false
        }

        /*
         * =====================================================
         * THREE REAL AREAS
         *
         * TOP
         * CENTER
         * BOTTOM
         *
         * موقعیت عناصر از JSON تعیین می‌شود.
         * =====================================================
         */

        val topBox = makeBox()
        val centerBox = makeBox()
        val bottomBox = makeBox()

        content.addView(topBox)
        content.addView(centerBox)
        content.addView(bottomBox)

        root.addView(content)

        /*
         * =====================================================
         * ICON
         * =====================================================
         */

        var iconView: View? = null

        if (
            welcome.hasIcon &&
            welcome.iconPos != "hidden" &&
            welcome.iconSvg.isNotBlank()
        ) {

            val iconHolder = FrameLayout(this).apply {

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {

                        gravity = Gravity.CENTER
                    }

                /*
                 * شروع Animation
                 */
                alpha = 0f

                scaleX = ICON_START_SCALE
                scaleY = ICON_START_SCALE

                translationY =
                    SvgHelper.dp(
                        this@WelcomeActivity,
                        ICON_START_Y
                    ).toFloat()
            }

            /*
             * تمام تنظیمات آیکون از JSON
             */
            SvgHelper.buildIconView(
                this,
                iconHolder,
                welcome.iconSvg,
                welcome.iconSize,
                welcome.iconBoxSize,
                welcome.iconBgOn,
                welcome.iconBg,
                welcome.iconShape,
                welcome.iconColorOn,
                welcome.iconColor
            )

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                welcome.iconPos
            ).addView(iconHolder)

            iconView = iconHolder
        }

        /*
         * =====================================================
         * TEXT
         * =====================================================
         */

        var textView: View? = null

        if (
            welcome.textPos != "hidden" &&
            (
                welcome.t.isNotBlank() ||
                    welcome.d.isNotBlank()
                )
        ) {

            val textBox =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                }

            /*
             * TITLE
             */
            if (welcome.t.isNotBlank()) {

                val title =
                    TextView(this).apply {

                        text = welcome.t

                        textSize =
                            welcome.tSize.toFloat()

                        setTextColor(
                            SvgHelper.parseColor(
                                welcome.txt
                            )
                        )

                        gravity = Gravity.CENTER

                        typeface = loadFont()

                        includeFontPadding = true

                        setLineSpacing(
                            0f,
                            TITLE_LINE_SPACING
                        )

                        alpha = 0f

                        translationY =
                            SvgHelper.dp(
                                this@WelcomeActivity,
                                TEXT_START_Y
                            ).toFloat()

                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                    }

                textBox.addView(title)

                textView = title
            }

            /*
             * DESCRIPTION
             */
            if (welcome.d.isNotBlank()) {

                val description =
                    TextView(this).apply {

                        text = welcome.d

                        textSize =
                            welcome.dSize.toFloat()

                        setTextColor(
                            SvgHelper.parseColor(
                                welcome.txt
                            )
                        )

                        gravity = Gravity.CENTER

                        typeface = loadFont()

                        includeFontPadding = true

                        setPadding(
                            0,
                            SvgHelper.dp(
                                this@WelcomeActivity,
                                DESCRIPTION_TOP_PADDING
                            ),
                            0,
                            0
                        )

                        setLineSpacing(
                            0f,
                            DESCRIPTION_LINE_SPACING
                        )

                        alpha = 0f

                        translationY =
                            SvgHelper.dp(
                                this@WelcomeActivity,
                                TEXT_START_Y
                            ).toFloat()

                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                    }

                textBox.addView(description)

                if (textView == null) {
                    textView = description
                }
            }

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                welcome.textPos
            ).addView(textBox)
        }

        /*
         * =====================================================
         * BUTTON
         * =====================================================
         */

        var buttonView: TextView? = null

        if (
            welcome.btn.isNotBlank() &&
            welcome.btnPos != "hidden"
        ) {

            buttonView =
                buildButton(welcome).apply {

                    /*
                     * Animation
                     */
                    alpha = 0f

                    scaleX = BUTTON_START_SCALE
                    scaleY = BUTTON_START_SCALE

                    translationY =
                        SvgHelper.dp(
                            this@WelcomeActivity,
                            BUTTON_START_Y
                        ).toFloat()
                }

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                welcome.btnPos
            ).addView(buttonView)
        }

        /*
         * =====================================================
         * SET CONTENT
         * =====================================================
         */

        setContentView(root)

        ThemeHelper.applySystemBars(
            this,
            config
        )

        /*
         * =====================================================
         * START ENTRANCE
         *
         * بعد از setContentView
         * تا هیچ Animation روی View جداشده اجرا نشود.
         * =====================================================
         */

        root.post {

            playRootEntrance()

            iconView?.let {
                animateIconIn(it)
            }

            textView?.let {
                animateTextIn(it)
            }

            buttonView?.let {
                animateButtonIn(it)
            }
        }
    }

    /*
     * =========================================================
     * BOX
     * =========================================================
     */

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

    /*
     * =========================================================
     * POSITION
     * =========================================================
     */

    private fun pickBox(
        top: LinearLayout,
        center: LinearLayout,
        bottom: LinearLayout,
        position: String
    ): LinearLayout {

        return when (position.lowercase()) {

            "top" ->
                top

            "bottom" ->
                bottom

            "center" ->
                center

            /*
             * اگر مقدار ناشناخته بود،
             * رفتار امن:
             * مرکز
             */
            else ->
                center
        }
    }

    /*
     * =========================================================
     * BUTTON
     * =========================================================
     */

    private fun buildButton(
        welcome: AppConfig.WelcomeConfig
    ): TextView {

        return TextView(this).apply {

            /*
             * متن از JSON
             */
            text =
                welcome.btn

            /*
             * اندازه از JSON
             */
            textSize =
                welcome.btnSize.toFloat()

            /*
             * رنگ متن از JSON
             */
            setTextColor(
                SvgHelper.parseColor(
                    welcome.btnTxt
                )
            )

            gravity =
                Gravity.CENTER

            typeface =
                loadFont()

            includeFontPadding =
                true

            /*
             * Padding داخلی
             *
             * این مقدار Layout Engine است،
             * نه مقدار اختصاصی اپ.
             */
            setPadding(
                SvgHelper.dp(
                    this@WelcomeActivity,
                    BUTTON_HORIZONTAL_PADDING
                ),
                SvgHelper.dp(
                    this@WelcomeActivity,
                    BUTTON_VERTICAL_PADDING
                ),
                SvgHelper.dp(
                    this@WelcomeActivity,
                    BUTTON_HORIZONTAL_PADDING
                ),
                SvgHelper.dp(
                    this@WelcomeActivity,
                    BUTTON_VERTICAL_PADDING
                )
            )

            /*
             * اندازه طبیعی دکمه
             */
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    gravity =
                        Gravity.CENTER
                }

            /*
             * Background کاملاً از JSON
             */
            background =
                makeButtonBackground(
                    welcome
                )

            /*
             * Click
             */
            isClickable = true
            isFocusable = true

            setOnClickListener {
                leaveWelcome()
            }
        }
    }

    /*
     * =========================================================
     * BUTTON BACKGROUND
     * =========================================================
     */

    private fun makeButtonBackground(
        welcome: AppConfig.WelcomeConfig
    ): GradientDrawable {

        return GradientDrawable().apply {

            /*
             * رنگ دکمه از JSON
             */
            setColor(
                SvgHelper.parseColor(
                    welcome.btnBg
                )
            )

            /*
             * Shape از JSON
             */
            cornerRadius =
                resolveButtonRadius(
                    welcome
                )

            /*
             * Style از JSON
             */
            when (welcome.btnStyle.lowercase()) {

                /*
                 * flat:
                 * بدون سایه
                 */
                "flat" -> {
                    alpha = 255
                }

                /*
                 * elevated:
                 * خود View در ادامه Elevation می‌گیرد.
                 */
                "elevated" -> {
                    alpha = 255
                }

                /*
                 * هر مقدار ناشناخته:
                 * امن و ساده
                 */
                else -> {
                    alpha = 255
                }
            }
        }
    }

    /*
     * =========================================================
     * BUTTON RADIUS
     * =========================================================
     *
     * btnShape از JSON اولویت دارد.
     *
     * pill:
     * دکمه کاملاً گرد
     *
     * round / rounded:
     * از btnRadius استفاده می‌کند.
     *
     * square:
     * گوشه تقریباً صفر
     * =========================================================
     */

    private fun resolveButtonRadius(
        welcome: AppConfig.WelcomeConfig
    ): Float {

        val shape =
            welcome.btnShape.lowercase()

        return when (shape) {

            "pill" -> {

                /*
                 * برای pill شعاع را
                 * بر اساس ارتفاع واقعی دکمه
                 * در onLayout اصلاح می‌کنیم.
                 *
                 * مقدار بالا عمداً زیاد است.
                 */
                SvgHelper.dp(
                    this,
                    PILL_RADIUS
                ).toFloat()
            }

            "round",
            "rounded" -> {

                SvgHelper.dp(
                    this,
                    welcome.btnRadius
                ).toFloat()
            }

            "square" -> {
                0f
            }

            else -> {

                SvgHelper.dp(
                    this,
                    welcome.btnRadius
                ).toFloat()
            }
        }
    }

    /*
     * =========================================================
     * ROOT ENTRANCE
     * =========================================================
     */

    private fun playRootEntrance() {

        root.animate()
            .alpha(1f)
            .setDuration(
                ROOT_ENTER_DURATION
            )
            .setInterpolator(
                DecelerateInterpolator(
                    1.4f
                )
            )
            .start()
    }

    /*
     * =========================================================
     * ICON ANIMATION
     * =========================================================
     */

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
                ICON_START_SCALE,
                1f
            )

        val scaleY =
            ObjectAnimator.ofFloat(
                view,
                View.SCALE_Y,
                ICON_START_SCALE,
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
                ICON_DURATION

            startDelay =
                ICON_DELAY

            interpolator =
                OvershootInterpolator(
                    ICON_OVERSHOOT
                )

            start()
        }
    }

    /*
     * =========================================================
     * TEXT ANIMATION
     * =========================================================
     */

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
                TEXT_DURATION

            startDelay =
                TEXT_DELAY

            interpolator =
                DecelerateInterpolator(
                    1.35f
                )

            start()
        }
    }

    /*
     * =========================================================
     * BUTTON ANIMATION
     * =========================================================
     */

    private fun animateButtonIn(
        view: TextView
    ) {

        /*
         * Elevated فقط وقتی در JSON انتخاب شده،
         * Elevation واقعی می‌گیرد.
         */
        if (
            config.welcome.btnStyle
                .equals(
                    "elevated",
                    ignoreCase = true
                )
        ) {

            view.elevation =
                SvgHelper.dp(
                    this,
                    BUTTON_ELEVATION
                ).toFloat()
        }

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
                BUTTON_START_SCALE,
                1f
            )

        val scaleY =
            ObjectAnimator.ofFloat(
                view,
                View.SCALE_Y,
                BUTTON_START_SCALE,
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
                BUTTON_DURATION

            startDelay =
                BUTTON_DELAY

            interpolator =
                OvershootInterpolator(
                    BUTTON_OVERSHOOT
                )

            start()
        }
    }

    /*
     * =========================================================
     * LEAVE
     * =========================================================
     */

    private fun leaveWelcome() {

        if (isLeaving) {
            return
        }

        isLeaving = true

        /*
         * جلوگیری از کلیک مجدد
         */
        content.isEnabled = false

        /*
         * خروج محسوس اما نرم
         */
        content.animate()
            .translationY(
                -SvgHelper.dp(
                    this,
                    EXIT_TRANSLATION_Y
                ).toFloat()
            )
            .alpha(0f)
            .scaleX(EXIT_SCALE)
            .scaleY(EXIT_SCALE)
            .setDuration(
                EXIT_DURATION
            )
            .setInterpolator(
                DecelerateInterpolator(
                    1.35f
                )
            )
            .withEndAction {

                openWeb()
            }
            .start()
    }

    /*
     * =========================================================
     * OPEN WEB
     * =========================================================
     *
     * Welcome فقط صفحه ورود است.
     *
     * مقصد بعدی:
     * WebActivity
     *
     * خود WebActivity از config.web
     * استفاده می‌کند.
     * =========================================================
     */

    private fun openWeb() {

        startActivity(
            Intent(
                this,
                WebActivity::class.java
            )
        )

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

    /*
     * =========================================================
     * FONT
     * =========================================================
     */

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

    /*
     * =========================================================
     * CONSTANTS
     *
     * این‌ها تنظیمات Engine هستند،
     * نه اطلاعات اپ.
     *
     * بنابراین در app.json قرار نمی‌گیرند.
     * =========================================================
     */

    companion object {

        /*
         * Layout
         */
        private const val CONTENT_PADDING = 24

        /*
         * Text
         */
        private const val DESCRIPTION_TOP_PADDING = 10

        private const val TITLE_LINE_SPACING = 1.08f

        private const val DESCRIPTION_LINE_SPACING = 1.12f

        /*
         * Root entrance
         */
        private const val ROOT_ENTER_DURATION = 280L

        /*
         * Icon
         */
        private const val ICON_START_SCALE = 0.78f

        private const val ICON_START_Y = 34

        private const val ICON_DELAY = 60L

        private const val ICON_DURATION = 650L

        private const val ICON_OVERSHOOT = 0.72f

        /*
         * Text
         */
        private const val TEXT_START_Y = 24

        private const val TEXT_DELAY = 160L

        private const val TEXT_DURATION = 500L

        /*
         * Button
         */
        private const val BUTTON_START_SCALE = 0.82f

        private const val BUTTON_START_Y = 26

        private const val BUTTON_DELAY = 280L

        private const val BUTTON_DURATION = 520L

        private const val BUTTON_OVERSHOOT = 0.7f

        private const val BUTTON_HORIZONTAL_PADDING = 30

        private const val BUTTON_VERTICAL_PADDING = 12

        private const val BUTTON_ELEVATION = 7

        /*
         * فقط برای shape = pill
         */
        private const val PILL_RADIUS = 100

        /*
         * Exit
         */
        private const val EXIT_DURATION = 380L

        private const val EXIT_TRANSLATION_Y = 20

        private const val EXIT_SCALE = 0.965f
    }
}
