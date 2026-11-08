package com.stepan.minifactory

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(private val engine: GameEngine) {
    private val cam = engine.camera
    private lateinit var cv: Canvas
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val bg = Paint()
    private var sky: Shader? = null
    private var lastW = 0
    private var lastH = 0
    private val path = Path()
    private val tmp = FloatArray(2)
    private val qa = FloatArray(8)
    private val tmp2 = FloatArray(2)
    private var t = 0f
    private val sorted = ArrayList<Machine>()
    private val depthCmp = Comparator<Machine> { a, b ->
        val d = (a.x + a.y) - (b.x + b.y)
        if (d != 0) d else a.x - b.x
    }

    private val outlineColor = Color.argb(110, 8, 14, 30)
    private val tilesA = intArrayOf(Color.rgb(50, 66, 102), Color.rgb(80, 80, 88), Color.rgb(34, 86, 98), Color.rgb(74, 58, 114), Color.rgb(96, 64, 56))
    private val tilesB = intArrayOf(Color.rgb(56, 74, 112), Color.rgb(88, 88, 96), Color.rgb(40, 96, 108), Color.rgb(82, 66, 124), Color.rgb(106, 72, 62))
    private val wallColors = intArrayOf(Color.rgb(62, 80, 122), Color.rgb(116, 94, 80), Color.rgb(40, 112, 132), Color.rgb(104, 74, 152), Color.rgb(150, 70, 60))
    private val slabColors = intArrayOf(Color.rgb(34, 46, 76), Color.rgb(50, 46, 48), Color.rgb(20, 52, 64), Color.rgb(40, 30, 72), Color.rgb(56, 32, 28))

    // ---------- примитивы ----------

    private fun shade(c: Int, f: Float): Int {
        val r = (Color.red(c) * f).toInt().coerceIn(0, 255)
        val g = (Color.green(c) * f).toInt().coerceIn(0, 255)
        val b = (Color.blue(c) * f).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun q(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        ex: Float, ey: Float, ez: Float,
        color: Int, outline: Boolean = true
    ) {
        cam.project(ax, ay, az, tmp); qa[0] = tmp[0]; qa[1] = tmp[1]
        cam.project(bx, by, bz, tmp); qa[2] = tmp[0]; qa[3] = tmp[1]
        cam.project(cx, cy, cz, tmp); qa[4] = tmp[0]; qa[5] = tmp[1]
        cam.project(ex, ey, ez, tmp); qa[6] = tmp[0]; qa[7] = tmp[1]
        path.reset()
        path.moveTo(qa[0], qa[1])
        path.lineTo(qa[2], qa[3])
        path.lineTo(qa[4], qa[5])
        path.lineTo(qa[6], qa[7])
        path.close()
        fill.color = color
        cv.drawPath(path, fill)
        if (outline) {
            line.color = outlineColor
            line.strokeWidth = 1.4f
            cv.drawPath(path, line)
        }
    }

    private fun box(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, color: Int) {
        q(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, shade(color, 0.92f))
        q(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, shade(color, 0.70f))
        q(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, shade(color, 1.12f))
    }

    private fun tri(
        ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, z: Float, color: Int
    ) {
        cam.project(ax, ay, z, tmp); path.reset(); path.moveTo(tmp[0], tmp[1])
        cam.project(bx, by, z, tmp); path.lineTo(tmp[0], tmp[1])
        cam.project(cx, cy, z, tmp); path.lineTo(tmp[0], tmp[1])
        path.close()
        fill.color = color
        cv.drawPath(path, fill)
    }

    private fun outlineTile(x: Int, y: Int, z: Float, color: Int, w: Float) {
        val fx = x.toFloat()
        val fy = y.toFloat()
        cam.project(fx, fy, z, tmp); path.reset(); path.moveTo(tmp[0], tmp[1])
        cam.project(fx + 1f, fy, z, tmp); path.lineTo(tmp[0], tmp[1])
        cam.project(fx + 1f, fy + 1f, z, tmp); path.lineTo(tmp[0], tmp[1])
        cam.project(fx, fy + 1f, z, tmp); path.lineTo(tmp[0], tmp[1])
        path.close()
        line.color = color
        line.strokeWidth = w
        cv.drawPath(path, line)
    }

    private fun seg(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, color: Int, w: Float) {
        cam.project(ax, ay, az, tmp)
        cam.project(bx, by, bz, tmp2)
        line.color = color
        line.strokeWidth = w
        cv.drawLine(tmp[0], tmp[1], tmp2[0], tmp2[1], line)
    }

    private fun disc(wx: Float, wy: Float, z: Float, r: Float, color: Int) {
        cam.project(wx, wy, z, tmp)
        fill.color = color
        cv.drawCircle(tmp[0], tmp[1], r, fill)
    }

    // ---------- главный проход ----------

    fun draw(c: Canvas, w: Int, h: Int) {
        cv = c
        t = engine.time
        val st = engine.state
        if (w != lastW || h != lastH) {
            sky = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.rgb(16, 36, 84), Color.rgb(6, 12, 30), Shader.TileMode.CLAMP
            )
            lastW = w
            lastH = h
        }
        bg.shader = sky
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val n = st.gridSize
        drawGround(st, n)
        drawWalls(n.toFloat(), st.currentFactory.coerceIn(0, 4))

        val sel = engine.selected
        if (sel != null) {
            val pulse = 0.5f + 0.5f * sin(t * 5f)
            q(
                sel.x.toFloat(), sel.y.toFloat(), 0.02f,
                sel.x + 1f, sel.y.toFloat(), 0.02f,
                sel.x + 1f, sel.y + 1f, 0.02f,
                sel.x.toFloat(), sel.y + 1f, 0.02f,
                Color.argb((60 + 60 * pulse).toInt(), 60, 200, 255), false
            )
            outlineTile(sel.x, sel.y, 0.03f, Color.rgb(90, 225, 255), 3f * cam.zoom.coerceAtLeast(0.8f))
        }

        sorted.clear()
        sorted.addAll(st.machines)
        sorted.sortWith(depthCmp)

        for (m in sorted) {
            if (m.type != MachineType.CONVEYOR) drawShadow(m)
        }
        val blink = ((t * 3f).toInt() and 1) == 0
        for (m in sorted) {
            when (m.type) {
                MachineType.EXTRACTOR -> drawExtractor(m)
                MachineType.SMELTER -> drawSmelter(m)
                MachineType.PART_MAKER -> drawPartMaker(m)
                MachineType.STORAGE -> drawStorage(m, st)
                MachineType.CONVEYOR -> drawConveyor(m as Conveyor)
                MachineType.MINE -> drawMine(m)
                MachineType.REFINERY -> drawRefinery(m)
                MachineType.ASSEMBLY -> drawAssembly(m)
                MachineType.POWER_PLANT -> drawPowerPlant(m)
                MachineType.RESEARCH_CENTER -> drawResearch(m)
                MachineType.LOGISTICS -> drawLogistics(m)
            }
            if (m.recipe != null) drawPort(m)
            if (m.type.power > 0 && st.powerFactor < 0.95f && blink) {
                disc(m.x + 0.5f, m.y + 0.5f, 2.1f, 6f * cam.zoom, Color.rgb(255, 70, 70))
            }
        }

        if (sel != null) drawDirArrow(sel)
        drawParticles()
    }

    private fun drawGround(st: GameState, n: Int) {
        val nf = n.toFloat()
        val fid = st.currentFactory.coerceIn(0, 4)
        box(-0.35f, -0.35f, -0.55f, nf + 0.35f, nf + 0.35f, 0f, slabColors[fid])
        val building = engine.mode != Mode.IDLE
        val terr = st.territorySize()
        val vw = cam.viewW
        val vh = cam.viewH
        val padX = Iso.HW * cam.zoom * 1.2f
        val padY = Iso.HH * cam.zoom * 1.5f
        for (y in 0 until n) {
            for (x in 0 until n) {
                cam.project(x + 0.5f, y + 0.5f, 0f, tmp)
                if (tmp[0] < -padX || tmp[0] > vw + padX || tmp[1] < -padY || tmp[1] > vh + padY) continue
                val locked = x >= terr || y >= terr
                var col = if ((x + y) % 2 == 0) tilesA[fid] else tilesB[fid]
                if (locked) col = shade(col, 0.5f)
                val fx = x.toFloat()
                val fy = y.toFloat()
                q(fx, fy, 0f, fx + 1f, fy, 0f, fx + 1f, fy + 1f, 0f, fx, fy + 1f, 0f, col, true)
                if (locked) {
                    q(fx, fy, 0.01f, fx + 1f, fy, 0.01f, fx + 1f, fy + 1f, 0.01f, fx, fy + 1f, 0.01f,
                        Color.argb(46, 255, 90, 60), false)
                } else if (building && st.machineAt(x, y) == null) {
                    q(fx, fy, 0.01f, fx + 1f, fy, 0.01f, fx + 1f, fy + 1f, 0.01f, fx, fy + 1f, 0.01f,
                        Color.argb(48, 60, 255, 140), false)
                }
            }
        }
        if (terr < n) drawFence(terr)
    }

    private fun drawFence(t: Int) {
        val tf = t.toFloat()
        val post = Color.rgb(255, 176, 40)
        val rail = Color.rgb(240, 240, 245)
        for (i in 0 until t) {
            val c = i + 0.5f
            box(tf - 0.05f, c - 0.05f, 0f, tf + 0.05f, c + 0.05f, 0.45f, post)
            box(c - 0.05f, tf - 0.05f, 0f, c + 0.05f, tf + 0.05f, 0.45f, post)
        }
        box(tf - 0.03f, 0f, 0.30f, tf + 0.03f, tf, 0.36f, rail)
        box(0f, tf - 0.03f, 0.30f, tf, tf + 0.03f, 0.36f, rail)
    }

    private fun drawWalls(n: Float, fid: Int) {
        val wallC = wallColors[fid]
        box(-0.3f, -0.3f, 0f, n, 0f, 1.7f, wallC)
        box(-0.3f, 0f, 0f, 0f, n, 1.7f, shade(wallC, 0.95f))
        val orange = Color.rgb(255, 150, 40)
        q(-0.3f, 0f, 0.3f, n, 0f, 0.3f, n, 0f, 0.45f, -0.3f, 0f, 0.45f, orange, false)
        q(0f, 0f, 0.3f, 0f, n, 0.3f, 0f, n, 0.45f, 0f, 0f, 0.45f, shade(orange, 0.8f), false)
        val win = Color.rgb(255, 200, 96)
        var k = 1f
        while (k < n - 1f) {
            q(k, 0f, 0.85f, k + 1.6f, 0f, 0.85f, k + 1.6f, 0f, 1.4f, k, 0f, 1.4f, win)
            q(0f, k, 0.85f, 0f, k + 1.6f, 0.85f, 0f, k + 1.6f, 1.4f, 0f, k, 1.4f, shade(win, 0.82f))
            k += 3f
        }
    }

    private fun drawShadow(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        q(x + 0.02f, y + 0.02f, 0.005f, x + 1.18f, y + 0.02f, 0.005f, x + 1.18f, y + 1.18f, 0.005f,
            x + 0.02f, y + 1.18f, 0.005f, Color.argb(34, 0, 0, 0), false)
        q(x + 0.08f, y + 0.08f, 0.006f, x + 1.08f, y + 0.08f, 0.006f, x + 1.08f, y + 1.08f, 0.006f,
            x + 0.08f, y + 1.08f, 0.006f, Color.argb(34, 0, 0, 0), false)
    }

    private fun drawPort(m: Machine) {
        val cx = m.x + 0.5f + DX[m.dir] * 0.44f
        val cy = m.y + 0.5f + DY[m.dir] * 0.44f
        box(cx - 0.09f, cy - 0.09f, 0.12f, cx + 0.09f, cy + 0.09f, 0.3f, Color.rgb(255, 176, 32))
    }

    // ---------- станки ----------

    private fun drawExtractor(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        box(x + 0.08f, y + 0.08f, 0f, x + 0.92f, y + 0.92f, 0.38f, Color.rgb(74, 90, 122))
        box(x + 0.14f, y + 0.14f, 0.38f, x + 0.86f, y + 0.86f, 0.46f, Color.rgb(255, 150, 40))
        box(x + 0.30f, y + 0.30f, 0.46f, x + 0.70f, y + 0.70f, 1.15f, Color.rgb(236, 136, 36))
        val bob = if (m.working) sin(t * 10f + m.id) * 0.07f else 0f
        box(x + 0.44f, y + 0.44f, 1.15f, x + 0.56f, y + 0.56f, 1.45f + bob, Color.rgb(160, 170, 190))
        box(x + 0.36f, y + 0.36f, 1.45f + bob, x + 0.64f, y + 0.64f, 1.6f + bob, Color.rgb(44, 52, 74))
        // кучка руды
        box(x + 0.16f, y + 0.70f, 0.46f, x + 0.28f, y + 0.82f, 0.58f, ItemType.ORE.color)
        if (m.outCount > 1) box(x + 0.30f, y + 0.74f, 0.46f, x + 0.40f, y + 0.84f, 0.56f, ItemType.ORE.color)
    }

    private fun drawSmelter(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        box(x + 0.08f, y + 0.08f, 0f, x + 0.92f, y + 0.92f, 0.8f, Color.rgb(126, 72, 66))
        box(x + 0.08f, y + 0.08f, 0.8f, x + 0.92f, y + 0.92f, 0.95f, Color.rgb(70, 76, 96))
        box(x + 0.60f, y + 0.14f, 0.95f, x + 0.84f, y + 0.38f, 1.65f, Color.rgb(206, 62, 54))
        box(x + 0.585f, y + 0.125f, 1.25f, x + 0.855f, y + 0.395f, 1.38f, Color.rgb(242, 242, 247))
        box(x + 0.575f, y + 0.115f, 1.65f, x + 0.865f, y + 0.405f, 1.74f, Color.rgb(60, 64, 82))
        val glow = if (m.working) {
            val p = 0.5f + 0.5f * sin(t * 8f + m.id)
            Color.rgb(255, (150 + 70 * p).toInt(), 40)
        } else Color.rgb(92, 52, 44)
        q(x + 0.22f, y + 0.923f, 0.22f, x + 0.68f, y + 0.923f, 0.22f, x + 0.68f, y + 0.923f, 0.55f, x + 0.22f, y + 0.923f, 0.55f, glow)
        q(x + 0.923f, y + 0.22f, 0.22f, x + 0.923f, y + 0.68f, 0.22f, x + 0.923f, y + 0.68f, 0.55f, x + 0.923f, y + 0.22f, 0.55f, shade(glow, 0.85f))
    }

    private fun drawPartMaker(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        box(x + 0.08f, y + 0.08f, 0f, x + 0.92f, y + 0.92f, 0.5f, Color.rgb(46, 96, 168))
        box(x + 0.14f, y + 0.14f, 0.5f, x + 0.86f, y + 0.86f, 0.58f, Color.rgb(60, 70, 95))
        box(x + 0.40f, y + 0.40f, 0.58f, x + 0.60f, y + 0.60f, 0.78f, Color.rgb(40, 44, 60))
        if (m.working) box(x + 0.18f, y + 0.62f, 0.58f, x + 0.32f, y + 0.76f, 0.72f, ItemType.PART.color)

        val cx = x + 0.5f
        val cy = y + 0.5f
        val fx = DX[m.dir].toFloat()
        val fy = DY[m.dir].toFloat()
        val ph = if (m.working) sin(t * 3f + m.id) else 0f
        val sz = 0.85f
        val ex = cx + fx * (0.04f + 0.10f * ph)
        val ey = cy + fy * (0.04f + 0.10f * ph)
        val ez = 1.38f
        val hx = cx + fx * (0.30f + 0.12f * ph)
        val hy = cy + fy * (0.30f + 0.12f * ph)
        val hz = 0.88f + 0.10f * ph
        val z = cam.zoom
        val dark = Color.rgb(30, 34, 50)
        val yellow = Color.rgb(255, 194, 26)
        seg(cx, cy, sz, ex, ey, ez, dark, 11f * z)
        seg(ex, ey, ez, hx, hy, hz, dark, 11f * z)
        seg(cx, cy, sz, ex, ey, ez, yellow, 7.5f * z)
        seg(ex, ey, ez, hx, hy, hz, yellow, 7.5f * z)
        disc(cx, cy, sz, 6f * z, dark)
        disc(ex, ey, ez, 6f * z, dark)
        disc(ex, ey, ez, 3f * z, yellow)
        // захват
        val grip = if (m.working) 4f + 2f * (0.5f + 0.5f * ph) else 5f
        disc(hx, hy, hz - 0.04f, grip * z, dark)
    }

    private fun drawStorage(m: Machine, st: GameState) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        box(x + 0.06f, y + 0.06f, 0f, x + 0.94f, y + 0.94f, 0.85f, Color.rgb(212, 128, 52))
        box(x + 0.02f, y + 0.02f, 0.85f, x + 0.98f, y + 0.98f, 1.0f, Color.rgb(64, 78, 108))
        q(x + 0.2f, y + 0.943f, 0f, x + 0.8f, y + 0.943f, 0f, x + 0.8f, y + 0.943f, 0.6f, x + 0.2f, y + 0.943f, 0.6f, Color.rgb(78, 54, 40))
        for (i in 1..3) {
            val zz = 0.15f * i
            q(x + 0.2f, y + 0.945f, zz, x + 0.8f, y + 0.945f, zz, x + 0.8f, y + 0.945f, zz + 0.02f, x + 0.2f, y + 0.945f, zz + 0.02f, Color.rgb(50, 36, 30), false)
        }
        val cap = st.storageCapacity(m)
        val fillR = if (cap > 0) m.storedTotal().toFloat() / cap else 0f
        val crates = (fillR * 3.99f).toInt()
        val px = floatArrayOf(0.14f, 0.52f, 0.30f)
        val py = floatArrayOf(0.16f, 0.22f, 0.54f)
        for (i in 0 until crates) {
            box(x + px[i], y + py[i], 1.0f, x + px[i] + 0.26f, y + py[i] + 0.26f, 1.26f, Color.rgb(224, 170, 90))
        }
        val full = m.storedTotal() >= cap
        box(x + 0.80f, y + 0.12f, 1.0f, x + 0.90f, y + 0.22f, 1.1f, if (full) Color.rgb(255, 80, 80) else Color.rgb(80, 230, 130))
    }

    // ---------- новые здания ----------

    private fun drawMine(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        val z = cam.zoom
        box(x + 0.06f, y + 0.06f, 0f, x + 0.94f, y + 0.94f, 0.2f, Color.rgb(96, 88, 82))
        box(x + 0.12f, y + 0.14f, 0.2f, x + 0.60f, y + 0.62f, 0.58f, Color.rgb(132, 102, 72))
        box(x + 0.20f, y + 0.22f, 0.58f, x + 0.50f, y + 0.52f, 0.78f, Color.rgb(112, 88, 64))
        box(x + 0.30f, y + 0.32f, 0.78f, x + 0.42f, y + 0.44f, 0.86f, ItemType.ORE.color)
        val hx = x + 0.66f
        val hy = y + 0.12f
        val wood = Color.rgb(156, 108, 58)
        box(hx, hy, 0.2f, hx + 0.07f, hy + 0.07f, 1.45f, wood)
        box(hx + 0.2f, hy, 0.2f, hx + 0.27f, hy + 0.07f, 1.45f, wood)
        box(hx, hy + 0.2f, 0.2f, hx + 0.07f, hy + 0.27f, 1.45f, wood)
        box(hx + 0.2f, hy + 0.2f, 0.2f, hx + 0.27f, hy + 0.27f, 1.45f, wood)
        box(hx - 0.02f, hy - 0.02f, 1.45f, hx + 0.29f, hy + 0.29f, 1.58f, Color.rgb(52, 60, 82))
        val wx = hx + 0.135f
        val wy = hy + 0.135f
        val ang = if (m.working) t * 5f else 0.6f
        for (k in 0 until 3) {
            val a = ang + k * 1.0472f
            seg(wx - cos(a) * 0.14f, wy - sin(a) * 0.14f, 1.8f, wx + cos(a) * 0.14f, wy + sin(a) * 0.14f, 1.8f, Color.rgb(230, 230, 240), 4f * z)
        }
        disc(wx, wy, 1.8f, 5f * z, Color.rgb(255, 190, 40))
        box(wx - 0.02f, wy - 0.02f, 1.58f, wx + 0.02f, wy + 0.02f, 1.8f, Color.rgb(52, 60, 82))
        val cp = if (m.working) 0.5f + 0.5f * sin(t * 2f + m.id) else 0.2f
        val cx0 = x + 0.12f + 0.3f * cp
        box(cx0, y + 0.72f, 0.2f, cx0 + 0.24f, y + 0.88f, 0.4f, Color.rgb(70, 82, 106))
        box(cx0 + 0.04f, y + 0.74f, 0.4f, cx0 + 0.20f, y + 0.86f, 0.48f, ItemType.ORE.color)
    }

    private fun drawRefinery(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        val silver = Color.rgb(196, 202, 214)
        val red = Color.rgb(214, 66, 58)
        box(x + 0.10f, y + 0.08f, 0f, x + 0.40f, y + 0.38f, 1.05f, silver)
        box(x + 0.09f, y + 0.07f, 0.50f, x + 0.41f, y + 0.39f, 0.62f, red)
        box(x + 0.50f, y + 0.08f, 0f, x + 0.80f, y + 0.38f, 0.80f, shade(silver, 0.94f))
        box(x + 0.49f, y + 0.07f, 0.38f, x + 0.81f, y + 0.39f, 0.48f, red)
        box(x + 0.40f, y + 0.18f, 0.45f, x + 0.50f, y + 0.27f, 0.55f, Color.rgb(150, 160, 170))
        box(x + 0.08f, y + 0.42f, 0f, x + 0.92f, y + 0.92f, 0.62f, Color.rgb(96, 116, 128))
        box(x + 0.06f, y + 0.40f, 0.62f, x + 0.94f, y + 0.94f, 0.72f, Color.rgb(60, 72, 90))
        box(x + 0.70f, y + 0.58f, 0.72f, x + 0.84f, y + 0.72f, 1.5f, red)
        box(x + 0.69f, y + 0.57f, 1.1f, x + 0.85f, y + 0.73f, 1.2f, Color.rgb(242, 242, 247))
        val p = 0.5f + 0.5f * sin(t * 6f + m.id)
        val glow = if (m.working) Color.rgb(70, (190 + 50 * p).toInt(), 160) else Color.rgb(40, 70, 70)
        q(x + 0.2f, y + 0.923f, 0.16f, x + 0.8f, y + 0.923f, 0.16f, x + 0.8f, y + 0.923f, 0.42f, x + 0.2f, y + 0.923f, 0.42f, glow)
        disc(x + 0.25f, y + 0.23f, 1.05f, 4f * cam.zoom, if (m.working) Color.rgb(255, 90, 90) else Color.rgb(90, 40, 40))
    }

    private fun drawAssembly(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        box(x + 0.06f, y + 0.10f, 0f, x + 0.94f, y + 0.92f, 0.72f, Color.rgb(70, 112, 176))
        box(x + 0.06f, y + 0.10f, 0.72f, x + 0.94f, y + 0.92f, 0.80f, Color.rgb(48, 60, 90))
        box(x + 0.10f, y + 0.14f, 0.80f, x + 0.90f, y + 0.34f, 1.0f, Color.rgb(206, 212, 226))
        box(x + 0.10f, y + 0.48f, 0.80f, x + 0.90f, y + 0.68f, 1.0f, Color.rgb(206, 212, 226))
        q(x + 0.06f, y + 0.923f, 0.34f, x + 0.94f, y + 0.923f, 0.34f, x + 0.94f, y + 0.923f, 0.46f, x + 0.06f, y + 0.923f, 0.46f, Color.rgb(255, 150, 40), false)
        val gl = if (m.working) Color.rgb(255, 210, 80) else Color.rgb(150, 110, 50)
        q(x + 0.28f, y + 0.925f, 0.02f, x + 0.72f, y + 0.925f, 0.02f, x + 0.72f, y + 0.925f, 0.32f, x + 0.28f, y + 0.925f, 0.32f, gl)
        val gp = if (m.working) 0.5f + 0.5f * sin(t * 2.5f + m.id) else 0.5f
        val gx = x + 0.2f + 0.5f * gp
        val dark = Color.rgb(40, 44, 60)
        box(gx, y + 0.38f, 1.0f, gx + 0.14f, y + 0.46f, 1.1f, Color.rgb(255, 190, 30))
        seg(gx + 0.07f, y + 0.42f, 1.05f, gx + 0.07f, y + 0.42f, 0.86f, dark, 3f * cam.zoom)
        disc(gx + 0.07f, y + 0.42f, 0.86f, 4f * cam.zoom, dark)
        disc(x + 0.88f, y + 0.20f, 1.05f, 3.5f * cam.zoom, if (m.working) Color.rgb(90, 240, 140) else Color.rgb(120, 120, 130))
    }

    private fun drawPowerPlant(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        val con = Color.rgb(196, 200, 210)
        for (i in 0 until 4) {
            val ins = 0.025f * i
            box(x + 0.08f + ins, y + 0.04f + ins, i * 0.3f, x + 0.52f - ins, y + 0.40f - ins, (i + 1) * 0.3f, shade(con, 1f - 0.03f * i))
        }
        for (i in 0 until 3) {
            val ins = 0.025f * i
            box(x + 0.58f + ins, y + 0.06f + ins, i * 0.3f, x + 0.94f - ins, y + 0.40f - ins, (i + 1) * 0.3f, shade(con, 0.93f))
        }
        box(x + 0.06f, y + 0.44f, 0f, x + 0.94f, y + 0.94f, 0.5f, Color.rgb(88, 98, 120))
        box(x + 0.06f, y + 0.44f, 0.5f, x + 0.94f, y + 0.94f, 0.58f, Color.rgb(54, 62, 84))
        val p = 0.5f + 0.5f * sin(t * 5f + m.id)
        val core = if (m.working) Color.rgb(255, (210 + 40 * p).toInt(), 60) else Color.rgb(120, 110, 60)
        q(x + 0.30f, y + 0.943f, 0.10f, x + 0.70f, y + 0.943f, 0.10f, x + 0.70f, y + 0.943f, 0.36f, x + 0.30f, y + 0.943f, 0.36f, core)
        val bars = if (m.working) 3 else 1
        for (k in 0 until 3) {
            val col = if (k < bars) Color.rgb(90, 235, 140) else Color.rgb(60, 70, 80)
            box(x + 0.12f + k * 0.08f, y + 0.50f, 0.58f, x + 0.17f + k * 0.08f, y + 0.56f, 0.70f, col)
        }
    }

    private fun drawResearch(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        box(x + 0.10f, y + 0.16f, 0f, x + 0.90f, y + 0.90f, 0.7f, Color.rgb(214, 224, 238))
        box(x + 0.10f, y + 0.16f, 0.7f, x + 0.90f, y + 0.90f, 0.78f, Color.rgb(60, 80, 130))
        box(x + 0.30f, y + 0.30f, 0.78f, x + 0.70f, y + 0.70f, 1.45f, Color.rgb(96, 176, 234))
        box(x + 0.29f, y + 0.29f, 1.0f, x + 0.71f, y + 0.71f, 1.06f, Color.rgb(230, 240, 255))
        box(x + 0.47f, y + 0.47f, 1.45f, x + 0.53f, y + 0.53f, 1.75f, Color.rgb(200, 205, 220))
        val a = if (m.working) t * 1.6f + m.id else 0.6f
        val cx = x + 0.5f
        val cy = y + 0.5f
        seg(cx - cos(a) * 0.22f, cy - sin(a) * 0.22f, 1.8f, cx + cos(a) * 0.22f, cy + sin(a) * 0.22f, 1.8f, Color.rgb(245, 245, 252), 6f * cam.zoom)
        val pulse = 0.5f + 0.5f * sin(t * 4f)
        disc(cx, cy, 1.8f, 4f * cam.zoom, if (m.working) Color.rgb(90, (200 + 55 * pulse).toInt(), 255) else Color.rgb(100, 120, 140))
        val wc = if (m.working) Color.rgb(140, 230, 255) else Color.rgb(90, 120, 150)
        q(x + 0.2f, y + 0.903f, 0.18f, x + 0.8f, y + 0.903f, 0.18f, x + 0.8f, y + 0.903f, 0.5f, x + 0.2f, y + 0.903f, 0.5f, wc)
    }

    private fun drawLogistics(m: Machine) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        val z = cam.zoom
        val dark = Color.rgb(40, 44, 60)
        box(x + 0.06f, y + 0.06f, 0f, x + 0.94f, y + 0.60f, 0.8f, Color.rgb(232, 170, 50))
        box(x + 0.04f, y + 0.04f, 0.8f, x + 0.96f, y + 0.62f, 0.92f, Color.rgb(104, 76, 150))
        box(x + 0.04f, y + 0.60f, 0f, x + 0.96f, y + 0.96f, 0.05f, Color.rgb(66, 72, 92))
        q(x + 0.56f, y + 0.603f, 0.05f, x + 0.86f, y + 0.603f, 0.05f, x + 0.86f, y + 0.603f, 0.5f, x + 0.56f, y + 0.603f, 0.5f, Color.rgb(70, 50, 38))
        val tp = if (m.working) 0.5f + 0.5f * sin(t * 1.8f + m.id) else 0f
        val tx = x + 0.10f + 0.40f * tp
        box(tx, y + 0.68f, 0.05f, tx + 0.28f, y + 0.86f, 0.32f, Color.rgb(244, 244, 250))
        box(tx + 0.28f, y + 0.70f, 0.05f, tx + 0.40f, y + 0.86f, 0.24f, Color.rgb(224, 64, 64))
        disc(tx + 0.07f, y + 0.86f, 0.05f, 4f * z, dark)
        disc(tx + 0.33f, y + 0.86f, 0.05f, 4f * z, dark)
        disc(x + 0.90f, y + 0.10f, 0.95f, 3.5f * z, if (m.working) Color.rgb(90, 240, 140) else Color.rgb(120, 120, 130))
    }

    // ---------- конвейеры (прямые и угловые) ----------

    private val railColor = Color.rgb(96, 108, 134)
    private val beltColor = Color.rgb(40, 44, 58)
    private val railBuf = ArrayList<FloatArray>()

    private fun sgn(k: Int): Int = if (k == 0 || k == 1) 1 else -1

    private fun bandLo(s: Int): Float = if (s > 0) 0.80f else 0.06f

    private fun feeds(n: Machine?, towardUsDir: Int): Boolean =
        n != null && (n.type == MachineType.CONVEYOR || n.recipe != null) && n.dir == towardUsDir

    /** Направление движения предметов на входе в ленту. Если оно не совпадает с направлением выхода — это поворот. */
    private fun beltEntry(m: Conveyor): Int {
        val st = engine.state
        val back = (m.dir + 2) % 4
        if (feeds(st.machineAt(m.x + DX[back], m.y + DY[back]), m.dir)) return m.dir
        for (k in 0 until 4) {
            if (k == back || k == m.dir) continue
            val n = st.machineAt(m.x + DX[k], m.y + DY[k])
            if (feeds(n, (k + 2) % 4)) return (k + 2) % 4
        }
        return m.dir
    }

    private fun addRail(horiz: Boolean, a0: Float, a1: Float, p0: Float, p1: Float) {
        railBuf.add(if (horiz) floatArrayOf(a0, p0, a1, p1) else floatArrayOf(p0, a0, p1, a1))
    }

    private fun drawConveyor(m: Conveyor) {
        val x = m.x.toFloat()
        val y = m.y.toFloat()
        val e = beltEntry(m)
        if (e == m.dir) drawStraightBelt(m, x, y) else drawCornerBelt(m, x, y, e)

        // стрелка направления выхода
        val cx = x + 0.5f
        val cy = y + 0.5f
        val fx = DX[m.dir].toFloat()
        val fy = DY[m.dir].toFloat()
        val px = -fy
        val py = fx
        tri(
            cx + fx * 0.24f, cy + fy * 0.24f,
            cx - fx * 0.12f + px * 0.16f, cy - fy * 0.12f + py * 0.16f,
            cx - fx * 0.12f - px * 0.16f, cy - fy * 0.12f - py * 0.16f,
            0.13f, Color.argb(200, 255, 190, 40)
        )

        val items = m.items
        if (items.isEmpty()) return
        // порядок отрисовки: сначала те, что дальше от зрителя
        if (m.dir == 0 || m.dir == 1) {
            for (i in items.indices.reversed()) drawBeltItem(m, items[i])
        } else {
            for (i in items.indices) drawBeltItem(m, items[i])
        }
    }

    private fun drawStraightBelt(m: Conveyor, x: Float, y: Float) {
        val horizontal = m.dir % 2 == 0
        if (horizontal) {
            box(x, y + 0.06f, 0f, x + 1f, y + 0.20f, 0.2f, railColor)
            box(x, y + 0.20f, 0f, x + 1f, y + 0.80f, 0.12f, beltColor)
        } else {
            box(x + 0.06f, y, 0f, x + 0.20f, y + 1f, 0.2f, railColor)
            box(x + 0.20f, y, 0f, x + 0.80f, y + 1f, 0.12f, beltColor)
        }
        val off = (t * engine.state.conveyorSpeed()) % 0.25f
        val stripe = Color.rgb(70, 80, 104)
        for (k in 0 until 4) {
            val u = k * 0.25f + off
            val pos = if (m.dir == 0 || m.dir == 1) u else 1f - u
            if (horizontal) {
                seg(x + pos, y + 0.22f, 0.125f, x + pos, y + 0.78f, 0.125f, stripe, 2.5f * cam.zoom)
            } else {
                seg(x + 0.22f, y + pos, 0.125f, x + 0.78f, y + pos, 0.125f, stripe, 2.5f * cam.zoom)
            }
        }
        if (horizontal) {
            box(x, y + 0.80f, 0f, x + 1f, y + 0.94f, 0.2f, railColor)
            box(x + 0.45f, y + 0.84f, 0.2f, x + 0.55f, y + 0.90f, 0.24f, Color.rgb(40, 170, 255))
        } else {
            box(x + 0.80f, y, 0f, x + 0.94f, y + 1f, 0.2f, railColor)
            box(x + 0.84f, y + 0.45f, 0.2f, x + 0.90f, y + 0.55f, 0.24f, Color.rgb(40, 170, 255))
        }
    }

    private fun topFill(x0: Float, y0: Float, x1: Float, y1: Float) {
        q(x0, y0, 0.12f, x1, y0, 0.12f, x1, y1, 0.12f, x0, y1, 0.12f, shade(beltColor, 1.12f), false)
    }

    /** Угловая секция: предметы входят с боковой стороны (направление e) и выходят вперёд (направление m.dir). */
    private fun drawCornerBelt(m: Conveyor, x: Float, y: Float, e: Int) {
        val d = m.dir
        val b = (e + 2) % 4
        val bH = b % 2 == 0
        val dH = d % 2 == 0
        val sb = sgn(b)
        val sd = sgn(d)

        // борта
        railBuf.clear()
        val bEdge = if (sb < 0) 0f else 0.8f
        addRail(bH, bEdge, bEdge + 0.2f, bandLo(sd), bandLo(sd) + 0.14f)
        val ob = bandLo(-sd)
        addRail(bH, if (sb < 0) 0f else 0.06f, if (sb < 0) 0.94f else 1f, ob, ob + 0.14f)
        val dEdge = if (sd < 0) 0f else 0.8f
        addRail(dH, dEdge, dEdge + 0.2f, bandLo(sb), bandLo(sb) + 0.14f)
        val od = bandLo(-sb)
        addRail(dH, if (sd < 0) 0f else 0.06f, if (sd < 0) 0.94f else 1f, od, od + 0.14f)

        for (r in railBuf) {
            if ((r[0] + r[2] + r[1] + r[3]) * 0.5f < 1f) box(x + r[0], y + r[1], 0f, x + r[2], y + r[3], 0.2f, railColor)
        }

        // полотно: входная половина, выходная половина и центр
        val ba0 = if (sb < 0) 0f else 0.5f
        val da0 = if (sd < 0) 0f else 0.5f
        val rx = FloatArray(12)
        // вход
        if (bH) { rx[0] = ba0; rx[1] = 0.2f; rx[2] = ba0 + 0.5f; rx[3] = 0.8f }
        else { rx[0] = 0.2f; rx[1] = ba0; rx[2] = 0.8f; rx[3] = ba0 + 0.5f }
        // выход
        if (dH) { rx[4] = da0; rx[5] = 0.2f; rx[6] = da0 + 0.5f; rx[7] = 0.8f }
        else { rx[4] = 0.2f; rx[5] = da0; rx[6] = 0.8f; rx[7] = da0 + 0.5f }
        // центр
        rx[8] = 0.2f; rx[9] = 0.2f; rx[10] = 0.8f; rx[11] = 0.8f
        for (i in 0 until 3) box(x + rx[i * 4], y + rx[i * 4 + 1], 0f, x + rx[i * 4 + 2], y + rx[i * 4 + 3], 0.12f, beltColor)
        for (i in 0 until 3) topFill(x + rx[i * 4], y + rx[i * 4 + 1], x + rx[i * 4 + 2], y + rx[i * 4 + 3])

        // бегущие полосы
        val off = (t * engine.state.conveyorSpeed()) % 0.25f
        val stripe = Color.rgb(70, 80, 104)
        for (k in 0 until 4) {
            val p = k * 0.25f + off
            if (p < 0.5f) {
                val a = if (sgn(e) > 0) p else 1f - p
                if (bH) seg(x + a, y + 0.22f, 0.125f, x + a, y + 0.78f, 0.125f, stripe, 2.5f * cam.zoom)
                else seg(x + 0.22f, y + a, 0.125f, x + 0.78f, y + a, 0.125f, stripe, 2.5f * cam.zoom)
            } else {
                val a = if (sgn(d) > 0) p else 1f - p
                if (dH) seg(x + a, y + 0.22f, 0.125f, x + a, y + 0.78f, 0.125f, stripe, 2.5f * cam.zoom)
                else seg(x + 0.22f, y + a, 0.125f, x + 0.78f, y + a, 0.125f, stripe, 2.5f * cam.zoom)
            }
        }

        for (r in railBuf) {
            if ((r[0] + r[2] + r[1] + r[3]) * 0.5f >= 1f) box(x + r[0], y + r[1], 0f, x + r[2], y + r[3], 0.2f, railColor)
        }
        // индикатор поворота на внешнем борту выходной половины
        val lo = bandLo(-sb) + 0.04f
        val along = if (sd < 0) 0.2f else 0.7f
        if (dH) box(x + along, y + lo, 0.2f, x + along + 0.1f, y + lo + 0.06f, 0.24f, Color.rgb(40, 170, 255))
        else box(x + lo, y + along, 0.2f, x + lo + 0.06f, y + along + 0.1f, 0.24f, Color.rgb(40, 170, 255))
    }

    private fun drawBeltItem(m: Conveyor, bi: BeltItem) {
        val cx = m.x + 0.5f
        val cy = m.y + 0.5f
        val p0x = cx - DX[bi.inDir] * 0.5f
        val p0y = cy - DY[bi.inDir] * 0.5f
        val p2x = cx + DX[m.dir] * 0.5f
        val p2y = cy + DY[m.dir] * 0.5f
        val k = bi.pos
        val u = 1f - k
        val px = u * u * p0x + 2f * u * k * cx + k * k * p2x
        val py = u * u * p0y + 2f * u * k * cy + k * k * p2y
        val s = 0.1f
        box(px - s, py - s, 0.12f, px + s, py + s, 0.32f, bi.type.color)
    }

    private fun drawDirArrow(m: Machine) {
        val bob = sin(t * 6f) * 0.04f
        val cx = m.x + 0.5f
        val cy = m.y + 0.5f
        val fx = DX[m.dir].toFloat()
        val fy = DY[m.dir].toFloat()
        val px = -fy
        val py = fx
        tri(
            cx + fx * (1.0f + bob), cy + fy * (1.0f + bob),
            cx + fx * 0.62f + px * 0.2f, cy + fy * 0.62f + py * 0.2f,
            cx + fx * 0.62f - px * 0.2f, cy + fy * 0.62f - py * 0.2f,
            0.06f, Color.rgb(90, 225, 255)
        )
    }

    private fun drawParticles() {
        val list = engine.effects.list
        for (p in list) {
            cam.project(p.wx, p.wy, p.z, tmp)
            val k = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.kind == 0) {
                fill.color = Color.argb((k * 120).toInt().coerceIn(0, 255), 236, 240, 248)
                cv.drawCircle(tmp[0], tmp[1], p.size * Iso.HW * cam.zoom * (1.2f + (1f - k) * 2.2f), fill)
            } else {
                fill.color = Color.argb((k * 255).toInt().coerceIn(0, 255), Color.red(p.color), Color.green(p.color), Color.blue(p.color))
                cv.drawCircle(tmp[0], tmp[1], 2.6f * cam.zoom + 0.5f, fill)
            }
        }
    }
}
