package ir.apkbuilder

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * تنظیم رنگ نوار بالا (status) و پایین (navigation) بر اساس welcome.bg
 * و آیکون‌ها به صورت خودکار (تیره -> سفید، روشن -> مشکی)
 */
object ThemeHelper {

    /**
     * رنگ نوارها = دقیقاً welcome.bg
     * (اگه گرادیانت بود، از رنگ اول استفاده می‌کنیم)
     */
    fun applySystemBars(activity: Activity, config: AppConfig) {
        val welcomeBg = SvgHelper.parseColor(config.welcome.bg)
        applySystemBarsWithColor(activity, welcomeBg)
    }

    fun applySystemBarsWithColor(activity: Activity, color: Int) {
        val window: Window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        window.statusBarColor = color
        window.navigationBarColor = color

        // آیکون‌ها: اگه رنگ روشن بود -> آیکون تیره، وگرنه -> آیکون روشن
        val isLight = isColorLight(color)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = isLight
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightNavigationBars = isLight
        }
    }

    /**
     * روشنایی رنگ: luminance > 0.5 => روشن
     */
    fun isColorLight(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 0.5
    }

    /**
     * تمام‌صفحه (اختیاری) — فعلاً استفاده نمی‌کنیم
     */
    fun applyEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
    }
}
