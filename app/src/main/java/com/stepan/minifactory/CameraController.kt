package com.stepan.minifactory

object Iso {
    const val HW = 48f
    const val HH = 24f
    const val ZU = 46f
}

class CameraController {
    var camX = 0f
    var camY = 0f
    var zoom = 0.85f
    var viewW = 1080f
    var viewH = 1920f
    private var gridN = 12

    fun configure(n: Int) {
        gridN = n
        fit()
    }

    fun fit() {
        camX = 0f
        camY = gridN * Iso.HH - 10f
        val zw = viewW / (gridN * Iso.HW * 2f + 60f)
        val zh = viewH / (gridN * Iso.HH * 2f + 420f)
        zoom = minOf(zw, zh * 1.35f).coerceIn(0.4f, 2.2f)
    }

    fun project(wx: Float, wy: Float, z: Float, out: FloatArray) {
        val ix = (wx - wy) * Iso.HW
        val iy = (wx + wy) * Iso.HH - z * Iso.ZU
        out[0] = (ix - camX) * zoom + viewW / 2f
        out[1] = (iy - camY) * zoom + viewH / 2f
    }

    fun unproject(sx: Float, sy: Float, out: FloatArray) {
        val ix = (sx - viewW / 2f) / zoom + camX
        val iy = (sy - viewH / 2f) / zoom + camY
        val a = ix / Iso.HW
        val b = iy / Iso.HH
        out[0] = (a + b) / 2f
        out[1] = (b - a) / 2f
    }

    fun pan(dxPx: Float, dyPx: Float) {
        camX += dxPx / zoom
        camY += dyPx / zoom
        clamp()
    }

    fun zoomBy(f: Float) {
        zoom = (zoom * f).coerceIn(0.4f, 2.4f)
        clamp()
    }

    private fun clamp() {
        val ex = gridN * Iso.HW
        val ey = gridN * Iso.HH
        camX = camX.coerceIn(-ex, ex)
        camY = camY.coerceIn(gridN * Iso.HH - ey, gridN * Iso.HH + ey)
    }
}
