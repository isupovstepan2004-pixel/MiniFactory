package com.stepan.minifactory

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class HudView(
    private val c: Context,
    private val engine: GameEngine,
    private val host: ScreenHost
) : FrameLayout(c), UiListener {

    private val shop = ShopScreen(c, engine) { host.hide() }
    private val production = ProductionScreen(c, engine)
    private val staffScreen = StaffScreen(c, engine)
    private val warehouse = WarehouseScreen(c, engine)
    private val research = ResearchScreen(c, engine)
    private val orders = OrdersScreen(c, engine)
    private val career = CareerScreen(c, engine)
    private val settings = SettingsScreen(c, engine) { host.hide() }
    private val building = BuildingScreen(c, engine) { host.hide() }

    private val moneyText = c.label("", 22f, Pal.ACCENT, true)
    private val profitText = c.label("", 12f, Pal.GOOD, true)
    private val powerText = c.label("", 12f, Pal.ACCENT2, true)
    private val rpText = c.label("", 12f, Color.rgb(200, 160, 255), true)
    private val factoryText = c.label("", 10.5f, Pal.MUTED, true)
    private val levelBadge = c.label("1", 18f, Color.WHITE, true)
    private val levelCaption = c.label("Уровень", 10f, Pal.MUTED, true)
    private val xpBar = ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal)
    private val toastView = c.label("", 14f, Color.WHITE, true)
    private val ordersBtn = c.label("📜\nЗаказы", 11f, Color.WHITE, true)

    private val ctxPanel = LinearLayout(c)
    private val ctxIcon = IconView(c)
    private val ctxTitle = c.label("", 15f, Pal.TEXT, true)
    private val ctxStatus = c.label("", 12f, Pal.MUTED)
    private val btnRotate = c.label("", 12f, Color.WHITE, true)
    private val btnMove = c.label("", 12f, Color.WHITE, true)
    private val btnUpgrade = c.label("", 12f, Color.WHITE, true)
    private val btnInfo = c.label("", 12f, Color.WHITE, true)
    private val btnSell = c.label("", 12f, Color.WHITE, true)
    private val btnClose = c.label("", 12f, Color.WHITE, true)
    private val buildBtns = HashMap<MachineType, LinearLayout>()
    private val buildLabels = HashMap<MachineType, TextView>()

    private val hideToast = Runnable {
        toastView.animate().alpha(0f).setStartDelay(0).setDuration(300).start()
    }
    private val ticker = object : Runnable {
        override fun run() {
            tick()
            postDelayed(this, 250)
        }
    }

    init {
        // ---------- верхняя панель ----------
        val top = LinearLayout(c)
        top.orientation = LinearLayout.VERTICAL
        top.setPadding(c.dp(8), c.dp(6), c.dp(8), 0)

        val bar = c.row()
        bar.setPadding(c.dp(12), c.dp(8), c.dp(12), c.dp(8))
        val barBg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(44, 80, 154), Color.rgb(22, 44, 96))
        )
        barBg.shape = GradientDrawable.RECTANGLE
        barBg.cornerRadius = c.dp(18).toFloat()
        barBg.setStroke(c.dp(2), Pal.EDGE)
        bar.background = barBg
        bar.elevation = c.dp(6).toFloat()

        bar.addView(c.label("🪙", 26f), LinearLayout.LayoutParams(WRAP, WRAP))
        val moneyBox = LinearLayout(c)
        moneyBox.orientation = LinearLayout.VERTICAL
        moneyBox.setPadding(c.dp(8), 0, 0, 0)
        moneyBox.addView(moneyText)
        val sub = c.row()
        sub.addView(profitText)
        sub.addView(powerText, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = c.dp(10) })
        sub.addView(rpText, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = c.dp(10) })
        moneyBox.addView(sub)
        moneyBox.addView(factoryText)
        bar.addView(moneyBox, LinearLayout.LayoutParams(0, WRAP, 1f))

        val lvlBox = LinearLayout(c)
        lvlBox.orientation = LinearLayout.VERTICAL
        lvlBox.gravity = Gravity.END
        lvlBox.addView(levelCaption)
        xpBar.max = 1000
        xpBar.progressTintList = ColorStateList.valueOf(Pal.ACCENT2)
        xpBar.progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(14, 26, 60))
        lvlBox.addView(xpBar, LinearLayout.LayoutParams(c.dp(70), c.dp(8)).apply { topMargin = c.dp(4) })
        bar.addView(lvlBox, LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = c.dp(8) })

        levelBadge.gravity = Gravity.CENTER
        val oval = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(255, 214, 76), Color.rgb(255, 136, 20))
        )
        oval.shape = GradientDrawable.OVAL
        oval.setStroke(c.dp(2), Color.rgb(130, 62, 0))
        levelBadge.background = oval
        levelBadge.elevation = c.dp(3).toFloat()
        bar.addView(levelBadge, LinearLayout.LayoutParams(c.dp(42), c.dp(42)))
        top.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP))

        val nav = LinearLayout(c)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.addView(navButton(c.label("🛒\nМагазин", 11f, Color.WHITE, true), shop, Color.rgb(255, 150, 40)), navLp())
        nav.addView(navButton(c.label("🏭\nЛинии", 11f, Color.WHITE, true), production, Pal.BLUE), navLp())
        nav.addView(navButton(c.label("👷\nПерсонал", 11f, Color.WHITE, true), staffScreen, Color.rgb(30, 190, 190)), navLp())
        nav.addView(navButton(c.label("📦\nСклад", 11f, Color.WHITE, true), warehouse, Color.rgb(90, 120, 230)), navLp())
        nav.addView(navButton(c.label("🔬\nНаука", 11f, Color.WHITE, true), research, Pal.PURPLE), navLp())
        nav.addView(navButton(ordersBtn, orders, Pal.GOOD), navLp())
        nav.addView(navButton(c.label("🏆\nКарьера", 11f, Color.WHITE, true), career, Color.rgb(240, 110, 160)), navLp())
        nav.addView(navButton(c.label("⚙\nНастр.", 11f, Color.WHITE, true), settings, Pal.SLATE), navLp())
        val navScroll = HorizontalScrollView(c)
        navScroll.isHorizontalScrollBarEnabled = false
        navScroll.clipChildren = false
        navScroll.addView(nav)
        top.addView(navScroll, c.lp(7))
        addView(top, LayoutParams(MATCH, WRAP, Gravity.TOP))

        // ---------- нижняя панель ----------
        val bottom = LinearLayout(c)
        bottom.orientation = LinearLayout.VERTICAL
        bottom.setPadding(c.dp(8), 0, c.dp(8), c.dp(8))

        ctxPanel.orientation = LinearLayout.VERTICAL
        ctxPanel.setPadding(c.dp(12), c.dp(10), c.dp(12), c.dp(12))
        val ctxBg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(40, 74, 144), Color.rgb(20, 40, 90))
        )
        ctxBg.shape = GradientDrawable.RECTANGLE
        ctxBg.cornerRadius = c.dp(18).toFloat()
        ctxBg.setStroke(c.dp(2), Pal.ACCENT2)
        ctxPanel.background = ctxBg
        ctxPanel.elevation = c.dp(6).toFloat()
        ctxPanel.visibility = GONE

        val head = c.row()
        head.addView(ctxIcon, LinearLayout.LayoutParams(c.dp(48), c.dp(48)))
        val headText = LinearLayout(c)
        headText.orientation = LinearLayout.VERTICAL
        headText.setPadding(c.dp(10), 0, 0, 0)
        headText.addView(ctxTitle)
        headText.addView(ctxStatus, c.lp(2))
        head.addView(headText, LinearLayout.LayoutParams(0, WRAP, 1f))
        ctxPanel.addView(head)

        val btnRow = LinearLayout(c)
        btnRow.orientation = LinearLayout.HORIZONTAL
        for (b in listOf(btnRotate, btnMove, btnUpgrade, btnInfo, btnSell, btnClose)) {
            b.gravity = Gravity.CENTER
            b.setPadding(c.dp(1), c.dp(10), c.dp(1), c.dp(13))
            val p = LinearLayout.LayoutParams(0, WRAP, 1f)
            p.setMargins(c.dp(2), 0, c.dp(2), 0)
            btnRow.addView(b, p)
        }
        ctxPanel.addView(btnRow, c.lp(10))
        bottom.addView(ctxPanel, c.lp(0))

        btnRotate.setOnClickListener {
            if (engine.mode == Mode.PLACE) engine.rotatePlacement() else engine.rotateSelected()
        }
        btnMove.setOnClickListener { engine.startMove() }
        btnUpgrade.setOnClickListener { engine.upgradeSelected() }
        btnInfo.setOnClickListener { host.show(building) }
        btnSell.setOnClickListener { engine.removeSelected() }
        btnClose.setOnClickListener {
            if (engine.mode != Mode.IDLE) engine.cancelMode() else engine.deselect()
        }

        val buildRow = LinearLayout(c)
        buildRow.orientation = LinearLayout.HORIZONTAL
        for (t in MachineType.values()) {
            val b = LinearLayout(c)
            b.orientation = LinearLayout.VERTICAL
            b.gravity = Gravity.CENTER_HORIZONTAL
            b.setPadding(c.dp(6), c.dp(8), c.dp(6), c.dp(11))
            b.elevation = c.dp(4).toFloat()
            val ic = IconView(c)
            ic.showMachine(t)
            b.addView(ic, LinearLayout.LayoutParams(c.dp(46), c.dp(46)))
            val lb = c.label("${t.title}\n${money(t.cost.toLong())}", 10.5f, Pal.TEXT, true)
            lb.gravity = Gravity.CENTER
            lb.maxLines = 3
            b.addView(lb, c.lp(4))
            b.setOnClickListener { engine.togglePlacing(t) }
            buildBtns[t] = b
            buildLabels[t] = lb
            val p = LinearLayout.LayoutParams(c.dp(104), WRAP)
            p.setMargins(c.dp(3), c.dp(2), c.dp(3), c.dp(4))
            buildRow.addView(b, p)
        }
        val hs = HorizontalScrollView(c)
        hs.isHorizontalScrollBarEnabled = false
        hs.clipChildren = false
        hs.addView(buildRow)
        hs.setPadding(0, c.dp(6), 0, 0)
        bottom.addView(hs, LinearLayout.LayoutParams(MATCH, WRAP))
        addView(bottom, LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        // ---------- сообщения ----------
        toastView.setPadding(c.dp(18), c.dp(9), c.dp(18), c.dp(12))
        toastView.background = cartoonBg(c, Color.rgb(48, 90, 170), 18, 3)
        toastView.elevation = c.dp(8).toFloat()
        toastView.alpha = 0f
        val tlp = LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        tlp.topMargin = c.dp(170)
        addView(toastView, tlp)
    }

    private fun navLp(): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(c.dp(72), WRAP)
        p.setMargins(c.dp(2), 0, c.dp(2), c.dp(2))
        return p
    }

    private fun navButton(b: TextView, screen: GameScreen, color: Int): TextView {
        b.gravity = Gravity.CENTER
        b.setPadding(0, c.dp(6), 0, c.dp(9))
        b.background = cartoonBg(c, color, 14, 3)
        b.elevation = c.dp(4).toFloat()
        b.setOnClickListener {
            engine.cancelMode()
            engine.deselect()
            host.show(screen)
        }
        return b
    }

    private fun setBtn(b: TextView, text: String, color: Int, enabled: Boolean) {
        if (b.text.toString() != text) b.text = text
        val col = if (enabled) color else Pal.DISABLED
        val key = col * 2 + (if (enabled) 1 else 0)
        if (b.tag != key) {
            b.tag = key
            b.background = cartoonBg(c, col, 12, 3)
            b.alpha = if (enabled) 1f else 0.6f
        }
    }

    // ---------- UiListener ----------

    override fun onMessage(text: String) {
        toastView.text = text
        removeCallbacks(hideToast)
        toastView.animate().cancel()
        toastView.animate().alpha(1f).setStartDelay(0).setDuration(150).start()
        postDelayed(hideToast, 2600)
    }

    override fun onStateChanged() {
        refreshContext()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private fun tick() {
        val s = engine.state
        val eco = engine.economy
        moneyText.text = money(s.money)
        val net = eco.netPerSec()
        profitText.text = (if (net >= 0f) "📈 +" else "📉 −") + usd(Math.abs(net)) + "/с"
        profitText.setTextColor(if (net >= 0f) Pal.GOOD else Pal.BAD)
        powerText.text = "⚡ ${s.powerDemand.toInt()}/${s.powerSupply.toInt()}"
        powerText.setTextColor(if (s.powerDemand > s.powerSupply + 0.01f) Pal.BAD else Pal.ACCENT2)
        rpText.text = "🔬 ${s.researchPoints.toInt()}"
        factoryText.text = "🏭 ${s.factoryDef().title}"
        levelBadge.text = "${s.level}"
        val need = eco.xpForNext(s.level)
        xpBar.progress = (1000f * s.xp / need).toInt().coerceIn(0, 1000)
        val rc = engine.orders.readyCount()
        val ot = if (rc > 0) "📜\nЗаказы ($rc)" else "📜\nЗаказы"
        if (ordersBtn.text.toString() != ot) ordersBtn.text = ot
        refreshContext()
        host.current?.update()
    }

    private fun statusText(m: Machine): String {
        val s = engine.state
        return when (m.type) {
            MachineType.CONVEYOR -> "Предметов на ленте: ${(m as Conveyor).items.size}"
            MachineType.STORAGE -> "Заполнено ${m.storedTotal()} / ${s.storageCapacity(m)}"
            MachineType.POWER_PLANT -> "Выработка ${f1(s.plantOutput(m))} МВт • нагрузка ${f1(s.powerUsed)}"
            MachineType.RESEARCH_CENTER -> if (m.working) "Исследует • ${f2(RP_RATE * m.level * s.powerFactor)} ОИ/с" else "Нет питания"
            MachineType.LOGISTICS -> "В очереди ${m.storedTotal()} • ${if (m.working) "рейс идёт" else "ожидание"}"
            else -> {
                val spd = (s.speedMult(m) * 100).toInt()
                if (m.working) "Работает • скорость $spd%"
                else if (m.type.power > 0 && s.powerFactor < 0.05f) "Нет питания — постройте электростанцию"
                else if (m.recipe != null && m.outCount >= m.recipe.outCap) "Выход заблокирован — подведите ленту"
                else "Ожидает ресурсы"
            }
        }
    }

    private fun refreshContext() {
        val s = engine.state
        val m = engine.selected
        val mode = engine.mode
        when {
            mode == Mode.PLACE -> {
                val t = engine.placeType
                ctxPanel.visibility = VISIBLE
                if (t != null) {
                    ctxIcon.showMachine(t)
                    ctxTitle.text = "Строим: ${t.title}"
                    ctxStatus.text = "Тап по пустой клетке • ${money(t.cost.toLong())} за штуку"
                }
                btnRotate.visibility = VISIBLE
                setBtn(btnRotate, "⟳ ${DIR_ARROWS[engine.placeDir]}", Pal.BLUE, true)
                btnMove.visibility = GONE
                btnUpgrade.visibility = GONE
                btnInfo.visibility = GONE
                btnSell.visibility = GONE
                setBtn(btnClose, "✕ Готово", Pal.BAD, true)
            }
            mode == Mode.MOVE -> {
                ctxPanel.visibility = VISIBLE
                if (m != null) ctxIcon.showMachine(m.type)
                ctxTitle.text = "Перемещение: ${m?.type?.title ?: ""}"
                ctxStatus.text = "Нажмите на свободную клетку"
                btnRotate.visibility = GONE
                btnMove.visibility = GONE
                btnUpgrade.visibility = GONE
                btnInfo.visibility = GONE
                btnSell.visibility = GONE
                setBtn(btnClose, "✕ Отмена", Pal.BAD, true)
            }
            m != null -> {
                ctxPanel.visibility = VISIBLE
                ctxIcon.showMachine(m.type)
                ctxTitle.text = "${m.type.title}  •  ур. ${m.level}"
                ctxStatus.text = statusText(m)
                btnRotate.visibility = VISIBLE
                btnMove.visibility = VISIBLE
                btnInfo.visibility = VISIBLE
                btnSell.visibility = VISIBLE
                setBtn(btnRotate, "⟳ ${DIR_ARROWS[m.dir]}", Pal.BLUE, true)
                setBtn(btnMove, "✥", Pal.PURPLE, true)
                setBtn(btnInfo, "ℹ Инфо", Pal.ACCENT2, true)
                if (engine.canUpgrade(m)) {
                    btnUpgrade.visibility = VISIBLE
                    val cost = engine.upgradeCost(m)
                    setBtn(btnUpgrade, "⬆ ${money(cost)}", Pal.GOOD, s.money >= cost)
                } else if (m.type == MachineType.CONVEYOR) {
                    btnUpgrade.visibility = GONE
                } else {
                    btnUpgrade.visibility = VISIBLE
                    setBtn(btnUpgrade, "MAX", Pal.DISABLED, false)
                }
                setBtn(btnSell, "🗑 +${money(m.type.refund.toLong())}", Pal.ACCENT, true)
                setBtn(btnClose, "✕", Pal.SLATE, true)
            }
            else -> ctxPanel.visibility = GONE
        }
        for ((t, b) in buildBtns) {
            val active = mode == Mode.PLACE && engine.placeType == t
            val locked = Unlocks.reason(t, s) != null
            val afford = s.money >= t.cost
            val col = if (active) Pal.ACCENT2 else if (locked) Color.rgb(60, 70, 100) else Color.rgb(46, 84, 160)
            val key = col * 8 + (if (active) 4 else 0) + (if (locked) 2 else 0) + (if (afford) 1 else 0)
            if (b.tag != key) {
                b.tag = key
                b.background = cartoonBg(c, col, 16, 3)
                b.alpha = if (locked) 0.55f else if (afford) 1f else 0.65f
                val lb = buildLabels[t]
                if (lb != null) {
                    lb.text = "${t.title}\n" + (if (locked) "🔒 закрыто" else money(t.cost.toLong()))
                    lb.setTextColor(if (active) Pal.BG else Pal.TEXT)
                }
            }
        }
    }
}
