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

/**
 * دیالوگ خروج.
 * همه مقادیر از exit توی json.
 * btnLayout: row یا column.
 */
object ExitDialog {

    fun show(activity: Activity, config: AppConfig, onCancel: () -> Unit) {
        val e = config.exit

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val col = LinearLayout(activity)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(
            SvgHelper.dp(activity, 24),
            SvgHelper.dp(activity, 24),
            SvgHelper.dp(activity, 24),
            SvgHelper.dp(activity, 24)
        )

        val bg = GradientDrawable()
        bg.setColor(SvgHelper.parseColor(e.bg))
        bg.cornerRadius = SvgHelper.dp(activity, 16).toFloat()
        col.background = bg

        // عنوان
        if (e.t.isNotBlank()) {
            val tv = TextView(activity)
            tv.text = e.t
            tv.textSize = e.tSize.toFloat()
            tv.setTextColor(SvgHelper.parseColor(e.txt))
            tv.gravity = Gravity.CENTER
            tv.typeface = loadFont(activity)
            col.addView(tv)
        }

        // پیام
        if (e.m.isNotBlank()) {
            val tv = TextView(activity)
            tv.text = e.m
            tv.textSize = e.mSize.toFloat()
            tv.setTextColor(SvgHelper.parseColor(e.txt))
            tv.gravity = Gravity.CENTER
            tv.typeface = loadFont(activity)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = SvgHelper.dp(activity, 8)
            tv.layoutParams = lp
            col.addView(tv)
        }

        // دکمه‌ها
        val row = LinearLayout(activity)
        row.orientation = if (e.btnLayout.lowercase() == "column") {
            LinearLayout.VERTICAL
        } else {
            LinearLayout.HORIZONTAL
        }
        row.gravity = Gravity.CENTER
        val rowLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowLp.topMargin = SvgHelper.dp(activity, 16)
        row.layoutParams = rowLp

        val btnYes = makeButton(activity, e.yes, e.yesBg, e.yesTxt, e.btnSize, e.btnShape)
        val btnNo = makeButton(activity, e.no, e.noBg, e.noTxt, e.btnSize, e.btnShape)

        if (e.btnLayout.lowercase() == "column") {
            row.addView(btnYes)
            row.addView(btnNo)
        } else {
            val lpYes = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            val lpNo = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            lpNo.marginStart = SvgHelper.dp(activity, 8)
            btnYes.layoutParams = lpYes
            btnNo.layoutParams = lpNo
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
            onCancel()
        }

        dialog.setContentView(col)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun makeButton(
        activity: Activity,
        text: String,
        bgColor: String,
        textColor: String,
        size: Int,
        shape: String
    ): Button {
        val btn = Button(activity)
        btn.text = text
        btn.textSize = size.toFloat()
        btn.setTextColor(SvgHelper.parseColor(textColor))
        btn.isAllCaps = false
        btn.typeface = loadFont(activity)

        val bg = GradientDrawable()
        bg.setColor(SvgHelper.parseColor(bgColor))
        when (shape.lowercase()) {
            "circle" -> bg.cornerRadius = 999f
            "rounded" -> bg.cornerRadius = SvgHelper.dp(activity, 8).toFloat()
            "square" -> bg.cornerRadius = 0f
            else -> bg.cornerRadius = SvgHelper.dp(activity, 8).toFloat()
        }
        btn.background = bg
        return btn
    }

    private fun loadFont(activity: Activity): Typeface? {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.lalezar)
        } catch (e: Exception) {
            null
        }
    }
}
