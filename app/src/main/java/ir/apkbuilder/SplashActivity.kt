package ir.apkbuilder

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Splash: پس‌زمینه از json + آیکون SVG اختیاری + عنوان اختیاری.
 * بعد از ۲ ثانیه → Onboarding (اگه فعال) یا Welcome.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)
        val sp = config.splash

        // ریشه: FrameLayout با پس‌زمینه
        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(sp.bgMode, sp.bg, sp.bg2, sp.bgAngle)

        // ستون مرکزی عمودی
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        val colLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        col.layoutParams = colLp
        root.addView(col)

        // آیکون (اگه تعریف شده)
        if (sp.hasIcon && !sp.iconSvg.isNullOrBlank()) {
            val iconHolder = FrameLayout(this)
            val holderSize = SvgHelper.dp(this, sp.iconBoxSize)
            val holderLp = LinearLayout.LayoutParams(holderSize, holderSize)
            holderLp.gravity = Gravity.CENTER_HORIZONTAL
            iconHolder.layoutParams = holderLp
            SvgHelper.buildIconView(
                this,
                iconHolder,
                sp.iconSvg,
                sp.iconSize,
                sp.iconBoxSize,
                sp.iconBgOn,
                sp.iconBg,
                sp.iconShape,
                sp.iconColorOn,
                sp.iconColor
            )
            // فاصله زیرش
            val wrap = LinearLayout.LayoutParams(holderSize, holderSize)
            wrap.gravity = Gravity.CENTER_HORIZONTAL
            wrap.bottomMargin = SvgHelper.dp(this, 16)

            // چون buildIconView قبلاً box رو به parent اضافه کرده،
            // برای سادگی اینجا holder رو مستقیم اضافه می‌کنیم
            col.addView(iconHolder, wrap)
        }

        // متن (اگه تعریف شده)
        if (sp.hasText && sp.title.isNotBlank()) {
            val tv = TextView(this)
            tv.text = sp.title
            tv.setTextColor(SvgHelper.parseColor(sp.txt))
            tv.textSize = sp.tSize.toFloat()
            tv.gravity = Gravity.CENTER
            tv.typeface = loadFont()
            col.addView(tv)
        }

        setContentView(root)

        // رنگ نوارها
        ThemeHelper.applySystemBars(this, config)

        // بعد از ۲ ثانیه برو مرحله بعد
        root.postDelayed({
            val next = when {
                config.onb.active && config.onb.pages.isNotEmpty() &&
                        shouldShowOnboarding(config) -> OnboardingActivity::class.java
                config.welcome.active -> WelcomeActivity::class.java
                else -> WebActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
        }, 2000)
    }

    private fun shouldShowOnboarding(config: AppConfig): Boolean {
        if (!config.onb.firstRunOnly) return true
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return !prefs.getBoolean("onb_shown", false)
    }

    private fun loadFont(): Typeface? {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(this, R.font.lalezar)
        } catch (e: Exception) {
            null
        }
    }
}
