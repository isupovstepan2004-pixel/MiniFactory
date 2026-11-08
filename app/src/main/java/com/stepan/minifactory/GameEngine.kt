package com.stepan.minifactory

import android.content.Context
import android.graphics.Color
import kotlin.math.floor

class GameEngine(ctx: Context) {
    var state = GameState()
    val camera = CameraController()
    private val saver = SaveManager(ctx)
    val production = ProductionManager(this)
    val power = PowerManager(this)
    val economy = EconomyManager(this)
    val orders = OrderManager(this)
    val staff = StaffManager(this)
    val career = CareerManager(this)
    val effects = Effects()

    var time = 0f
    var mode = Mode.IDLE
    var placeType: MachineType? = null
    var placeDir = 0
    var selected: Machine? = null
    var moving: Machine? = null
    var ui: UiListener? = null
    var freshStart = false

    private var saveTimer = 0f
    private val tmp = FloatArray(2)
    private val tmp2 = FloatArray(2)

    init {
        production.listener = object : ProductionListener {
            override fun onProduced(m: Machine, item: ItemType) {
                if (!state.effectsOn) return
                val cx = m.x + 0.5f
                val cy = m.y + 0.5f
                when (m.type) {
                    MachineType.PART_MAKER -> effects.burst(cx, cy, 0.9f, Color.rgb(255, 210, 90), 5)
                    MachineType.EXTRACTOR -> effects.burst(cx, cy, 0.3f, Color.rgb(170, 130, 90), 3)
                    MachineType.SMELTER -> effects.burst(cx, cy, 0.9f, Color.rgb(255, 150, 50), 3)
                    MachineType.MINE -> effects.burst(cx, cy, 0.3f, Color.rgb(150, 120, 90), 4)
                    MachineType.REFINERY -> effects.burst(cx, cy, 0.8f, Color.rgb(120, 240, 190), 4)
                    MachineType.ASSEMBLY -> effects.burst(cx, cy, 1.0f, ItemType.GADGET.color, 6)
                    else -> {}
                }
            }
        }
        val loaded = saver.load()
        if (loaded != null) {
            state = loaded
            camera.configure(state.gridSize)
            orders.ensureOrders()
            power.recompute()
        } else {
            newGame()
            freshStart = true
        }
    }

    fun say(text: String) {
        ui?.onMessage(text)
    }

    fun saveNow() {
        saver.save(state)
    }

    // ---------- Новая игра ----------

    fun newGame() {
        state = GameState()
        state.money = 400L
        state.setGrid(Factories.all[0].gridSize)
        state.territory[0] = 10
        addStarterLine(5)
        orders.ensureOrders()
        power.recompute()
        camera.configure(state.gridSize)
        selected = null
        moving = null
        placeType = null
        mode = Mode.IDLE
        effects.list.clear()
        saveNow()
        ui?.onStateChanged()
    }

    fun resetGame() {
        saver.clear()
        newGame()
    }

    private fun addStarterLine(y: Int) {
        addStarter(MachineType.EXTRACTOR, 1, y, 0)
        addStarter(MachineType.CONVEYOR, 2, y, 0)
        addStarter(MachineType.CONVEYOR, 3, y, 0)
        addStarter(MachineType.SMELTER, 4, y, 0)
        addStarter(MachineType.CONVEYOR, 5, y, 0)
        addStarter(MachineType.CONVEYOR, 6, y, 0)
        addStarter(MachineType.PART_MAKER, 7, y, 0)
        addStarter(MachineType.CONVEYOR, 8, y, 0)
        addStarter(MachineType.STORAGE, 9, y, 0)
    }

    fun switchFactory(id: Int) {
        if (!state.unlockedFactories.contains(id)) return
        if (state.currentFactory == id) return
        val f = Factories.all[id]
        val first = !state.layouts.containsKey(id)
        state.switchTo(id, f.gridSize)
        if (first && state.machines.isEmpty()) {
            state.territory[id] = 10
            addStarterLine(5)
        }
        selected = null
        moving = null
        placeType = null
        mode = Mode.IDLE
        effects.list.clear()
        camera.configure(f.gridSize)
        power.recompute()
        saveNow()
        ui?.onStateChanged()
        say("🏭 ${f.title}")
    }

    private fun addStarter(type: MachineType, x: Int, y: Int, dir: Int) {
        state.addMachine(createMachine(state.nextMachineId++, type, x, y, dir))
    }

    // ---------- Цикл ----------

