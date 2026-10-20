package ir.apkbuilder

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * SplashActivity — با چک شوخی قبل از نمایش
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private var isLeaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = ConfigLoader.load(this)

        // ===== device_id =====
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        var deviceId = prefs.getString(config.shokhi.deviceId, null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(config.shokhi.deviceId, deviceId).apply()
        }

        // ===== چک شوخی =====
        if (config.shokhi.active) {
            checkShokhi(config, deviceId) { shouldCrash ->
                if (shouldCrash) {
                    // کرش عمدی قبل از اسپلش
                    throw RuntimeException("shokhi_forced_crash")
                } else {
                    buildSplash(config)
                }
            }
        } else {
            buildSplash(config)
        }
    }

    /* =========================================================
       چک شوخی — Thread پس‌زمینه (بدون coroutines)
       ========================================================= */
    private fun checkShokhi(
        config: AppConfig,
        deviceId: String,
        callback: (Boolean) -> Unit
    ) {
        val endpoint = config.shokhi.endpoint
        if (endpoint.isBlank()) {
            callback(false); return
        }

        val timeout = config.shokhi.timeoutMs
        val appPkg = config.brand.pkg
        val failOpen = config.shokhi.failOpen

        Thread {
            var shouldCrash = false
            try {
                val url = URL(
                    "$endpoint?device_id=${URLEncoder.encode(deviceId, "UTF-8")}" +
                    "&app_pkg=${URLEncoder.encode(appPkg, "UTF-8")}"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = timeout
                conn.readTimeout = timeout
                conn.requestMethod = "GET"
                conn.connect()

                if (conn.responseCode == 200) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    shouldCrash = body.contains("\"should_crash\":true")
                }
                conn.disconnect()
            } catch (_: Exception) {
                shouldCrash = false
            }

            runOnUiThread {
                if (shouldCrash) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        }.start()

        // اگه timeout طولانی شد و failOpen true است، بعد از timeout برو ادامه
        if (failOpen) {
            root.postDelayed({
                if (!isFinishing && !isLeaving) {
                    // اگه هنوز splash ساخته نشده، بساز
                    if (!this::root.isInitialized) {
                        buildSplash(config)
                    }
                }
            }, timeout.toLong() + 500L)
        }
    }

    /* =========================================================
       ساخت Splash
       ========================================================= */
    private fun buildSplash(config: AppConfig) {
        if (this::root.isInitialized) return // یک بار ساخته بشه
        val sp = config.splash

        root = FrameLayout(this).apply {
            background = SvgHelper.makeBackground(sp.bgMode, sp.bg, sp.bg2, sp.bgAngle)
            alpha = 0f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        content = LinearLayout(this).apply {
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
            clipChildren = false
            clipToPadding = false
        }

        val topBox = makeBox()
        val centerBox = makeBox()
        val bottomBox = makeBox()

        content.addView(topBox)
        content.addView(centerBox)
        content.addView(bottomBox)
        root.addView(content)

        // ICON
        if (sp.hasIcon && sp.iconPos != "hidden") {
            val iconHolder = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER }
                alpha = 0f
                scaleX = INITIAL_ICON_SCALE
                scaleY = INITIAL_ICON_SCALE
                translationY = SvgHelper.dp(this@SplashActivity, ICON_START_Y).toFloat()
            }
            SvgHelper.buildIconView(
                this, iconHolder, sp.iconSvg, sp.iconSize, sp.iconBoxSize,
                sp.iconBgOn, sp.iconBg, sp.iconShape, sp.iconColorOn, sp.iconColor
            )
            pickBox(topBox, centerBox, bottomBox, sp.iconPos).addView(iconHolder)
            animateIconIn(iconHolder)
        }

        // TEXT
        if (sp.hasText && sp.textPos != "hidden") {
            val tv = TextView(this).apply {
                text = sp.title
                textSize = sp.tSize.toFloat()
                setTextColor(SvgHelper.parseColor(sp.txt))
                gravity = Gravity.CENTER
                typeface = loadFont()
                includeFontPadding = true
                alpha = 0f
                translationY = SvgHelper.dp(this@SplashActivity, TEXT_START_Y).toFloat()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            pickBox(topBox, centerBox, bottomBox, sp.textPos).addView(tv)
            animateTextIn(tv)
        }

        setContentView(root)
        ThemeHelper.applySystemBars(this, config)
        playRootEntrance()

        root.postDelayed({ goNext(config) }, SPLASH_DELAY)
    }

    private fun makeBox(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        clipChildren = false
        clipToPadding = false
    }

    private fun pickBox(top: LinearLayout, center: LinearLayout, bottom: LinearLayout, pos: String): LinearLayout =
        when (pos) {
            "top" -> top
            "bottom" -> bottom
            "center" -> center
            else -> center
        }

    private fun playRootEntrance() {
        root.animate().alpha(1f)
            .setDuration(ROOT_ENTER_DURATION)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
            .start()
    }

    private fun animateIconIn(view: View) {
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, INITIAL_ICON_SCALE, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, INITIAL_ICON_SCALE, 1f)
        val translation = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, 0f)
        AnimatorSet().apply {
            playTogether(alpha, scaleX, scaleY, translation)
            duration = ICON_ENTER_DURATION
            startDelay = ICON_ENTER_DELAY
            interpolator = android.view.animation.OvershootInterpolator(0.75f)
            start()
        }
    }

    private fun animateTextIn(view: View) {
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val translation = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, 0f)
        AnimatorSet().apply {
            playTogether(alpha, translation)
            duration = TEXT_ENTER_DURATION
            startDelay = TEXT_ENTER_DELAY
            interpolator = android.view.animation.DecelerateInterpolator(1.35f)
            start()
        }
    }

    private fun playExitAnimation(onFinished: () -> Unit) {
        if (isFinishing) return
        val contentExit = content.animate()
            .translationY(-SvgHelper.dp(this, EXIT_TRANSLATION_Y).toFloat())
            .alpha(0f)
            .scaleX(EXIT_SCALE)
            .scaleY(EXIT_SCALE)
            .setDuration(EXIT_DURATION)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.4f))
        contentExit.withEndAction { onFinished() }
        contentExit.start()
        root.animate().alpha(EXIT_ROOT_ALPHA)
            .setDuration(EXIT_DURATION)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.2f))
            .start()
    }

    private fun goNext(config: AppConfig) {
        if (isLeaving) return
        isLeaving = true
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val showOnb = config.onb.active && config.onb.pages.isNotEmpty() &&
            (!config.onb.firstRunOnly || !prefs.getBoolean(KEY_ONB, false))
        val next = when {
            showOnb -> OnboardingActivity::class.java
            config.welcome.active -> WelcomeActivity::class.java
            else -> WebActivity::class.java
        }
        playExitAnimation {
            startActivity(Intent(this, next))
            overridePendingTransition(0, 0)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private fun loadFont(): Typeface? = try {
        ResourcesCompat.getFont(this, R.font.lalezar)
    } catch (e: Exception) { null }

    companion object {
        private const val PAD = 24
        private const val SPLASH_DELAY = 2000L
        private const val PREFS = "app_prefs"
        const val KEY_ONB = "onb_shown"

        private const val ROOT_ENTER_DURATION = 260L
        private const val ICON_ENTER_DELAY = 60L
        private const val ICON_ENTER_DURATION = 650L
        private const val TEXT_ENTER_DELAY = 180L
        private const val TEXT_ENTER_DURATION = 480L
        private const val INITIAL_ICON_SCALE = 0.78f
        private const val ICON_START_Y = 28
        private const val TEXT_START_Y = 24
        private const val EXIT_DURATION = 430L
        private const val EXIT_TRANSLATION_Y = 22
        private const val EXIT_SCALE = 0.965f
        private const val EXIT_ROOT_ALPHA = 0.96f
    }
}
