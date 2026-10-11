package ir.apkbuilder

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

/**
 * صفحه خوش‌آمد.
 *
 * قانون:
 *  - پس‌زمینه از welcome.bgMode/bg/bg2/bgAngle
 *  - آیکون از welcome.hasIcon/iconSvg/...
 *  - متن (عنوان + زیرمتن) از welcome.t / welcome.d
 *  - دکمه از welcome.btn / btnShape / btnStyle / btnBg / btnTxt / btnSize / btnRadius
 *  - محل هر عنصر از welcome.iconPos / textPos / btnPos
 *  - مقدار "hidden" → اون عنصر ساخته نمیشه
 */
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val w = config.welcome

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(w.bgMode, w.bg, w.bg2, w.bgAngle)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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

        val boxTop    = makeBox()
        val boxCenter = makeBox()
        val boxBottom = makeBox()

        col.addView(boxTop)
        col.addView(boxCenter)
        col.addView(boxBottom)

        // --- آیکون ---
        if (w.hasIcon && w.iconPos != "hidden" && w.iconSvg.isNotBlank()) {
            val holder = FrameLayout(this)
            SvgHelper.buildIconView(
                this, holder,
                w.iconSvg, w.iconSize, w.iconBoxSize,
                w.iconBgOn, w.iconBg, w.iconShape,
                w.iconColorOn, w.iconColor
            )
            pickBox(boxTop, boxCenter, boxBottom, w.iconPos).addView(holder)
        }

        // --- متن (عنوان + زیرمتن) ---
        if (w.textPos != "hidden") {
            val textBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val tvTitle = TextView(this).apply {
                text = w.t
                textSize = w.tSize.toFloat()
                setTextColor(SvgHelper.parseColor(w.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()
            }
            textBox.addView(tvTitle)

            val tvDesc = TextView(this).apply {
                text = w.d
                textSize = w.dSize.toFloat()
                setTextColor(SvgHelper.parseColor(w.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()
                setPadding(0, SvgHelper.dp(this@WelcomeActivity, 8), 0, 0)
            }
            textBox.addView(tvDesc)

            pickBox(boxTop, boxCenter, boxBottom, w.textPos).addView(textBox)
        }

        // --- دکمه ---
        if (w.btnPos != "hidden") {
            val btn = Button(this).apply {
                text = w.btn
                textSize = w.btnSize.toFloat()
                setTextColor(SvgHelper.parseColor(w.btnTxt))
                typeface = loadFont()
                isAllCaps = false
                background = buildButtonBackground(w)
                setPadding(
                    SvgHelper.dp(this@WelcomeActivity, 32),
                    SvgHelper.dp(this@WelcomeActivity, 12),
                    SvgHelper.dp(this@WelcomeActivity, 32),
                    SvgHelper.dp(this@WelcomeActivity, 12)
                )
                setOnClickListener {
                    startActivity(Intent(this@WelcomeActivity, WebActivity::class.java))
                    finish()
                }
            }
            pickBox(boxTop, boxCenter, boxBottom, w.btnPos).addView(btn)
        }

        root.addView(col)
        setContentView(root)

        ThemeHelper.applySystemBars(this, config)
    }

    /**
     * ساخت پس‌زمینه دکمه بر اساس btnShape / btnStyle / btnBg / btnRadius
     */
    private fun buildButtonBackground(w: WelcomeConfig): GradientDrawable {
        val bg = GradientDrawable()
        val bgColor = SvgHelper.parseColor(w.btnBg)

        when (w.btnStyle) {
            "outline" -> {
                bg.setColor(0x00000000) // شفاف
                bg.setStroke(SvgHelper.dp(this, 2), bgColor)
            }
            else -> {
                bg.setColor(bgColor)
            }
        }

        when (w.btnShape) {
            "pill"   -> bg.cornerRadius = 999f
            "square" -> bg.cornerRadius = 0f
            "rounded" -> bg.cornerRadius = SvgHelper.dp(this, w.btnRadius).toFloat()
        }

        return bg
    }

    private fun makeBox(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
    }

    private fun pickBox(
        top: LinearLayout,
        center: LinearLayout,
        bottom: LinearLayout,
        pos: String
    ): LinearLayout = when (pos) {
        "top"    -> top
        "center" -> center
        "bottom" -> bottom
        else     -> center
    }

    private fun loadFont(): Typeface? = try {
        ResourcesCompat.getFont(this, R.font.lalezar)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val PAD = 24
    }
}
