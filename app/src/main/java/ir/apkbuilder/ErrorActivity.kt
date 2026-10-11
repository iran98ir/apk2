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

/**
 * صفحه خطا.
 * نوع خطا از intent میاد: offline, timeout, dns, ssl, e404, e500, down, unknown
 * اطلاعات نمایش از error.items[type] خونده میشه.
 */
class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val err = config.error
        val type = intent.getStringExtra("error_type") ?: "unknown"

        val item = err.items[type]
            ?: err.items["unknown"]
            ?: err.items.values.firstOrNull()
            ?: error("error.items is empty in config.json")

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(err.bgMode, err.bg, err.bg2, err.bgAngle)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        col.setPadding(
            SvgHelper.dp(this, 24),
            SvgHelper.dp(this, 24),
            SvgHelper.dp(this, 24),
            SvgHelper.dp(this, 24)
        )
        root.addView(col)

        // آیکون
        if (item.iconSvg != null && item.iconSvg.isNotBlank()) {
            val iconHolder = FrameLayout(this)
            val holderSize = SvgHelper.dp(this, item.iconBoxSize)
            val lp = LinearLayout.LayoutParams(holderSize, holderSize)
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = SvgHelper.dp(this, 16)
            iconHolder.layoutParams = lp
            SvgHelper.buildIconView(
                this,
                iconHolder,
                item.iconSvg,
                item.iconSize,
                item.iconBoxSize,
                item.iconBgOn,
                item.iconBg,
                item.iconShape,
                item.iconColorOn,
                item.iconColor
            )
            col.addView(iconHolder)
        }

        // عنوان
        if (item.t.isNotBlank()) {
            val tv = TextView(this)
            tv.text = item.t
            tv.textSize = err.tSize.toFloat()
            tv.setTextColor(SvgHelper.parseColor(err.txt))
            tv.gravity = Gravity.CENTER
            tv.typeface = loadFont()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = SvgHelper.dp(this, 8)
            tv.layoutParams = lp
            col.addView(tv)
        }

        // توضیح
        if (item.d.isNotBlank()) {
            val tv = TextView(this)
            tv.text = item.d
            tv.textSize = err.dSize.toFloat()
            tv.setTextColor(SvgHelper.parseColor(err.txt))
            tv.gravity = Gravity.CENTER
            tv.typeface = loadFont()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = SvgHelper.dp(this, 24)
            tv.layoutParams = lp
            col.addView(tv)
        }

        // دکمه تلاش مجدد
        if (err.retryText.isNotBlank()) {
            val btn = Button(this)
            btn.text = err.retryText
            btn.setTextColor(SvgHelper.parseColor("#FFFFFF"))
            btn.typeface = loadFont()
            btn.isAllCaps = false

            val bg = GradientDrawable()
            bg.setColor(SvgHelper.parseColor(err.btnBg))
            when (err.btnShape.lowercase()) {
                "circle" -> bg.cornerRadius = 999f
                "rounded" -> bg.cornerRadius = SvgHelper.dp(this, 8).toFloat()
                "square" -> bg.cornerRadius = 0f
                else -> bg.cornerRadius = SvgHelper.dp(this, 8).toFloat()
            }
            btn.background = bg

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_HORIZONTAL
            btn.layoutParams = lp
            btn.setPadding(
                SvgHelper.dp(this, 32),
                SvgHelper.dp(this, 12),
                SvgHelper.dp(this, 32),
                SvgHelper.dp(this, 12)
            )

            btn.setOnClickListener {
                startActivity(Intent(this, WebActivity::class.java))
                finish()
            }
            col.addView(btn)
        }

        setContentView(root)
        ThemeHelper.applySystemBars(this, config)
    }

    private fun loadFont(): Typeface? {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(this, R.font.lalezar)
        } catch (e: Exception) {
            null
        }
    }
}
