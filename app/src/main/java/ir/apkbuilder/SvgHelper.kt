package ir.apkbuilder

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PictureDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.Paint
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.view.Gravity
import android.util.TypedValue
import com.caverock.androidsvg.SVG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * ابزار رندر SVG، ساخت پس‌زمینه (single/gradient)، ساخت باکس آیکون.
 * همه مقادیر از config میاد.
 */
object SvgHelper {

    fun parseColor(c: String): Int {
        return try {
            Color.parseColor(c)
        } catch (e: Exception) {
            Color.BLACK
        }
    }

    /**
     * از یک رشته SVG، Drawable می‌سازد.
     * رنگ آیکون رو هم اگه iconColorOn باشه با iconColor جایگزین می‌کند.
     */
    fun svgToDrawable(
        context: Context,
        svgString: String?,
        tint: Int? = null,
        sizePx: Int? = null
    ): Drawable? {
        if (svgString.isNullOrBlank()) return null
        return try {
            val svg = SVG.getFromString(svgString)
            // اگه size داده شده، اندازه‌ی بوم رو تنظیم کن
            if (sizePx != null) {
                svg.setDocumentWidth(sizePx.toFloat())
                svg.setDocumentHeight(sizePx.toFloat())
            }
            val bmp = Bitmap.createBitmap(
                svg.documentWidth.toInt().coerceAtLeast(1),
                svg.documentHeight.toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            svg.renderToCanvas(canvas)
            val d = BitmapDrawable(context.resources, bmp)
            if (tint != null) {
                d.setTint(tint)
            }
            d
        } catch (e: Exception) {
            null
        }
    }

    /**
     * پس‌زمینه‌ی single یا gradient می‌سازد.
     */
    fun makeBackground(
        bgMode: String,
        bg: String,
        bg2: String,
        bgAngle: Int
    ): Drawable {
        val color1 = parseColor(bg)
        val color2 = parseColor(if (bg2.isBlank()) bg else bg2)

        return if (bgMode.equals("gradient", ignoreCase = true)) {
            val gd = GradientDrawable(
                angleToOrientation(bgAngle),
                intArrayOf(color1, color2)
            )
            gd
        } else {
            val gd = GradientDrawable()
            gd.setColor(color1)
            gd
        }
    }

    private fun angleToOrientation(angle: Int): GradientDrawable.Orientation {
        // تبدیل زاویه به orientation نزدیک‌ترین
        return when (((angle % 360) + 360) % 360) {
            in 0..22, in 338..360 -> GradientDrawable.Orientation.LEFT_RIGHT
            in 23..67 -> GradientDrawable.Orientation.BL_TR
            in 68..112 -> GradientDrawable.Orientation.BOTTOM_TOP
            in 113..157 -> GradientDrawable.Orientation.BR_TL
            in 158..202 -> GradientDrawable.Orientation.RIGHT_LEFT
            in 203..247 -> GradientDrawable.Orientation.TR_BL
            in 248..292 -> GradientDrawable.Orientation.TOP_BOTTOM
            else -> GradientDrawable.Orientation.TL_BR
        }
    }

    /**
     * ساخت باکس آیکون: شکل (square/circle/rounded) + رنگ پس‌زمینه + گرادیانت اختیاری
     * سپس SVG رو داخلش قرار می‌دهد.
     */
    fun buildIconView(
        context: Context,
        parent: FrameLayout,
        iconSvg: String?,
        iconSizeDp: Int,
        iconBoxSizeDp: Int,
        iconBgOn: Boolean,
        iconBgColor: String,
        iconShape: String,
        iconColorOn: Boolean,
        iconColor: String
    ): ImageView {
        // اندازه‌ها
        val boxPx = dp(context, iconBoxSizeDp)
        val iconPx = dp(context, iconSizeDp)

        // FrameLayout با اندازه باکس
        val box = FrameLayout(context)
        val boxLp = FrameLayout.LayoutParams(boxPx, boxPx)
        box.layoutParams = boxLp

        // اگر bg روشن است -> پس‌زمینه به box
        if (iconBgOn) {
            val bg = GradientDrawable()
            bg.setColor(parseColor(iconBgColor))
            when (iconShape.lowercase()) {
                "circle" -> bg.shape = GradientDrawable.OVAL
                "rounded" -> bg.cornerRadius = boxPx / 4f
                else -> bg.shape = GradientDrawable.RECTANGLE
            }
            box.background = bg
        } else if (iconShape.lowercase() == "circle") {
            // حتی بدون رنگ، اگه circle بود، یه کلیپ دایره‌ای نیاز داریم
            // ساده: بدون پس‌زمینه
        }

        val img = ImageView(context)
        val imgLp = FrameLayout.LayoutParams(iconPx, iconPx)
        imgLp.gravity = Gravity.CENTER
        img.layoutParams = imgLp
        img.scaleType = ImageView.ScaleType.FIT_CENTER

        val tint = if (iconColorOn) parseColor(iconColor) else null
        val d = svgToDrawable(context, iconSvg, tint, iconPx)
        if (d != null) img.setImageDrawable(d)

        box.addView(img)
        parent.addView(box)
        return img
    }

    fun dp(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
