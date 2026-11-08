package com.stepan.minifactory

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color

abstract class GameScreen(protected val ctx: Context, protected val engine: GameEngine) {
    abstract val title: String
    val scroll = ScrollView(ctx)
    protected val content = LinearLayout(ctx)
    private var lastSig: String? = null

    init {
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(ctx.dp(12), ctx.dp(4), ctx.dp(12), ctx.dp(24))
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(content, FrameLayout.LayoutParams(MATCH, WRAP))
    }

    protected abstract fun render()

    protected open fun signature(): String = ""

    fun update(force: Boolean = false) {
        val sig = signature()
        if (!force && sig == lastSig) return
        lastSig = sig
        val sy = scroll.scrollY
        content.removeAllViews()
        render()
        scroll.post { scroll.scrollTo(0, sy) }
    }

    protected fun refreshNow() {
        update(true)
    }
}

class ScreenHost(private val c: Context) : FrameLayout(c) {
    private val titleView = c.label("", 18f, Pal.TEXT, true)
    private val body = FrameLayout(c)
    var current: GameScreen? = null

    val isShowing: Boolean get() = visibility == VISIBLE

    init {
        setBackgroundColor(Color.argb(190, 4, 8, 20))
        visibility = GONE
        isClickable = true

        val panel = LinearLayout(c)
        panel.orientation = LinearLayout.VERTICAL
        panel.background = rounded(Pal.PANEL, c.dp(20).toFloat(), Pal.EDGE, c.dp(2))

        val head = c.row()
        head.setPadding(c.dp(16), c.dp(10), c.dp(10), c.dp(6))
        head.addView(titleView, LinearLayout.LayoutParams(0, WRAP, 1f))
        val close = c.pillButton("✕", Pal.PANEL2, true, 16f) { hide() }
        head.addView(close, LinearLayout.LayoutParams(c.dp(48), c.dp(44)))
        panel.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))
        panel.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val lp = LayoutParams(MATCH, MATCH)
        lp.setMargins(c.dp(10), c.dp(10), c.dp(10), c.dp(10))
        addView(panel, lp)
    }

    fun show(s: GameScreen) {
        animate().cancel()
        current = s
        titleView.text = s.title
        body.removeAllViews()
        (s.scroll.parent as? ViewGroup)?.removeView(s.scroll)
        body.addView(s.scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        s.update(true)
        alpha = 0f
        visibility = VISIBLE
        animate().alpha(1f).setDuration(180).start()
    }

    fun hide() {
        if (visibility != VISIBLE) return
        animate().cancel()
        animate().alpha(0f).setDuration(150).withEndAction {
            visibility = GONE
            current = null
            body.removeAllViews()
        }.start()
    }
}
