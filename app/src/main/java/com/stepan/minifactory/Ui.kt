package com.stepan.minifactory

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

const val MATCH = -1
const val WRAP = -2

object Pal {
    val BG = Color.rgb(8, 16, 36)
    val PANEL = Color.rgb(16, 32, 66)
    val PANEL2 = Color.rgb(30, 56, 112)
    val EDGE = Color.rgb(70, 120, 210)
    val ACCENT = Color.rgb(255, 170, 30)
    val ACCENT2 = Color.rgb(40, 190, 255)
    val GOOD = Color.rgb(60, 205, 100)
    val BAD = Color.rgb(255, 84, 84)
    val PURPLE = Color.rgb(150, 100, 255)
    val BLUE = Color.rgb(46, 140, 255)
    val SLATE = Color.rgb(100, 125, 170)
    val TEXT = Color.rgb(240, 246, 255)
    val MUTED = Color.rgb(160, 182, 222)
    val DISABLED = Color.rgb(62, 72, 98)
}

fun shadeColor(c: Int, f: Float): Int {
    val r = (Color.red(c) * f).toInt().coerceIn(0, 255)
    val g = (Color.green(c) * f).toInt().coerceIn(0, 255)
    val b = (Color.blue(c) * f).toInt().coerceIn(0, 255)
    return Color.rgb(r, g, b)
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun money(n: Long): String = "$" + String.format(Locale.US, "%,d", n)

fun rounded(color: Int, radius: Float, stroke: Int = 0, strokeW: Int = 0): GradientDrawable {
    val g = GradientDrawable()
    g.shape = GradientDrawable.RECTANGLE
    g.cornerRadius = radius
    g.setColor(color)
    if (strokeW > 0) g.setStroke(strokeW, stroke)
    return g
}

/** Яркая мультяшная «объёмная» кнопка: градиент, обводка и тёмный нижний бортик. */
fun cartoonBg(c: Context, color: Int, radiusDp: Int = 14, depthDp: Int = 3): Drawable {
    val r = c.dp(radiusDp).toFloat()
    val bottom = GradientDrawable()
    bottom.shape = GradientDrawable.RECTANGLE
    bottom.cornerRadius = r
    bottom.setColor(shadeColor(color, 0.5f))
    val top = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(shadeColor(color, 1.35f), color)
    )
    top.shape = GradientDrawable.RECTANGLE
    top.cornerRadius = r
    top.setStroke(c.dp(2), shadeColor(color, 0.58f))
    val ld = LayerDrawable(arrayOf<Drawable>(bottom, top))
    ld.setLayerInset(1, 0, 0, 0, c.dp(depthDp))
    return ld
}

fun Context.label(text: String, sp: Float = 14f, color: Int = Pal.TEXT, bold: Boolean = false): TextView {
    val t = TextView(this)
    t.text = text
    t.textSize = sp
    t.setTextColor(color)
    if (bold) {
        t.setTypeface(t.typeface, Typeface.BOLD)
        t.setShadowLayer(2f, 0f, 1.5f, Color.argb(150, 0, 0, 0))
    }
    return t
}

fun Context.pillButton(
    text: String,
    color: Int,
    enabled: Boolean = true,
    sp: Float = 14f,
    onClick: () -> Unit
): TextView {
    val col = if (enabled) color else Pal.DISABLED
    val b = label(text, sp, if (enabled) Color.WHITE else Pal.MUTED, true)
    b.gravity = Gravity.CENTER
    b.setPadding(dp(14), dp(10), dp(14), dp(13))
    b.background = cartoonBg(this, col, 14, 3)
    if (enabled) {
        b.setShadowLayer(3f, 0f, 2f, Color.argb(170, 0, 0, 0))
        b.elevation = dp(3).toFloat()
        b.setOnClickListener { onClick() }
        b.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }
            false
        }
    }
    return b
}

fun Context.card(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.VERTICAL
    l.setPadding(dp(14), dp(12), dp(14), dp(14))
    val g = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.rgb(42, 76, 146), Color.rgb(24, 46, 98))
    )
    g.shape = GradientDrawable.RECTANGLE
    g.cornerRadius = dp(18).toFloat()
    g.setStroke(dp(2), Pal.EDGE)
    l.background = g
    l.elevation = dp(5).toFloat()
    return l
}

fun Context.row(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.HORIZONTAL
    l.gravity = Gravity.CENTER_VERTICAL
    return l
}

fun Context.lp(top: Int = 0, w: Int = MATCH, h: Int = WRAP): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(w, h)
    p.topMargin = dp(top)
    return p
}

fun Context.weightLp(weight: Float = 1f): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, WRAP, weight)

fun Context.icon(): IconView = IconView(this)

/** Строка «иконка слева, справа заголовок, значение и пояснение». */
fun Context.infoRow(icon: View, title: String, right: String?, rightColor: Int, desc: String?): LinearLayout {
    val r = row()
    r.addView(icon, LinearLayout.LayoutParams(dp(60), dp(60)))
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.setPadding(dp(12), 0, 0, 0)
    val top = row()
    top.addView(label(title, 16f, Pal.TEXT, true), weightLp())
    if (right != null) top.addView(label(right, 16f, rightColor, true))
    col.addView(top)
    if (desc != null) col.addView(label(desc, 12.5f, Pal.MUTED), lp(3))
    r.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
    return r
}

fun Context.pips(level: Int, max: Int, color: Int): LinearLayout {
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    for (i in 0 until max) {
        val v = View(this)
        v.background = rounded(if (i < level) color else Color.rgb(16, 28, 60), dp(3).toFloat())
        val p = LinearLayout.LayoutParams(0, dp(8), 1f)
        p.setMargins(dp(1), 0, dp(1), 0)
        r.addView(v, p)
    }
    return r
}

fun Context.progress(ratio: Float, color: Int): LinearLayout {
    val r = LinearLayout(this)
    r.orientation = LinearLayout.HORIZONTAL
    r.background = rounded(Color.rgb(12, 22, 50), dp(7).toFloat(), Pal.EDGE, dp(1))
    r.setPadding(dp(2), dp(2), dp(2), dp(2))
    val k = ratio.coerceIn(0f, 1f)
    val f = View(this)
    f.background = rounded(color, dp(5).toFloat())
    r.addView(f, LinearLayout.LayoutParams(0, dp(10), k))
    r.addView(View(this), LinearLayout.LayoutParams(0, dp(10), 1f - k))
    return r
}

fun f1(v: Float): String = String.format(Locale.US, "%.1f", v)

fun f2(v: Float): String = String.format(Locale.US, "%.2f", v)

fun fmtTime(sec: Float): String {
    val s = maxOf(0, sec.toInt())
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Строка «подпись — значение» для окон характеристик. */
fun Context.kv(label: String, value: String, valueColor: Int = Pal.TEXT): LinearLayout {
    val r = row()
    r.addView(label(label, 13f, Pal.MUTED), weightLp())
    r.addView(label(value, 13f, valueColor, true))
    return r
}

fun usd(v: Float): String = "$" + f2(v)
