package com.stepan.minifactory

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var engine: GameEngine
    private lateinit var gameView: GameView
    private lateinit var host: ScreenHost
    private lateinit var hud: HudView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        engine = GameEngine(this)
        gameView = GameView(this, engine)
        host = ScreenHost(this)
        hud = HudView(this, engine, host)

        val root = FrameLayout(this)
        root.setBackgroundColor(Pal.BG)
        root.fitsSystemWindows = true
        root.addView(gameView, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(hud, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(host, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)

        engine.ui = hud
        if (engine.freshStart) {
            engine.say("Добро пожаловать! Линия уже работает — загляните на склад 📦")
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.start()
    }

    override fun onPause() {
        engine.saveNow()
        gameView.stop()
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            host.isShowing -> host.hide()
            engine.mode != Mode.IDLE -> engine.cancelMode()
            engine.selected != null -> engine.deselect()
            else -> super.onBackPressed()
        }
    }
}
