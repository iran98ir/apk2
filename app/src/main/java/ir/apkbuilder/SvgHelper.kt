package ir.apkbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.ImageView
import android.view.Gravity
import com.caverock.androidsvg.SVG

/**
 * ابزار SVG و پس‌زمینه.
 *
 * قانون:
 *  - این فایل هیچ مقدار از json نمی‌دونه. فقط مقادیر دریافتی رو اعمال می‌کنه.
 *  - مقادیر از بیرون میان (از AppConfig).
 */
object SvgHelper {

    // ---------------------------------------------------------
    // رنگ
    // ---------------------------------------------------------
    fun parseColor(hex: String): Int {
        return Color.parseColor(hex)
    }

    // ---------------------------------------------------------
    // dp → px
    // ---------------------------------------------------------
    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    // ---------------------------------------------------------
    // SVG String → Drawable
    // ---------------------------------------------------------
    fun svgToDrawable(
        context: Context,
        svgString: String,
        tint: Int?,
        sizePx: Int
    ): Drawable? {
        if (svgString.isBlank()) return null
        return try {
            val svg = SVG.getFromString(svgString)
            svg.setDocumentWidth(sizePx.toFloat())
            svg.setDocumentHeight(sizePx.toFloat())

            val bmp = Bitmap.createBitmap(
                sizePx.coerceAtLeast(1),
                sizePx.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            svg.renderToCanvas(canvas)

            val d = BitmapDrawable(context.resources, bmp)
            if (tint != null) d.setTint(tint)
            d
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------
    // ساخت پس‌زمینه: single یا gradient
    // ---------------------------------------------------------
    fun makeBackground(
        bgMode: String,
        bg: String,
        bg2: String,
        bgAngle: Int
    ): Drawable {
        val color1 = parseColor(bg)
        val color2 = parseColor(bg2)

        return if (bgMode == "gradient") {
            GradientDrawable(
                angleToOrientation(bgAngle),
                intArrayOf(color1, color2)
            )
        } else {
            GradientDrawable().apply { setColor(color1) }
        }
    }

    // ---------------------------------------------------------
    // زاویه → orientation گرادیانت
    // ---------------------------------------------------------
    private fun angleToOrientation(angle: Int): GradientDrawable.Orientation {
        return when (((angle % 360) + 360) % 360) {
            in 0..22, in 338..360 -> GradientDrawable.Orientation.LEFT_RIGHT
            in 23..67             -> GradientDrawable.Orientation.BL_TR
            in 68..112            -> GradientDrawable.Orientation.BOTTOM_TOP
            in 113..157           -> GradientDrawable.Orientation.BR_TL
            in 158..202           -> GradientDrawable.Orientation.RIGHT_LEFT
            in 203..247           -> GradientDrawable.Orientation.TR_BL
            in 248..292           -> GradientDrawable.Orientation.TOP_BOTTOM
            else                  -> GradientDrawable.Orientation.TL_BR
        }
    }

    // ---------------------------------------------------------
    // ساخت باکس آیکون + SVG داخلش
    //
    // ورودی‌ها همه از json میان. این تابع هیچ تصمیمی نمی‌گیره.
    // ---------------------------------------------------------
    fun buildIconView(
        context: Context,
        parent: FrameLayout,
        iconSvg: String,
        iconSizeDp: Int,
        iconBoxSizeDp: Int,
        iconBgOn: Boolean,
        iconBgColor: String,
        iconShape: String,
        iconColorOn: Boolean,
        iconColor: String
    ) {
        val boxPx  = dp(context, iconBoxSizeDp)
        val iconPx = dp(context, iconSizeDp)

        // باکس
        val box = FrameLayout(context)
        box.layoutParams = FrameLayout.LayoutParams(boxPx, boxPx)

        // پس‌زمینه باکس (اگه iconBgOn true باشه)
        if (iconBgOn) {
            val bg = GradientDrawable()
            bg.setColor(parseColor(iconBgColor))
            when (iconShape) {
                "circle"   -> bg.shape = GradientDrawable.OVAL
                "rounded"  -> bg.cornerRadius = boxPx / 4f
                "squircle" -> bg.cornerRadius = boxPx * 0.30f
                "square"   -> bg.cornerRadius = 0f
            }
            box.background = bg
        } else if (iconShape == "circle") {
            // اگه bg خاموشه ولی شکل دایره‌ست، فقط clip می‌کنیم
            // (فعلاً ساده رد می‌شیم)
        }

        // آیکون داخل باکس
        val img = ImageView(context)
        img.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
            gravity = Gravity.CENTER
        }
        img.scaleType = ImageView.ScaleType.FIT_CENTER

        val tint = if (iconColorOn) parseColor(iconColor) else null
        val drawable = svgToDrawable(context, iconSvg, tint, iconPx)
        if (drawable != null) img.setImageDrawable(drawable)

        box.addView(img)
        parent.addView(box)
    }
}
