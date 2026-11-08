package com.stepan.minifactory

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class GameView(c: Context, private val engine: GameEngine) : View(c) {
    private val renderer = GameRenderer(engine)
    private var running = false
    private var last = 0L

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dt = if (last == 0L) 0.016f else ((frameTimeNanos - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = frameTimeNanos
            engine.update(dt)
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val scaleDetector = ScaleGestureDetector(c, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            engine.camera.zoomBy(detector.scaleFactor)
            return true
        }
    })

    private val gestureDetector = GestureDetector(c, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (!scaleDetector.isInProgress && e2.pointerCount == 1) {
                engine.camera.pan(distanceX, distanceY)
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            engine.onTap(e.x, e.y)
            return true
        }
    })

    fun start() {
        if (running) return
        running = true
        last = 0L
        Choreographer.getInstance().postFrameCallback(frame)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        engine.camera.viewW = w.toFloat()
        engine.camera.viewH = h.toFloat()
        engine.camera.fit()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas, width, height)
    }
}
