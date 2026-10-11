package ir.apkbuilder

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * دیالوگ خروج.
 *
 * قانون:
 *  - عنوان از exit.t
 *  - پیام از exit.m
 *  - رنگ‌ها از exit.bg / exit.txt
 *  - اندازه‌ها از exit.tSize / mSize / btnSize
 *  - دکمه‌ها از exit.yes / no + رنگ‌ها + شکل + استایل + چیدمان
 *  - انیمیشن از exit.anim (glass / fade / scale)
 */
object ExitDialog {

    fun show(activity: Activity, config: AppConfig, onNo: () -> Unit) {
        val e = config.exit

        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
        }

        val col = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                SvgHelper.dp(activity, PAD),
                SvgHelper.dp(activity, PAD),
                SvgHelper.dp(activity, PAD),
                SvgHelper.dp(activity, PAD)
            )
            background = GradientDrawable().apply {
                setColor(SvgHelper.parseColor(e.bg))
                cornerRadius = SvgHelper.dp(activity, 16).toFloat()
            }
        }

        // --- عنوان ---
        if (e.t.isNotBlank()) {
            col.addView(TextView(activity).apply {
                text = e.t
                textSize = e.tSize.toFloat()
                setTextColor(SvgHelper.parseColor(e.txt))
                gravity = Gravity.CENTER
                typeface = loadFont(activity)
            })
        }

        // --- پیام ---
        if (e.m.isNotBlank()) {
            val tv = TextView(activity).apply {
                text = e.m
                textSize = e.mSize.toFloat()
                setTextColor(SvgHelper.parseColor(e.txt))
                gravity = Gravity.CENTER
                typeface = loadFont(activity)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = SvgHelper.dp(activity, 8)
            }
            tv.layoutParams = lp
            col.addView(tv)
        }

        // --- دکمه‌ها ---
        val row = LinearLayout(activity).apply {
            orientation = if (e.btnLayout == "col") {
                LinearLayout.VERTICAL
            } else {
                LinearLayout.HORIZONTAL
            }
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = SvgHelper.dp(activity, 16)
            }
        }

        val btnYes = makeButton(
            activity,
            e.yes, e.yesBg, e.yesTxt,
            e.btnSize, e.btnShape, e.btnStyle
        )
        val btnNo = makeButton(
            activity,
            e.no, e.noBg, e.noTxt,
            e.btnSize, e.btnShape, e.btnStyle
        )

        if (e.btnLayout == "col") {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            btnYes.layoutParams = lp
            btnNo.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = SvgHelper.dp(activity, 8) }
            row.addView(btnYes)
            row.addView(btnNo)
        } else {
            btnYes.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            btnNo.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = SvgHelper.dp(activity, 8) }
            row.addView(btnYes)
            row.addView(btnNo)
        }

        col.addView(row)

        btnYes.setOnClickListener {
            dialog.dismiss()
            activity.finish()
        }
        btnNo.setOnClickListener {
            dialog.dismiss()
            onNo()
        }

        dialog.setContentView(col)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.88).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // --- انیمیشن بر اساس exit.anim ---
            when (e.anim) {
                "fade"  -> setWindowAnimations(android.R.style.Animation_Dialog)
                "scale" -> setWindowAnimations(android.R.style.Animation_Dialog)
                "glass" -> setWindowAnimations(android.R.style.Animation_Dialog)
            }
        }

        dialog.show()
    }

    private fun makeButton(
        activity: Activity,
        text: String,
        bgColor: String,
        textColor: String,
        size: Int,
        shape: String,
        style: String
    ): Button {
        val bg = GradientDrawable()
        val solid = SvgHelper.parseColor(bgColor)

        when (style) {
            "outline" -> {
                bg.setColor(0x00000000)
                bg.setStroke(SvgHelper.dp(activity, 2), solid)
            }
            else -> {
                bg.setColor(solid)
            }
        }

        when (shape) {
            "pill"    -> bg.cornerRadius = 999f
            "square"  -> bg.cornerRadius = 0f
            "rounded" -> bg.cornerRadius = SvgHelper.dp(activity, 8).toFloat()
        }

        return Button(activity).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(
                if (style == "outline") solid
                else SvgHelper.parseColor(textColor)
            )
            isAllCaps = false
            typeface = loadFont(activity)
            background = bg
        }
    }

    private fun loadFont(activity: Activity): Typeface? = try {
        ResourcesCompat.getFont(activity, R.font.lalezar)
    } catch (e: Exception) {
        null
    }

    private const val PAD = 24
}
