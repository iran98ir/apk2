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
 * صفحه خطا.
 *
 * قانون:
 *  - نوع خطا از intent میاد (کلید items)
 *  - اگه items[type] توی json نبود → items["unknown"]
 *  - اگه items["unknown"] هم نبود → items.values.first()
 *  - پس‌زمینه از error.bg/bgMode/bg2/bgAngle
 *  - متن/رنگ/آیکون از error.items[type]
 *  - دکمه تلاش مجدد از error.retryText / btnBg / btnShape / btnStyle
 */
class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val err = config.error
        val type = intent.getStringExtra(WebActivity.EXTRA_ERROR_TYPE) ?: "unknown"

        // انتخاب آیتم خطا از json
        val item = err.items[type]
            ?: err.items["unknown"]
            ?: err.items.values.first()

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(err.bgMode, err.bg, err.bg2, err.bgAngle)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(
                SvgHelper.dp(this@ErrorActivity, PAD),
                SvgHelper.dp(this@ErrorActivity, PAD),
                SvgHelper.dp(this@ErrorActivity, PAD),
                SvgHelper.dp(this@ErrorActivity, PAD)
            )
        }
        root.addView(col)

        // --- آیکون ---
        if (item.iconSvg.isNotBlank()) {
            val holder = FrameLayout(this)
            SvgHelper.buildIconView(
                this, holder,
                item.iconSvg, item.iconSize, item.iconBoxSize,
                item.iconBgOn, item.iconBg, item.iconShape,
                item.iconColorOn, item.iconColor
            )
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = SvgHelper.dp(this@ErrorActivity, 16)
            }
            holder.layoutParams = lp
            col.addView(holder)
        }

        // --- عنوان ---
        if (item.t.isNotBlank()) {
            val tv = TextView(this).apply {
                text = item.t
                textSize = err.tSize.toFloat()
                setTextColor(SvgHelper.parseColor(err.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = SvgHelper.dp(this@ErrorActivity, 8)
            }
            tv.layoutParams = lp
            col.addView(tv)
        }

        // --- توضیح ---
        if (item.d.isNotBlank()) {
            val tv = TextView(this).apply {
                text = item.d
                textSize = err.dSize.toFloat()
                setTextColor(SvgHelper.parseColor(err.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = SvgHelper.dp(this@ErrorActivity, 24)
            }
            tv.layoutParams = lp
            col.addView(tv)
        }

        // --- دکمه تلاش مجدد ---
        if (err.retryText.isNotBlank()) {
            val btn = Button(this).apply {
                text = err.retryText
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                typeface = loadFont()
                isAllCaps = false
                background = buildButtonBackground(err)
                setPadding(
                    SvgHelper.dp(this@ErrorActivity, 32),
                    SvgHelper.dp(this@ErrorActivity, 12),
                    SvgHelper.dp(this@ErrorActivity, 32),
                    SvgHelper.dp(this@ErrorActivity, 12)
                )
                setOnClickListener {
                    startActivity(Intent(this@ErrorActivity, WebActivity::class.java))
                    finish()
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            btn.layoutParams = lp
            col.addView(btn)
        }

        setContentView(root)
        ThemeHelper.applySystemBars(this, config)
    }

    /**
     * پس‌زمینه دکمه بر اساس error.btnShape / btnStyle / btnBg
     */
    private fun buildButtonBackground(err: ErrorConfig): GradientDrawable {
        val bg = GradientDrawable()
        val bgColor = SvgHelper.parseColor(err.btnBg)

        when (err.btnStyle) {
            "outline" -> {
                bg.setColor(0x00000000)
                bg.setStroke(SvgHelper.dp(this, 2), bgColor)
            }
            else -> {
                bg.setColor(bgColor)
            }
        }

        when (err.btnShape) {
            "pill"    -> bg.cornerRadius = 999f
            "square"  -> bg.cornerRadius = 0f
            "rounded" -> bg.cornerRadius = SvgHelper.dp(this, 8).toFloat()
        }

        return bg
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
