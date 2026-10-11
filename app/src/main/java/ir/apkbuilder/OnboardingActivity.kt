package ir.apkbuilder

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * صفحه آنبوردینگ — نسخه نهایی.
 *
 * قانون:
 *  - تعداد صفحات نمایشی از onb.count
 *  - محتوا از onb.pages
 *  - پس‌زمینه از onb.bgMode/bg/bg2/bgAngle
 *  - رنگ متن از onb.txt
 *  - Dots پایین: صفحه فعلی = مستطیل دو سر گرد نئونی، بقیه = نقطه پر
 *  - رنگ Dots = onb.txt با افکت نئون/درخشان
 *  - بدون دکمه Next — فقط Swipe
 *  - توی صفحه آخر، یه سوایپ دیگه به چپ → Welcome
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var dotsContainer: LinearLayout
    private lateinit var pager: ViewPager2
    private var totalPages: Int = 0
    private var currentPage: Int = 0

    @SuppressLint("ClickableViewAccessibility")
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

        val count = onb.count.toIntOrNull() ?: onb.pages.size
        val visiblePages = onb.pages.take(count)
        totalPages = visiblePages.size

        pager = ViewPager2(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            adapter = OnbAdapter(visiblePages, onb.txt)
            offscreenPageLimit = 1
        }
        col.addView(pager)

        dotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = SvgHelper.dp(this@OnboardingActivity, 40)
                topMargin = SvgHelper.dp(this@OnboardingActivity, 20)
            }
        }
        col.addView(dotsContainer)

        buildDots(onb.txt, 0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                buildDots(onb.txt, position)
            }
        })

        // تشخیص سوایپ اضافی به چپ توی صفحه آخر
        pager.getChildAt(0).setOnTouchListener { _, event ->
            handleExtraSwipe(event)
            false
        }

        setContentView(root)
        ThemeHelper.applySystemBars(this, config)
    }

    /**
     * اگه کاربر توی صفحه آخر بود و به چپ کشید → برو Welcome
     */
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f

    private fun handleExtraSwipe(event: MotionEvent) {
        if (totalPages == 0) return
        if (currentPage != totalPages - 1) return

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                // سوایپ به چپ: dx منفی، و افقی بیشتر از عمودی
                if (dx < -SWIPE_THRESHOLD && Math.abs(dx) > Math.abs(dy)) {
                    finishOnb()
                }
            }
        }
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

    // =========================================================
    // Dots نئونی
    // =========================================================
    private fun buildDots(txtColor: String, current: Int) {
        dotsContainer.removeAllViews()

        val baseColor = SvgHelper.parseColor(txtColor)
        val glowColor = withAlpha(baseColor, 0.45f)
        val dotColor  = withAlpha(baseColor, 0.95f)

        val dotSize = SvgHelper.dp(this, 8)
        val activeW = SvgHelper.dp(this, 26)
        val activeH = SvgHelper.dp(this, 8)
        val gap     = SvgHelper.dp(this, 8)

        for (i in 0 until totalPages) {
            val isActive = i == current

            val wrap = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (isActive) activeW else dotSize,
                    if (isActive) activeH else dotSize
                ).apply {
                    marginStart = if (i == 0) 0 else gap
                }
            }

            // هاله نئونی
            val glow = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    leftMargin   = -SvgHelper.dp(this@OnboardingActivity, 3)
                    rightMargin  = -SvgHelper.dp(this@OnboardingActivity, 3)
                    topMargin    = -SvgHelper.dp(this@OnboardingActivity, 3)
                    bottomMargin = -SvgHelper.dp(this@OnboardingActivity, 3)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = if (isActive) (activeH + 6) / 2f
                                   else (dotSize + 6) / 2f
                    setColor(if (isActive) glowColor else withAlpha(baseColor, 0.15f))
                }
                alpha = if (isActive) 0.85f else 0.4f
            }
            wrap.addView(glow)

            // نقطه اصلی
            val dot = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = if (isActive) activeH / 2f else dotSize / 2f
                    setColor(dotColor)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    outlineAmbientShadowColor = glowColor
                    outlineSpotShadowColor = glowColor
                }
                elevation = SvgHelper.dp(this@OnboardingActivity, 6).toFloat()
            }
            wrap.addView(dot)

            dotsContainer.addView(wrap)
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
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

            holder.boxTop.removeAllViews()
            holder.boxCenter.removeAllViews()
            holder.boxBottom.removeAllViews()

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

            if (page.textPos != "hidden") {
                val textBox = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
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
        private const val SWIPE_THRESHOLD = 80f
        private const val PREFS = "app_prefs"
    }
}
