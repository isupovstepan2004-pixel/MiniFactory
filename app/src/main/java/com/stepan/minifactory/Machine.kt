package com.stepan.minifactory

const val BELT_SPACING = 0.34f

fun createMachine(id: Int, type: MachineType, x: Int, y: Int, dir: Int): Machine =
    if (type == MachineType.CONVEYOR) Conveyor(id, x, y, dir) else Machine(id, type, x, y, dir)

open class Machine(val id: Int, val type: MachineType, var x: Int, var y: Int, var dir: Int) {
    var level = 1
    var timer = 0f
    var working = false
    var outCount = 0
    var anim = 0f
    val inBuf = IntArray(ItemType.values().size)
    val stored = IntArray(ItemType.values().size)
    val recipe: Recipe? = Recipes.of(type)

    fun storedTotal(): Int {
        var s = 0
        for (v in stored) s += v
        return s
    }

    open fun accepts(item: ItemType, state: GameState): Boolean {
        if (type == MachineType.STORAGE) return storedTotal() < state.storageCapacity(this)
        if (type == MachineType.LOGISTICS) return storedTotal() < LOGI_CAP
        val r = recipe ?: return false
        for (i in r.inItems.indices) {
            if (r.inItems[i] == item) return inBuf[item.ordinal] < r.cap(i)
        }
        return false
    }

    open fun accept(item: ItemType, state: GameState): Boolean {
        if (!accepts(item, state)) return false
        if (type == MachineType.STORAGE || type == MachineType.LOGISTICS) stored[item.ordinal]++
        else inBuf[item.ordinal]++
        return true
    }

    open fun update(dt: Float, state: GameState, listener: ProductionListener?) {
        anim += dt
        val r = recipe
        if (r != null) {
            val mult = state.speedMult(this)
            var hasIn = true
            for (i in r.inItems.indices) {
                if (inBuf[r.inItems[i].ordinal] < r.inQty[i]) {
                    hasIn = false
                    break
                }
            }
            if (hasIn && outCount + r.outQty <= r.outCap && mult > 0.001f) {
                working = true
                timer += dt * mult
                if (timer >= r.cycle) {
                    timer = 0f
                    for (i in r.inItems.indices) inBuf[r.inItems[i].ordinal] -= r.inQty[i]
                    outCount += r.outQty
                    val bc = state.bonusChance()
                    if (bc > 0f && state.rng.nextFloat() < bc) outCount += 1
                    when (r.out) {
                        ItemType.ORE -> state.stats.oreMined += r.outQty
                        ItemType.METAL -> state.stats.metalProduced += r.outQty
                        ItemType.PART -> state.stats.partsProduced += r.outQty
                        ItemType.GADGET -> state.stats.gadgetsProduced += r.outQty
                    }
                    listener?.onProduced(this, r.out)
                }
            } else {
                working = false
            }
            pushOutput(state, r.out)
            return
        }
        if (type == MachineType.RESEARCH_CENTER) {
            val f = state.powerFactor
            working = f > 0.05f
            if (working) state.researchPoints += dt * RP_RATE * level * f
        }
    }

    private fun pushOutput(state: GameState, ot: ItemType) {
        if (outCount <= 0) return
        val t = state.machineAt(x + DX[dir], y + DY[dir]) ?: return
        if (t is Conveyor) {
            if (t.canAcceptFrom(dir)) {
                t.addItem(ot, dir)
                outCount--
            }
        } else if (t.accept(ot, state)) {
            outCount--
        }
    }
}

class BeltItem(val type: ItemType, var pos: Float, val inDir: Int)

class Conveyor(id: Int, x: Int, y: Int, dir: Int) : Machine(id, MachineType.CONVEYOR, x, y, dir) {
    val items = ArrayList<BeltItem>()

    fun canAcceptFrom(srcDir: Int): Boolean {
        if (srcDir == (dir + 2) % 4) return false
        return items.isEmpty() || items[items.size - 1].pos >= BELT_SPACING
    }

    fun addItem(t: ItemType, inDir: Int) {
        items.add(BeltItem(t, 0f, inDir))
    }

    override fun accepts(item: ItemType, state: GameState): Boolean = false

    override fun update(dt: Float, state: GameState, listener: ProductionListener?) {
        anim += dt
        val speed = state.conveyorSpeed()
        for (i in items.indices) {
            val bi = items[i]
            val limit = if (i == 0) 1f else items[i - 1].pos - BELT_SPACING
            val np = bi.pos + speed * dt
            bi.pos = if (np > limit) maxOf(bi.pos, limit) else np
        }
        if (items.isNotEmpty() && items[0].pos >= 0.999f) {
            val f = items[0]
            val t = state.machineAt(x + DX[dir], y + DY[dir])
            if (t is Conveyor) {
                if (t.canAcceptFrom(dir)) {
                    t.addItem(f.type, dir)
                    items.removeAt(0)
                }
            } else if (t != null && t.accept(f.type, state)) {
                items.removeAt(0)
            }
        }
        working = items.isNotEmpty()
    }
}
