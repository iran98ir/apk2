package ir.apkbuilder

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

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

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(onb.bgMode, onb.bg, onb.bg2, onb.bgAngle)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(col)

        val pager = ViewPager2(this)
        val pagerLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        pager.layoutParams = pagerLp
        pager.adapter = OnbAdapter(onb.pages, onb.txt)
        col.addView(pager)

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
        btn.text = "ادامه"
        btn.setTextColor(SvgHelper.parseColor(onb.txt))
        btn.typeface = loadFont()
        btn.isAllCaps = false
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

    inner class OnbAdapter(
        private val pages: List<OnbPage>,
        private val txtColor: String
    ) : RecyclerView.Adapter<OnbAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val iconHolder: FrameLayout = v.findViewWithTag("iconHolder")
            val tvTitle: TextView = v.findViewWithTag("tvTitle")
            val tvDesc: TextView = v.findViewWithTag("tvDesc")
        }

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

            val iconHolder = FrameLayout(ctx)
            iconHolder.tag = "iconHolder"
            iconHolder.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            wrap.addView(iconHolder)

            val tvTitle = TextView(ctx)
            tvTitle.tag = "tvTitle"
            tvTitle.gravity = Gravity.CENTER
            tvTitle.setPadding(0, SvgHelper.dp(ctx, 16), 0, SvgHelper.dp(ctx, 8))
            wrap.addView(tvTitle)

            val tvDesc = TextView(ctx)
            tvDesc.tag = "tvDesc"
            tvDesc.gravity = Gravity.CENTER
            wrap.addView(tvDesc)

            return VH(wrap)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]

            holder.iconHolder.removeAllViews()
            if (!page.iconSvg.isNullOrBlank()) {
                SvgHelper.buildIconView(
                    holder.itemView.context,
                    holder.iconHolder,
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

            holder.tvTitle.text = page.t
            holder.tvTitle.textSize = page.tSize.toFloat()
            holder.tvTitle.setTextColor(SvgHelper.parseColor(txtColor))
            holder.tvTitle.typeface = loadFont()

            holder.tvDesc.text = page.d
            holder.tvDesc.textSize = page.dSize.toFloat()
            holder.tvDesc.setTextColor(SvgHelper.parseColor(txtColor))
            holder.tvDesc.typeface = loadFont()
        }

        override fun getItemCount(): Int = pages.size
    }
}
