package com.stepan.minifactory

import android.graphics.Color

class Particle(
    var wx: Float, var wy: Float, var z: Float,
    var vx: Float, var vy: Float, var vz: Float,
    var life: Float, val maxLife: Float,
    val color: Int, val size: Float, val kind: Int
)

class Effects {
    val list = ArrayList<Particle>()
    private val rnd = java.util.Random()
    private fun r(): Float = rnd.nextFloat()

    fun smoke(wx: Float, wy: Float, z: Float, size: Float = 0.12f) {
        if (list.size > 100) return
        list.add(
            Particle(
                wx + (r() - 0.5f) * 0.1f, wy + (r() - 0.5f) * 0.1f, z,
                (r() - 0.5f) * 0.08f + 0.03f, (r() - 0.5f) * 0.08f, 0.45f + r() * 0.2f,
                2.0f, 2.0f, Color.rgb(235, 238, 245), size, 0
            )
        )
    }

    fun burst(wx: Float, wy: Float, z: Float, color: Int, n: Int) {
        for (i in 0 until n) {
            if (list.size > 100) return
            list.add(
                Particle(
                    wx, wy, z,
                    (r() - 0.5f) * 1.2f, (r() - 0.5f) * 1.2f, 0.8f + r() * 0.8f,
                    0.5f, 0.5f, color, 0.05f, 1
                )
            )
        }
    }

    fun update(dt: Float, state: GameState) {
        if (state.effectsOn) {
            for (m in state.machines) {
                if (!m.working) continue
                if ((m.anim * 3f).toInt() == ((m.anim - dt) * 3f).toInt()) continue
                when (m.type) {
                    MachineType.SMELTER -> smoke(m.x + 0.72f, m.y + 0.26f, 1.7f)
                    MachineType.REFINERY -> smoke(m.x + 0.77f, m.y + 0.67f, 1.5f)
                    MachineType.POWER_PLANT -> {
                        smoke(m.x + 0.30f, m.y + 0.21f, 1.25f, 0.16f)
                        if (((m.anim * 3f).toInt() and 1) == 0) smoke(m.x + 0.74f, m.y + 0.21f, 0.95f, 0.12f)
                    }
                    else -> {}
                }
            }
        } else if (list.isNotEmpty()) {
            list.clear()
        }
        var i = 0
        while (i < list.size) {
            val p = list[i]
            p.life -= dt
            if (p.life <= 0f) {
                list.removeAt(i)
            } else {
                p.wx += p.vx * dt
                p.wy += p.vy * dt
                p.z += p.vz * dt
                if (p.kind == 1) p.vz -= 2.5f * dt
                i++
            }
        }
    }
}
