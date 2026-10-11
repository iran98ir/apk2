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
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * صفحه آنبوردینگ.
 *
 * قانون:
 *  - تعداد صفحات نمایشی از onb.count ("2" یا "3")
 *  - صفحات از onb.pages (آرایه json)
 *  - پس‌زمینه از onb.bg/bgMode/bg2/bgAngle
 *  - رنگ متن از onb.txt
 *  - هر صفحه: آیکون + عنوان + زیرمتن با موقعیت از json
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var config: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = ConfigLoader.load(this)

        val onb = config.onb

        val root = FrameLayout(this)
        root.background = SvgHelper.makeBackground(onb.bgMode, onb.bg, onb.bg2, onb.bgAngle)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(col)

        // تعداد نمایشی
        val total = onb.count.toIntOrNull() ?: onb.pages.size
        val visiblePages = onb.pages.take(total)

        val pager = ViewPager2(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            adapter = OnbAdapter(visiblePages, onb.txt)
        }
        col.addView(pager)

        // دکمه پایین (متن ثابت — توی json نیست)
        val btn = Button(this).apply {
            text = BTN_TEXT
            setTextColor(SvgHelper.parseColor(onb.txt))
            typeface = loadFont()
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    SvgHelper.dp(this@OnboardingActivity, 24),
                    SvgHelper.dp(this@OnboardingActivity, 8),
                    SvgHelper.dp(this@OnboardingActivity, 24),
                    SvgHelper.dp(this@OnboardingActivity, 24)
                )
            }
            setOnClickListener {
                if (pager.currentItem < visiblePages.size - 1) {
                    pager.currentItem = pager.currentItem + 1
                } else {
                    finishOnb()
                }
            }
        }
        col.addView(btn)

        setContentView(root)
        ThemeHelper.applySystemBars(this, config)
    }

    private fun finishOnb() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(SplashActivity.KEY_ONB, true)
            .apply()

        val next = if (config.welcome.active) {
            WelcomeActivity::class.java
        } else {
            WebActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }

    private fun loadFont(): Typeface? = try {
        ResourcesCompat.getFont(this, R.font.lalezar)
    } catch (e: Exception) {
        null
    }

    // =========================================================
    // Adapter
    // =========================================================
    inner class OnbAdapter(
        private val pages: List<OnbPage>,
        private val txtColor: String
    ) : RecyclerView.Adapter<OnbAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val boxTop: LinearLayout    = v.findViewWithTag("top")
            val boxCenter: LinearLayout = v.findViewWithTag("center")
            val boxBottom: LinearLayout = v.findViewWithTag("bottom")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context

            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(
                    SvgHelper.dp(ctx, PAD),
                    SvgHelper.dp(ctx, PAD),
                    SvgHelper.dp(ctx, PAD),
                    SvgHelper.dp(ctx, PAD)
                )
            }

            val top    = makeBox(ctx).apply { tag = "top" }
            val center = makeBox(ctx).apply { tag = "center" }
            val bottom = makeBox(ctx).apply { tag = "bottom" }

            col.addView(top)
            col.addView(center)
            col.addView(bottom)

            return VH(col)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            val ctx = holder.itemView.context

            // پاک کردن قبلی
            holder.boxTop.removeAllViews()
            holder.boxCenter.removeAllViews()
            holder.boxBottom.removeAllViews()

            // --- آیکون ---
            if (page.iconPos != "hidden" && page.iconSvg.isNotBlank()) {
                val holderBox = FrameLayout(ctx)
                SvgHelper.buildIconView(
                    ctx, holderBox,
                    page.iconSvg, page.iconSize, page.iconBoxSize,
                    page.iconBgOn, page.iconBg, page.iconShape,
                    page.iconColorOn, page.iconColor
                )
                pickBox(holder, page.iconPos).addView(holderBox)
            }

            // --- متن (عنوان + زیرمتن با هم) ---
            if (page.textPos != "hidden") {
                val textBox = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(0, SvgHelper.dp(ctx, 16), 0, 0)
                }

                val tvTitle = TextView(ctx).apply {
                    text = page.t
                    textSize = page.tSize.toFloat()
                    setTextColor(SvgHelper.parseColor(txtColor))
                    gravity = Gravity.CENTER
                    typeface = loadFont()
                }
                textBox.addView(tvTitle)

                val tvDesc = TextView(ctx).apply {
                    text = page.d
                    textSize = page.dSize.toFloat()
                    setTextColor(SvgHelper.parseColor(txtColor))
                    gravity = Gravity.CENTER
                    typeface = loadFont()
                    setPadding(0, SvgHelper.dp(ctx, 8), 0, 0)
                }
                textBox.addView(tvDesc)

                pickBox(holder, page.textPos).addView(textBox)
            }
        }

        override fun getItemCount(): Int = pages.size

        private fun makeBox(ctx: android.content.Context): LinearLayout =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0, 1f
                )
            }

        private fun pickBox(holder: VH, pos: String): LinearLayout = when (pos) {
            "top"    -> holder.boxTop
            "center" -> holder.boxCenter
            "bottom" -> holder.boxBottom
            else     -> holder.boxCenter
        }
    }

    companion object {
        private const val PAD = 24
        private const val BTN_TEXT = "بعدی"
        private const val PREFS = "app_prefs"
    }
}
