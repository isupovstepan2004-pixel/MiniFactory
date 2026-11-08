package com.stepan.minifactory

/** Электросеть: считает выработку, потребление и долю питания зданий. */
class PowerManager(private val engine: GameEngine) {
    private var t = 0f

    fun update(dt: Float) {
        t += dt
        if (t < 0.25f) return
        t = 0f
        recompute()
    }

    fun recompute() {
        val s = engine.state
        var supply = 0f
        var demand = 0f
        for (m in s.machines) {
            if (m.type == MachineType.POWER_PLANT) supply += s.plantOutput(m)
            else if (m.type.power > 0) demand += s.machinePower(m)
        }
        val used = minOf(supply, demand)
        s.powerSupply = supply
        s.powerDemand = demand
        s.powerUsed = used
        s.powerFactor = if (demand <= 0.001f) 1f else minOf(1f, supply / demand)
        for (m in s.machines) {
            if (m.type == MachineType.POWER_PLANT) m.working = used > 0.01f
        }
    }
}