    fun update(dt: Float) {
        time += dt
        power.update(dt)
        production.update(dt)
        economy.update(dt)
        orders.update(dt)
        career.update(dt)
        effects.update(dt, state)
        saveTimer += dt
        if (saveTimer >= 10f) {
            saveTimer = 0f
            saveNow()
        }
    }

    // ---------- Ввод ----------

    fun onTap(sx: Float, sy: Float) {
        camera.unproject(sx, sy, tmp)
        val gx = floor(tmp[0]).toInt()
        val gy = floor(tmp[1]).toInt()
        when (mode) {
            Mode.PLACE -> tryPlace(gx, gy)
            Mode.MOVE -> {
                val m = moving ?: return
                if (!state.inBounds(gx, gy)) return
                if (!state.isBuildable(gx, gy)) {
                    say("Территория закрыта — расширьте её в «Карьере»")
                    return
                }
                val at = state.machineAt(gx, gy)
                if (at != null && at !== m) {
                    say("Клетка занята")
                    return
                }
                state.moveMachine(m, gx, gy)
                mode = Mode.IDLE
                moving = null
                selected = m
                ui?.onStateChanged()
            }
            Mode.IDLE -> {
                camera.unproject(sx, sy + 0.55f * Iso.ZU * camera.zoom, tmp2)
                val ex = floor(tmp2[0]).toInt()
                val ey = floor(tmp2[1]).toInt()
                var pick = state.machineAt(ex, ey)
                if (pick == null) pick = state.machineAt(gx, gy)
                selected = pick
                ui?.onStateChanged()
            }
        }
    }

    private fun tryPlace(tx: Int, ty: Int) {
        val type = placeType ?: return
        if (!state.inBounds(tx, ty)) return
        if (!state.isBuildable(tx, ty)) {
            say("Территория закрыта — расширьте её в «Карьере»")
            return
        }
        if (state.machineAt(tx, ty) != null) {
            say("Клетка занята")
            return
        }
        if (!economy.spend(type.cost.toLong())) {
            say("Не хватает денег: нужно ${money(type.cost.toLong())}")
            return
        }
        val m = createMachine(state.nextMachineId++, type, tx, ty, placeDir)
        state.addMachine(m)
        power.recompute()
        if (type.power > 0 && state.countOf(MachineType.POWER_PLANT) == 0) {
            say("⚡ Этому зданию нужна электростанция")
        }
        if (state.effectsOn) effects.burst(tx + 0.5f, ty + 0.5f, 0.3f, Color.rgb(255, 220, 120), 6)
        ui?.onStateChanged()
    }

    // ---------- Режимы ----------

    fun togglePlacing(type: MachineType) {
        if (mode == Mode.PLACE && placeType == type) {
            cancelMode()
        } else {
            startPlacing(type)
        }
    }

    fun startPlacing(type: MachineType) {
        val why = Unlocks.reason(type, state)
        if (why != null) {
            say("🔒 $why")
            return
        }
        mode = Mode.PLACE
        placeType = type
        selected = null
        moving = null
        ui?.onStateChanged()
    }

    fun cancelMode() {
        mode = Mode.IDLE
        placeType = null
        moving = null
        ui?.onStateChanged()
    }

    fun deselect() {
        selected = null
        ui?.onStateChanged()
    }

    fun rotatePlacement() {
        placeDir = (placeDir + 1) % 4
        ui?.onStateChanged()
    }

    fun rotateSelected() {
        val m = selected ?: return
        m.dir = (m.dir + 1) % 4
        ui?.onStateChanged()
    }

    fun startMove() {
        val m = selected ?: return
        mode = Mode.MOVE
        moving = m
        ui?.onStateChanged()
    }

    fun upgradeCost(m: Machine): Long = (m.type.cost * 0.8f * m.level).toLong()

    fun canUpgrade(m: Machine): Boolean = m.type != MachineType.CONVEYOR && m.level < 5

    fun upgradeSelected() {
        val m = selected ?: return
        if (!canUpgrade(m)) return
        if (!economy.spend(upgradeCost(m))) {
            say("Не хватает денег")
            return
        }
        m.level++
        economy.addXp(15)
        power.recompute()
        say("⬆ ${m.type.title}: уровень ${m.level}")
        ui?.onStateChanged()
    }

    fun removeSelected() {
        val m = selected ?: return
        state.removeMachine(m)
        economy.refund(m.type.refund.toLong())
        selected = null
        power.recompute()
        say("Продано: ${m.type.title} (+${money(m.type.refund.toLong())})")
        ui?.onStateChanged()
    }
}
