package com.stepan.minifactory

class ProductionManager(private val engine: GameEngine) {
    var listener: ProductionListener? = null

    fun update(dt: Float) {
        val s = engine.state
        val list = s.machines
        for (i in list.indices) {
            list[i].update(dt, s, listener)
        }
    }
}
