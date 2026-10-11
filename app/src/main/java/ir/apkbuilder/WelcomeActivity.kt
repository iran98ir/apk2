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
 * Welcome: صفحه خوش‌آمد نهایی.
 * محتوا از json (آیکون، عنوان، توضیح، دکمه).
 * رنگ نوارها = welcome.bg.
 */
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val w = config.welcome

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(w.bgMode, w.bg, w.bg2, w.bgAngle)

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
        if (w.hasIcon && !w.iconSvg.isNullOrBlank()) {
            val iconHolder = FrameLayout(this)
            val holderSize = SvgHelper.dp(this, w.iconBoxSize)
            val holderLp = LinearLayout.LayoutParams(holderSize, holderSize)
            holderLp.gravity = Gravity.CENTER_HORIZONTAL
            holderLp.bottomMargin = SvgHelper.dp(this, 16)
            iconHolder.layoutParams = holderLp
            SvgHelper.buildIconView(
                this,
                iconHolder,
                w.iconSvg,
                w.iconSize,
                w.iconBoxSize,
                w.iconBgOn,
                w.iconBg,
                w.iconShape,
                w.iconColorOn,
                w.iconColor
            )
            col.addView(iconHolder)
        }

        // عنوان
        if (w.t.isNotBlank()) {
            val tvTitle = TextView(this)
            tvTitle.text = w.t
            tvTitle.textSize = w.tSize.toFloat()
            tvTitle.setTextColor(SvgHelper.parseColor(w.txt))
            tvTitle.gravity = Gravity.CENTER
            tvTitle.typeface = loadFont()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = SvgHelper.dp(this, 8)
            tvTitle.layoutParams = lp
            col.addView(tvTitle)
        }

        // توضیح
        if (w.d.isNotBlank()) {
            val tvDesc = TextView(this)
            tvDesc.text = w.d
            tvDesc.textSize = w.dSize.toFloat()
            tvDesc.setTextColor(SvgHelper.parseColor(w.txt))
            tvDesc.gravity = Gravity.CENTER
            tvDesc.typeface = loadFont()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = SvgHelper.dp(this, 24)
            tvDesc.layoutParams = lp
            col.addView(tvDesc)
        }

        // دکمه
        if (w.btn.isNotBlank()) {
            val btn = Button(this)
            btn.text = w.btn
            btn.textSize = w.btnSize.toFloat()
            btn.setTextColor(SvgHelper.parseColor(w.btnTxt))
            btn.typeface = loadFont()
            btn.isAllCaps = false

            val bg = GradientDrawable()
            bg.setColor(SvgHelper.parseColor(w.btnBg))
            when (w.btnShape.lowercase()) {
                "circle" -> bg.cornerRadius = SvgHelper.dp(this, w.btnRadius).toFloat() * 100f
                "rounded" -> bg.cornerRadius = SvgHelper.dp(this, w.btnRadius).toFloat()
                "square" -> bg.cornerRadius = 0f
                else -> bg.cornerRadius = SvgHelper.dp(this, w.btnRadius).toFloat()
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
        } else {
            // اگه دکمه نبود، خودکار برو Web
            root.postDelayed({
                startActivity(Intent(this, WebActivity::class.java))
                finish()
            }, 1500)
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
