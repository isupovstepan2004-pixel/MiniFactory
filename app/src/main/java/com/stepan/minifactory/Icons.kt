package com.stepan.minifactory

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/** Процедурно нарисованные мультяшные иконки станков, ресурсов и эмодзи-значков. */
class IconView(c: Context) : View(c) {
    private var mType: MachineType? = null
    private var iType: ItemType? = null
    private var emoji: String? = null

    private val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bgp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private var u = 1f
    private val outl = Color.rgb(14, 22, 44)

    fun showMachine(t: MachineType) {
        if (mType == t) return
        mType = t; iType = null; emoji = null
        invalidate()
    }

    fun showItem(t: ItemType) {
        if (iType == t) return
        mType = null; iType = t; emoji = null
        invalidate()
    }

    fun showEmoji(e: String) {
        if (emoji == e) return
        mType = null; iType = null; emoji = e
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgp.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(70, 118, 214), Color.rgb(26, 48, 104), Shader.TileMode.CLAMP)
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        u = minOf(w, h)
        if (u <= 0f) return
        rect.set(2f, 2f, w - 2f, h - 2f)
        cv.drawRoundRect(rect, u * 0.24f, u * 0.24f, bgp)
        sp.color = Color.rgb(140, 190, 255)
        sp.strokeWidth = u * 0.04f
        cv.drawRoundRect(rect, u * 0.24f, u * 0.24f, sp)
        fp.color = Color.argb(80, 0, 0, 0)
        rect.set(u * 0.18f, u * 0.80f, u * 0.82f, u * 0.93f)
        cv.drawOval(rect, fp)
        val m = mType
        val ity = iType
        val e = emoji
        if (m != null) drawMachine(cv, m)
        else if (ity != null) drawItem(cv, ity)
        else if (e != null) {
            tp.textSize = u * 0.55f
            cv.drawText(e, w / 2f, h / 2f + u * 0.2f, tp)
        }
    }

    // ---------- примитивы ----------

    private fun rr(cv: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, rad: Float = 0.06f) {
        rect.set(l * u, t * u, r * u, b * u)
        fp.color = color
        cv.drawRoundRect(rect, rad * u, rad * u, fp)
        // блик
        fp.color = Color.argb(55, 255, 255, 255)
        rect.set(l * u + 0.02f * u, t * u + 0.015f * u, r * u - 0.02f * u, t * u + (b - t) * 0.42f * u)
        cv.drawRoundRect(rect, rad * u * 0.6f, rad * u * 0.6f, fp)
        rect.set(l * u, t * u, r * u, b * u)
        sp.color = outl
        sp.strokeWidth = u * 0.03f
        cv.drawRoundRect(rect, rad * u, rad * u, sp)
    }

    private fun circ(cv: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        fp.color = color
        cv.drawCircle(cx * u, cy * u, r * u, fp)
        sp.color = outl
        sp.strokeWidth = u * 0.03f
        cv.drawCircle(cx * u, cy * u, r * u, sp)
    }

    private fun poly(cv: Canvas, color: Int, vararg p: Float) {
        path.reset()
        path.moveTo(p[0] * u, p[1] * u)
        var i = 2
        while (i < p.size) {
            path.lineTo(p[i] * u, p[i + 1] * u)
            i += 2
        }
        path.close()
        fp.color = color
        cv.drawPath(path, fp)
        sp.color = outl
        sp.strokeWidth = u * 0.03f
        cv.drawPath(path, sp)
    }

    private fun thick(cv: Canvas, ax: Float, ay: Float, bx: Float, by: Float, color: Int, w: Float) {
        sp.color = outl
        sp.strokeWidth = (w + 0.045f) * u
        cv.drawLine(ax * u, ay * u, bx * u, by * u, sp)
        sp.color = color
        sp.strokeWidth = w * u
        cv.drawLine(ax * u, ay * u, bx * u, by * u, sp)
    }

    // ---------- станки ----------

    private fun drawMachine(cv: Canvas, t: MachineType) {
        when (t) {
            MachineType.EXTRACTOR -> {
                rr(cv, 0.14f, 0.64f, 0.86f, 0.82f, Color.rgb(84, 102, 138))
                rr(cv, 0.28f, 0.30f, 0.72f, 0.66f, Color.rgb(244, 144, 40))
                rr(cv, 0.32f, 0.52f, 0.68f, 0.58f, Color.rgb(255, 218, 70), 0.02f)
                rr(cv, 0.45f, 0.12f, 0.55f, 0.32f, Color.rgb(176, 186, 206), 0.03f)
                rr(cv, 0.36f, 0.05f, 0.64f, 0.15f, Color.rgb(50, 58, 82))
                circ(cv, 0.22f, 0.62f, 0.06f, ItemType.ORE.color)
            }
            MachineType.SMELTER -> {
                rr(cv, 0.16f, 0.46f, 0.84f, 0.84f, Color.rgb(160, 90, 80))
                rr(cv, 0.12f, 0.38f, 0.88f, 0.48f, Color.rgb(76, 82, 104))
                rr(cv, 0.60f, 0.14f, 0.78f, 0.40f, Color.rgb(222, 66, 58), 0.03f)
                rr(cv, 0.60f, 0.22f, 0.78f, 0.28f, Color.rgb(246, 246, 250), 0.02f)
                rr(cv, 0.28f, 0.58f, 0.64f, 0.78f, Color.rgb(255, 190, 50), 0.05f)
                rr(cv, 0.34f, 0.62f, 0.58f, 0.74f, Color.rgb(255, 242, 150), 0.04f)
                fp.color = Color.argb(200, 240, 244, 250)
                cv.drawCircle(0.70f * u, 0.10f * u, 0.045f * u, fp)
                cv.drawCircle(0.78f * u, 0.05f * u, 0.035f * u, fp)
            }
            MachineType.PART_MAKER -> {
                rr(cv, 0.12f, 0.68f, 0.88f, 0.84f, Color.rgb(50, 104, 180))
                rr(cv, 0.40f, 0.56f, 0.60f, 0.70f, Color.rgb(46, 52, 72))
                thick(cv, 0.50f, 0.58f, 0.36f, 0.30f, Color.rgb(255, 196, 30), 0.07f)
                thick(cv, 0.36f, 0.30f, 0.66f, 0.42f, Color.rgb(255, 196, 30), 0.07f)
                circ(cv, 0.50f, 0.58f, 0.06f, Color.rgb(46, 52, 72))
                circ(cv, 0.36f, 0.30f, 0.06f, Color.rgb(46, 52, 72))
                circ(cv, 0.36f, 0.30f, 0.025f, Color.rgb(255, 196, 30))
                circ(cv, 0.67f, 0.43f, 0.05f, Color.rgb(46, 52, 72))
                rr(cv, 0.70f, 0.56f, 0.82f, 0.68f, ItemType.PART.color, 0.02f)
            }
            MachineType.CONVEYOR -> {
                rr(cv, 0.06f, 0.56f, 0.94f, 0.76f, Color.rgb(44, 48, 62), 0.09f)
                rr(cv, 0.06f, 0.52f, 0.94f, 0.58f, Color.rgb(118, 130, 158), 0.03f)
                rr(cv, 0.06f, 0.74f, 0.94f, 0.80f, Color.rgb(118, 130, 158), 0.03f)
                rr(cv, 0.20f, 0.80f, 0.26f, 0.88f, Color.rgb(60, 70, 96), 0.02f)
                rr(cv, 0.74f, 0.80f, 0.80f, 0.88f, Color.rgb(60, 70, 96), 0.02f)
                circ(cv, 0.12f, 0.66f, 0.07f, Color.rgb(150, 160, 184))
                circ(cv, 0.88f, 0.66f, 0.07f, Color.rgb(150, 160, 184))
                poly(cv, Color.rgb(255, 190, 40), 0.42f, 0.59f, 0.64f, 0.66f, 0.42f, 0.73f)
                rr(cv, 0.30f, 0.36f, 0.56f, 0.54f, Color.rgb(232, 172, 92), 0.03f)
            }
            MachineType.STORAGE -> {
                rr(cv, 0.14f, 0.36f, 0.86f, 0.84f, Color.rgb(220, 134, 54))
                rr(cv, 0.10f, 0.26f, 0.90f, 0.38f, Color.rgb(70, 86, 118))
                rr(cv, 0.34f, 0.52f, 0.66f, 0.84f, Color.rgb(84, 58, 42), 0.03f)
                rr(cv, 0.34f, 0.62f, 0.66f, 0.65f, Color.rgb(54, 38, 30), 0.01f)
                rr(cv, 0.34f, 0.72f, 0.66f, 0.75f, Color.rgb(54, 38, 30), 0.01f)
                rr(cv, 0.42f, 0.10f, 0.66f, 0.26f, Color.rgb(232, 178, 96), 0.03f)
                circ(cv, 0.78f, 0.48f, 0.04f, Color.rgb(90, 235, 140))
            }
            MachineType.MINE -> {
                rr(cv, 0.10f, 0.72f, 0.90f, 0.84f, Color.rgb(96, 88, 82))
                poly(cv, Color.rgb(140, 108, 76), 0.12f, 0.74f, 0.30f, 0.46f, 0.50f, 0.40f, 0.66f, 0.74f)
                thick(cv, 0.66f, 0.74f, 0.78f, 0.26f, Color.rgb(160, 112, 60), 0.05f)
                thick(cv, 0.90f, 0.74f, 0.78f, 0.26f, Color.rgb(160, 112, 60), 0.05f)
                thick(cv, 0.70f, 0.56f, 0.86f, 0.56f, Color.rgb(160, 112, 60), 0.04f)
                circ(cv, 0.78f, 0.22f, 0.09f, Color.rgb(50, 58, 80))
                circ(cv, 0.78f, 0.22f, 0.04f, Color.rgb(255, 196, 40))
                circ(cv, 0.26f, 0.66f, 0.05f, ItemType.ORE.color)
            }
            MachineType.REFINERY -> {
                rr(cv, 0.12f, 0.64f, 0.88f, 0.84f, Color.rgb(100, 120, 132))
                rr(cv, 0.12f, 0.26f, 0.38f, 0.66f, Color.rgb(196, 202, 212))
                rr(cv, 0.12f, 0.40f, 0.38f, 0.46f, Color.rgb(222, 70, 60), 0.02f)
                rr(cv, 0.44f, 0.38f, 0.66f, 0.66f, Color.rgb(196, 202, 212))
                rr(cv, 0.36f, 0.48f, 0.46f, 0.54f, Color.rgb(150, 160, 170), 0.02f)
                rr(cv, 0.72f, 0.20f, 0.86f, 0.66f, Color.rgb(222, 66, 58), 0.03f)
                rr(cv, 0.72f, 0.32f, 0.86f, 0.38f, Color.rgb(246, 246, 250), 0.02f)
                rr(cv, 0.30f, 0.70f, 0.62f, 0.80f, Color.rgb(90, 235, 180), 0.04f)
                fp.color = Color.argb(200, 240, 244, 250)
                cv.drawCircle(0.79f * u, 0.12f * u, 0.04f * u, fp)
                cv.drawCircle(0.85f * u, 0.06f * u, 0.03f * u, fp)
            }
            MachineType.ASSEMBLY -> {
                rr(cv, 0.10f, 0.44f, 0.90f, 0.84f, Color.rgb(70, 112, 176))
                poly(cv, Color.rgb(206, 212, 226), 0.10f, 0.44f, 0.28f, 0.24f, 0.48f, 0.44f)
                poly(cv, Color.rgb(206, 212, 226), 0.50f, 0.44f, 0.68f, 0.24f, 0.88f, 0.44f)
                rr(cv, 0.10f, 0.56f, 0.90f, 0.62f, Color.rgb(255, 150, 40), 0.02f)
                rr(cv, 0.36f, 0.64f, 0.64f, 0.84f, Color.rgb(255, 200, 70), 0.04f)
                circ(cv, 0.80f, 0.72f, 0.07f, Color.rgb(255, 196, 40))
                circ(cv, 0.80f, 0.72f, 0.025f, Color.rgb(40, 52, 90))
            }
            MachineType.POWER_PLANT -> {
                rr(cv, 0.10f, 0.68f, 0.90f, 0.84f, Color.rgb(88, 100, 124))
                poly(cv, Color.rgb(200, 204, 214), 0.12f, 0.70f, 0.20f, 0.26f, 0.40f, 0.26f, 0.48f, 0.70f)
                poly(cv, Color.rgb(176, 182, 196), 0.52f, 0.70f, 0.58f, 0.36f, 0.74f, 0.36f, 0.80f, 0.70f)
                poly(cv, Color.rgb(255, 224, 60), 0.44f, 0.34f, 0.34f, 0.56f, 0.42f, 0.56f, 0.38f, 0.78f, 0.56f, 0.50f, 0.47f, 0.50f, 0.54f, 0.34f)
                fp.color = Color.argb(190, 240, 244, 250)
                cv.drawCircle(0.30f * u, 0.18f * u, 0.04f * u, fp)
                cv.drawCircle(0.66f * u, 0.28f * u, 0.035f * u, fp)
            }
            MachineType.RESEARCH_CENTER -> {
                rr(cv, 0.12f, 0.60f, 0.88f, 0.84f, Color.rgb(214, 224, 238))
                rr(cv, 0.32f, 0.34f, 0.68f, 0.62f, Color.rgb(96, 176, 234))
                rr(cv, 0.38f, 0.40f, 0.62f, 0.50f, Color.rgb(190, 235, 255), 0.03f)
                thick(cv, 0.50f, 0.34f, 0.50f, 0.22f, Color.rgb(200, 205, 220), 0.04f)
                thick(cv, 0.36f, 0.22f, 0.64f, 0.14f, Color.rgb(240, 240, 250), 0.05f)
                circ(cv, 0.50f, 0.18f, 0.04f, Color.rgb(90, 240, 255))
                circ(cv, 0.24f, 0.72f, 0.05f, Color.rgb(90, 150, 230))
            }
            MachineType.LOGISTICS -> {
                rr(cv, 0.10f, 0.32f, 0.90f, 0.62f, Color.rgb(232, 170, 50))
                rr(cv, 0.08f, 0.22f, 0.92f, 0.34f, Color.rgb(104, 76, 150))
                rr(cv, 0.58f, 0.40f, 0.82f, 0.62f, Color.rgb(80, 56, 40), 0.03f)
                rr(cv, 0.12f, 0.62f, 0.50f, 0.80f, Color.rgb(244, 244, 250), 0.03f)
                rr(cv, 0.50f, 0.66f, 0.66f, 0.80f, Color.rgb(224, 64, 64), 0.03f)
                circ(cv, 0.24f, 0.82f, 0.06f, Color.rgb(40, 44, 60))
                circ(cv, 0.58f, 0.82f, 0.06f, Color.rgb(40, 44, 60))
            }
        }
    }

    // ---------- ресурсы ----------

    private fun drawItem(cv: Canvas, t: ItemType) {
        when (t) {
            ItemType.ORE -> {
                poly(cv, Color.rgb(150, 104, 66), 0.16f, 0.76f, 0.22f, 0.46f, 0.46f, 0.28f, 0.72f, 0.40f, 0.84f, 0.76f)
                path.reset()
                path.moveTo(0.28f * u, 0.50f * u)
                path.lineTo(0.46f * u, 0.35f * u)
                path.lineTo(0.60f * u, 0.44f * u)
                path.lineTo(0.40f * u, 0.58f * u)
                path.close()
                fp.color = Color.argb(110, 255, 220, 170)
                cv.drawPath(path, fp)
                circ(cv, 0.62f, 0.64f, 0.04f, Color.rgb(104, 70, 44))
                circ(cv, 0.34f, 0.68f, 0.035f, Color.rgb(104, 70, 44))
            }
            ItemType.METAL -> {
                poly(cv, Color.rgb(206, 220, 240), 0.18f, 0.50f, 0.30f, 0.34f, 0.94f, 0.34f, 0.82f, 0.50f)
                poly(cv, Color.rgb(168, 184, 210), 0.18f, 0.50f, 0.82f, 0.50f, 0.82f, 0.76f, 0.18f, 0.76f)
                poly(cv, Color.rgb(124, 140, 170), 0.82f, 0.50f, 0.94f, 0.34f, 0.94f, 0.60f, 0.82f, 0.76f)
                sp.color = Color.argb(200, 255, 255, 255)
                sp.strokeWidth = u * 0.03f
                cv.drawLine(0.26f * u, 0.57f * u, 0.50f * u, 0.57f * u, sp)
            }
            ItemType.PART -> {
                val cx = 0.5f
                val cy = 0.52f
                for (i in 0 until 8) {
                    cv.save()
                    cv.rotate(i * 45f, cx * u, cy * u)
                    rr(cv, cx - 0.065f, cy - 0.36f, cx + 0.065f, cy - 0.20f, Color.rgb(255, 196, 40), 0.02f)
                    cv.restore()
                }
                circ(cv, cx, cy, 0.26f, Color.rgb(255, 200, 50))
                fp.color = Color.argb(90, 255, 255, 255)
                cv.drawCircle((cx - 0.07f) * u, (cy - 0.08f) * u, 0.10f * u, fp)
                circ(cv, cx, cy, 0.10f, Color.rgb(34, 54, 110))
            }
            ItemType.GADGET -> {
                rr(cv, 0.22f, 0.26f, 0.78f, 0.80f, Color.rgb(64, 190, 236), 0.08f)
                rr(cv, 0.30f, 0.34f, 0.70f, 0.56f, Color.rgb(20, 40, 80), 0.04f)
                rr(cv, 0.34f, 0.38f, 0.50f, 0.46f, Color.rgb(120, 255, 200), 0.02f)
                circ(cv, 0.36f, 0.68f, 0.045f, Color.rgb(255, 196, 40))
                circ(cv, 0.50f, 0.68f, 0.045f, Color.rgb(255, 100, 100))
                circ(cv, 0.64f, 0.68f, 0.045f, Color.rgb(120, 240, 130))
                thick(cv, 0.50f, 0.26f, 0.50f, 0.14f, Color.rgb(200, 205, 220), 0.03f)
                circ(cv, 0.50f, 0.12f, 0.04f, Color.rgb(255, 90, 90))
            }
        }
    }
}
