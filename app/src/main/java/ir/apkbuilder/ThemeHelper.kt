package ir.apkbuilder

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowInsetsControllerCompat

/**
 * تنظیم رنگ نوار بالا (status bar) و پایین (navigation bar).
 *
 * قانون:
 *  - رنگ هر دو نوار = همیشه welcome.bg
 *  - رنگ آیکون‌ها = خودکار بر اساس روشنایی رنگ
 *  - تنها ورودی این فایل: AppConfig
 */
object ThemeHelper {

    fun applySystemBars(activity: Activity, config: AppConfig) {
        val color = SvgHelper.parseColor(config.welcome.bg)
        applySystemBarsWithColor(activity, color)
    }

    private fun applySystemBarsWithColor(activity: Activity, color: Int) {
        val window: Window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        window.statusBarColor = color
        window.navigationBarColor = color

        val light = isColorLight(color)
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            controller.isAppearanceLightStatusBars = light
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = light
        }
    }

    /**
     * تشخیص روشنایی رنگ.
     * luminance > 0.5 => روشن => آیکون تیره
     * luminance <= 0.5 => تیره => آیکون روشن
     */
    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 0.5
    }
}
