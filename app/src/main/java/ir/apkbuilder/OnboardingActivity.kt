package ir.apkbuilder

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.graphics.drawable.GradientDrawable

/**
 * Onboarding: صفحات از json.
 * هر صفحه: آیکون SVG + عنوان + توضیح.
 * دکمه پایین برای رفتن به صفحه بعد / ورود.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var config: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = ConfigLoader.load(this)

        val onb = config.onb

        // ریشه
        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(onb.bgMode, onb.bg, onb.bg2, onb.bgAngle)

        // ستون اصلی
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(col)

        // ViewPager2
        val pager = ViewPager2(this)
        val pagerLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        pager.layoutParams = pagerLp
        pager.adapter = OnbAdapter(onb.pages, onb.txt)
        col.addView(pager)

        // دکمه پایین
        val btn = Button(this)
        val btnLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnLp.setMargins(
            SvgHelper.dp(this, 24),
            SvgHelper.dp(this, 8),
            SvgHelper.dp(this, 24),
            SvgHelper.dp(this, 24)
        )
        btn.layoutParams = btnLp
        btn.text = "ادامه" // متن از json نمیاد چون توی نمونه json ندیدیم
        btn.setTextColor(SvgHelper.parseColor(onb.txt))
        btn.typeface = loadFont()
        col.addView(btn)

        btn.setOnClickListener {
            val current = pager.currentItem
            if (current < onb.pages.size - 1) {
                pager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        setContentView(root)
        ThemeHelper.applySystemBars(this, config)
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onb_shown", true).apply()

        val next = if (config.welcome.active) {
            WelcomeActivity::class.java
        } else {
            WebActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }

    private fun loadFont(): Typeface? {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(this, R.font.lalezar)
        } catch (e: Exception) {
            null
        }
    }

    // Adapter
    inner class OnbAdapter(
        private val pages: List<OnbPage>,
        private val txtColor: String
    ) : RecyclerView.Adapter<OnbAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context

            val wrap = LinearLayout(ctx)
            wrap.orientation = LinearLayout.VERTICAL
            wrap.gravity = Gravity.CENTER
            wrap.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            wrap.setPadding(
                SvgHelper.dp(ctx, 24),
                SvgHelper.dp(ctx, 24),
                SvgHelper.dp(ctx, 24),
                SvgHelper.dp(ctx, 24)
            )

            // آیکون
            val iconHolder = FrameLayout(ctx)
            iconHolder.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            wrap.addView(iconHolder)

            // عنوان
            val tvTitle = TextView(ctx)
            tvTitle.gravity = Gravity.CENTER
            tvTitle.setPadding(0, SvgHelper.dp(ctx, 16), 0, SvgHelper.dp(ctx, 8))
            wrap.addView(tvTitle)

            // توضیح
            val tvDesc = TextView(ctx)
            tvDesc.gravity = Gravity.CENTER
            wrap.addView(tvDesc)

            wrap.tag = Triple(iconHolder, tvTitle, tvDesc)
            return VH(wrap)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            val wrap = holder.itemView as LinearLayout
            @Suppress("UNCHECKED_CAST")
            val (iconHolder, tvTitle, tvDesc) = wrap.tag as Triple<FrameLayout, TextView, TextView>

            iconHolder.removeAllViews()
            if (!page.iconSvg.isNullOrBlank()) {
                SvgHelper.buildIconView(
                    wrap.context,
                    iconHolder,
                    page.iconSvg,
                    page.iconSize,
                    page.iconBoxSize,
                    page.iconBgOn,
                    page.iconBg,
                    page.iconShape,
                    page.iconColorOn,
                    page.iconColor
                )
            }

            tvTitle.text = page.t
            tvTitle.textSize = page.tSize.toFloat()
            tvTitle.setTextColor(SvgHelper.parseColor(txtColor))
            tvTitle.typeface = loadFont()

            tvDesc.text = page.d
            tvDesc.textSize = page.dSize.toFloat()
            tvDesc.setTextColor(SvgHelper.parseColor(txtColor))
            tvDesc.typeface = loadFont()
        }

        override fun getItemCount(): Int = pages.size
    }
}
