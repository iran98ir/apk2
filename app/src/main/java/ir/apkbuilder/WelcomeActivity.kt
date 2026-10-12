package ir.apkbuilder

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

/**
 * =========================================================
 * WelcomeActivity
 *
 * تمام مقادیر محتوایی و ظاهری از config.json خوانده می‌شوند.
 *
 * منبع:
 * config.json
 *     ↓
 * ConfigLoader
 *     ↓
 * AppConfig
 *     ↓
 * WelcomeConfig
 *     ↓
 * این Activity
 *
 * این فایل مخصوص هیچ اپ خاصی نیست.
 * =========================================================
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var welcome: WelcomeConfig

    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout

    private var iconView: View? = null
    private var titleView: TextView? = null
    private var descriptionView: TextView? = null
    private var buttonView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * =====================================================
         * LOAD CONFIG
         * =====================================================
         */

        config = ConfigLoader.load(this)
        welcome = config.welcome

        /*
         * =====================================================
         * ROOT
         * =====================================================
         */

        root = FrameLayout(this).apply {

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            background = SvgHelper.makeBackground(
                welcome.bgMode,
                welcome.bg,
                welcome.bg2,
                welcome.bgAngle
            )

            alpha = 0f

            clipChildren = false
            clipToPadding = false
        }

        /*
         * =====================================================
         * MAIN COLUMN
         * =====================================================
         */

        content = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            setPadding(
                SvgHelper.dp(this@WelcomeActivity, PAGE_PADDING),
                SvgHelper.dp(this@WelcomeActivity, PAGE_PADDING),
                SvgHelper.dp(this@WelcomeActivity, PAGE_PADDING),
                SvgHelper.dp(this@WelcomeActivity, PAGE_PADDING)
            )

            clipChildren = false
            clipToPadding = false
        }

        root.addView(content)

        /*
         * =====================================================
         * THREE FLEXIBLE AREAS
         *
         * iconPos / textPos / btnPos
         * تعیین می‌کنند هر بخش کجا قرار بگیرد.
         * =====================================================
         */

        val topBox = makeBox("top")
        val centerBox = makeBox("center")
        val bottomBox = makeBox("bottom")

        content.addView(topBox)
        content.addView(centerBox)
        content.addView(bottomBox)

        /*
         * =====================================================
         * ICON
         * =====================================================
         */

        if (
            welcome.hasIcon &&
            welcome.iconPos != "hidden" &&
            welcome.iconSvg.isNotBlank()
        ) {

            val iconHolder = FrameLayout(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                clipChildren = false
                clipToPadding = false
            }

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

            iconView = iconHolder

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                welcome.iconPos
            ).addView(iconHolder)
        }

        /*
         * =====================================================
         * TEXT
         * =====================================================
         */

        if (
            welcome.textPos != "hidden"
        ) {

            val textBox = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                clipChildren = false
                clipToPadding = false
            }

            /*
             * TITLE
             */

            val title = TextView(this).apply {

                text = welcome.t

                textSize = welcome.tSize.toFloat()

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

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            titleView = title

            textBox.addView(title)

            /*
             * DESCRIPTION
             */

            val description = TextView(this).apply {

                text = welcome.d

                textSize = welcome.dSize.toFloat()

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

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            descriptionView = description

            textBox.addView(description)

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

        if (
            welcome.btn.isNotBlank() &&
            welcome.btnPos != "hidden"
        ) {

            val button = buildButton(
                welcome
            )

            buttonView = button

            pickBox(
                topBox,
                centerBox,
                bottomBox,
                welcome.btnPos
            ).addView(button)
        }

        /*
         * =====================================================
         * CONTENT
         * =====================================================
         */

        setContentView(root)

        ThemeHelper.applySystemBars(
            this,
            config
        )

        /*
         * =====================================================
         * ENTRANCE ANIMATION
         * =====================================================
         */

        root.post {
            animateEntrance()
        }
    }

    /* =========================================================
       BOX
       ========================================================= */

    private fun makeBox(
        tagValue: String
    ): LinearLayout {

        return LinearLayout(this).apply {

            tag = tagValue

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                BOX_WEIGHT
            )

            clipChildren = false
            clipToPadding = false
        }
    }

    /* =========================================================
       PICK BOX
       ========================================================= */

    private fun pickBox(
        top: LinearLayout,
        center: LinearLayout,
        bottom: LinearLayout,
        position: String
    ): LinearLayout {

        return when (position.lowercase()) {

            "top" -> top

            "bottom" -> bottom

            "center" -> center

            else -> center
        }
    }

    /* =========================================================
       BUTTON
       ========================================================= */

    private fun buildButton(
        cfg: WelcomeConfig
    ): TextView {

        val button = TextView(this).apply {

            text = cfg.btn

            textSize = cfg.btnSize.toFloat()

            setTextColor(
                SvgHelper.parseColor(
                    cfg.btnTxt
                )
            )

            gravity = Gravity.CENTER

            typeface = loadFont()

            includeFontPadding = true

            minHeight = SvgHelper.dp(
                this@WelcomeActivity,
                BUTTON_MIN_HEIGHT
            )

            minWidth = SvgHelper.dp(
                this@WelcomeActivity,
                BUTTON_MIN_WIDTH
            )

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

            background = makeButtonBackground(
                cfg
            )

            /*
             * Elevation فقط رفتار عمومی style است.
             * مقدار محتوایی هاردکد نشده.
             */
            if (
                cfg.btnStyle.equals(
                    "elevated",
                    ignoreCase = true
                )
            ) {
                elevation = SvgHelper.dp(
                    this@WelcomeActivity,
                    BUTTON_ELEVATION
                ).toFloat()
            } else {
                elevation = 0f
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                leaveWelcome()
            }
        }

        return button
    }

    /* =========================================================
       BUTTON BACKGROUND
       ========================================================= */

    private fun makeButtonBackground(
        cfg: WelcomeConfig
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                SvgHelper.parseColor(
                    cfg.btnBg
                )
            )

            /*
             * Shape از config.json
             */

            cornerRadius =
                when (cfg.btnShape.lowercase()) {

                    "pill" -> {
                        /*
                         * برای pill مقدار ثابتِ ظاهری موتور
                         * است، نه مقدار محتوایی اپ.
                         */
                        SvgHelper.dp(
                            this@WelcomeActivity,
                            PILL_RADIUS
                        ).toFloat()
                    }

                    "round",
                    "rounded" -> {

                        SvgHelper.dp(
                            this@WelcomeActivity,
                            cfg.btnRadius
                        ).toFloat()
                    }

                    "square" -> {
                        0f
                    }

                    else -> {

                        SvgHelper.dp(
                            this@WelcomeActivity,
                            cfg.btnRadius
                        ).toFloat()
                    }
                }
        }
    }

    /* =========================================================
       ENTRANCE ANIMATION
       ========================================================= */

    private fun animateEntrance() {

        /*
         * ROOT
         */

        root.alpha = 0f

        root.animate()
            .alpha(1f)
            .setDuration(
                ROOT_FADE_DURATION
            )
            .setInterpolator(
                android.view.animation.DecelerateInterpolator(
                    ROOT_DECELERATION
                )
            )
            .start()

        /*
         * ICON
         */

        iconView?.let { view ->

            view.alpha = 0f

            view.scaleX = ICON_START_SCALE
            view.scaleY = ICON_START_SCALE

            view.translationY =
                SvgHelper.dp(
                    this,
                    ICON_START_TRANSLATION
                ).toFloat()

            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(
                    ICON_ANIMATION_DURATION
                )
                .setStartDelay(
                    ICON_START_DELAY
                )
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

        /*
         * TITLE
         */

        titleView?.let { view ->

            view.alpha = 0f

            view.translationY =
                SvgHelper.dp(
                    this,
                    TEXT_START_TRANSLATION
                ).toFloat()

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(
                    TEXT_ANIMATION_DURATION
                )
                .setStartDelay(
                    TEXT_START_DELAY
                )
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

        /*
         * DESCRIPTION
         */

        descriptionView?.let { view ->

            view.alpha = 0f

            view.translationY =
                SvgHelper.dp(
                    this,
                    DESCRIPTION_START_TRANSLATION
                ).toFloat()

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(
                    DESCRIPTION_ANIMATION_DURATION
                )
                .setStartDelay(
                    DESCRIPTION_START_DELAY
                )
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

        /*
         * BUTTON
         */

        buttonView?.let { view ->

            view.alpha = 0f

            view.scaleX = BUTTON_START_SCALE
            view.scaleY = BUTTON_START_SCALE

            view.translationY =
                SvgHelper.dp(
                    this,
                    BUTTON_START_TRANSLATION
                ).toFloat()

            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(
                    BUTTON_ANIMATION_DURATION
                )
                .setStartDelay(
                    BUTTON_START_DELAY
                )
                .setInterpolator(
                    android.view.animation.OvershootInterpolator(
                        BUTTON_OVERSHOOT
                    )
                )
                .start()
        }
    }

    /* =========================================================
       LEAVE WELCOME
       ========================================================= */

    private fun leaveWelcome() {

        /*
         * جلوگیری از چند کلیک.
         */

        buttonView?.isEnabled = false

        /*
         * خروج نرم خود Welcome
         */

        root.animate()
            .alpha(0f)
            .scaleX(EXIT_SCALE)
            .scaleY(EXIT_SCALE)
            .setDuration(
                EXIT_DURATION
            )
            .setInterpolator(
                android.view.animation.PathInterpolator(
                    0.16f,
                    1.0f,
                    0.3f,
                    1.0f
                )
            )
            .withEndAction {
                openWeb()
            }
            .start()
    }

    /* =========================================================
       OPEN WEB
       ========================================================= */

    private fun openWeb() {

        /*
         * مقصد واقعی از config.web.url
         * توسط WebActivity مدیریت می‌شود.
         *
         * هیچ URL اپ خاصی اینجا هاردکد نشده.
         */

        startActivity(
            Intent(
                this,
                WebActivity::class.java
            )
        )

        /*
         * جلوگیری از انیمیشن پیش‌فرض اندروید.
         */

        overridePendingTransition(
            0,
            0
        )

        finish()
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
       =========================================================
       
       این مقادیر فقط رفتار عمومی موتور/UI هستند.
       هیچ‌کدام مقدار اختصاصی config.json نیستند.
       ========================================================= */

    companion object {

        /*
         * صفحه
         */
        private const val PAGE_PADDING = 24

        /*
         * تقسیم سه ناحیه
         */
        private const val BOX_WEIGHT = 1f

        /*
         * متن
         */
        private const val TITLE_LINE_SPACING = 1.05f
        private const val DESCRIPTION_LINE_SPACING = 1.10f

        private const val DESCRIPTION_TOP_PADDING = 8

        /*
         * دکمه
         */
        private const val BUTTON_MIN_HEIGHT = 52
        private const val BUTTON_MIN_WIDTH = 150

        private const val BUTTON_HORIZONTAL_PADDING = 28
        private const val BUTTON_VERTICAL_PADDING = 12

        private const val BUTTON_ELEVATION = 8

        /*
         * فقط fallback عمومی برای شکل pill.
         * رنگ/اندازه/متن/محتوا از config می‌آید.
         */
        private const val PILL_RADIUS = 100

        /*
         * Root entrance
         */
        private const val ROOT_FADE_DURATION = 360L
        private const val ROOT_DECELERATION = 1.4f

        /*
         * Icon
         */
        private const val ICON_START_SCALE = 0.82f
        private const val ICON_START_TRANSLATION = 22
        private const val ICON_ANIMATION_DURATION = 620L
        private const val ICON_START_DELAY = 40L

        /*
         * Title
         */
        private const val TEXT_START_TRANSLATION = 24
        private const val TEXT_ANIMATION_DURATION = 520L
        private const val TEXT_START_DELAY = 140L

        /*
         * Description
         */
        private const val DESCRIPTION_START_TRANSLATION = 20
        private const val DESCRIPTION_ANIMATION_DURATION = 520L
        private const val DESCRIPTION_START_DELAY = 220L

        /*
         * Button
         */
        private const val BUTTON_START_SCALE = 0.88f
        private const val BUTTON_START_TRANSLATION = 24
        private const val BUTTON_ANIMATION_DURATION = 600L
        private const val BUTTON_START_DELAY = 300L
        private const val BUTTON_OVERSHOOT = 1.08f

        /*
         * Exit
         */
        private const val EXIT_SCALE = 0.97f
        private const val EXIT_DURATION = 260L
    }
}
