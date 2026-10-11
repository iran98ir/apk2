package ir.apkbuilder

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

/**
 * صفحه اسپلش.
 *
 * قانون:
 *  - پس‌زمینه از splash.bgMode/bg/bg2/bgAngle
 *  - آیکون از splash.hasIcon/iconSvg/iconSize/...
 *  - متن از splash.hasText/title/tSize/txt
 *  - محل قرارگیری از splash.iconPos / splash.textPos
 *  - اگه مقدار "hidden" بود → همون عنصر مخفی میشه
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val sp = config.splash

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(sp.bgMode, sp.bg, sp.bg2, sp.bgAngle)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(
                SvgHelper.dp(this@SplashActivity, PAD),
                SvgHelper.dp(this@SplashActivity, PAD),
                SvgHelper.dp(this@SplashActivity, PAD),
                SvgHelper.dp(this@SplashActivity, PAD)
            )
        }

        // --- آیکون ---
        if (sp.hasIcon && sp.iconPos != "hidden") {
            val holder = FrameLayout(this)
            holder.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            SvgHelper.buildIconView(
                this, holder,
                sp.iconSvg, sp.iconSize, sp.iconBoxSize,
                sp.iconBgOn, sp.iconBg, sp.iconShape,
                sp.iconColorOn, sp.iconColor
            )
            addToColumn(col, holder, sp.iconPos)
        }

        // --- متن ---
        if (sp.hasText && sp.textPos != "hidden") {
            val tv = TextView(this).apply {
                text = sp.title
                textSize = sp.tSize.toFloat()
                setTextColor(SvgHelper.parseColor(sp.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()
            }
            addToColumn(col, tv, sp.textPos)
        }

        root.addView(col)
        setContentView(root)

        ThemeHelper.applySystemBars(this, config)

        root.postDelayed({ goNext(config) }, SPLASH_DELAY)
    }

    private fun addToColumn(col: LinearLayout, view: android.view.View, pos: String) {
        // ترتیب: بالا، وسط، پایین
        // از LinearLayout با weight استفاده نمی‌کنیم؛ به جاش سه باکس می‌سازیم
        when (pos) {
            "top"    -> col.addView(view, 0)
            "center" -> col.addView(view)
            "bottom" -> col.addView(view)
        }
    }

    private fun goNext(config: AppConfig) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        val showOnb = config.onb.active &&
                config.onb.pages.isNotEmpty() &&
                (!config.onb.firstRunOnly || !prefs.getBoolean(KEY_ONB, false))

        val next = when {
            showOnb            -> OnboardingActivity::class.java
            config.welcome.active -> WelcomeActivity::class.java
            else               -> WebActivity::class.java
        }

        startActivity(Intent(this, next))
        finish()
    }

    private fun loadFont(): Typeface? = try {
        ResourcesCompat.getFont(this, R.font.lalezar)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val PAD = 24
        private const val SPLASH_DELAY = 2000L
        private const val PREFS = "app_prefs"
        const val KEY_ONB = "onb_shown"
    }
}
