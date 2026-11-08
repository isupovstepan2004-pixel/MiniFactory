package com.stepan.minifactory

class Req(val text: String, val ok: Boolean)

class CareerManager(private val engine: GameEngine) {
    val factories = Factories.all
    private var timer = 0f

    fun update(dt: Float) {
        timer += dt
        if (timer < 1f) return
        timer = 0f
        val s = engine.state
        for (a in Achievements.all) {
            if (!s.achievements.contains(a.id) && a.check(s)) {
                s.achievements.add(a.id)
                engine.economy.addXp(40)
                engine.say("🏆 Достижение: ${a.title}")
            }
        }
    }

    fun requirements(f: FactoryDef): List<Req> {
        val s = engine.state
        return listOf(
            Req("Деньги: ${money(s.money)} / ${money(f.needMoney)}", s.money >= f.needMoney),
            Req("Уровень: ${s.level} / ${f.needLevel}", s.level >= f.needLevel),
            Req("Заказы: ${s.stats.contractsDone} / ${f.needContracts}", s.stats.contractsDone >= f.needContracts),
            Req("Достижения: ${s.achievements.size} / ${f.needAchievements}", s.achievements.size >= f.needAchievements)
        )
    }

    fun requirementsMet(f: FactoryDef): Boolean {
        for (r in requirements(f)) if (!r.ok) return false
        return true
    }

    fun unlock(f: FactoryDef) {
        val s = engine.state
        if (s.unlockedFactories.contains(f.id)) return
        if (!requirementsMet(f)) {
            engine.say("Требования не выполнены")
            return
        }
        if (!engine.economy.spend(f.needMoney)) {
            engine.say("Не хватает денег")
            return
        }
        s.unlockedFactories.add(f.id)
        engine.economy.addXp(200)
        engine.say("🏭 Открыто: ${f.title}!")
        engine.switchFactory(f.id)
    }

    // ---------- территории ----------

    fun nextTerritory(): Int {
        val s = engine.state
        val cur = s.territorySize()
        return if (cur >= s.gridSize) -1 else minOf(s.gridSize, cur + 2)
    }

    fun expandCost(newSize: Int): Long =
        250L * (newSize - 8) * (engine.state.currentFactory + 1)

    fun expandLevel(newSize: Int): Int = newSize / 2 - 3

    fun expandTerritory(): Boolean {
        val s = engine.state
        val ns = nextTerritory()
        if (ns < 0) return false
        if (s.level < expandLevel(ns)) {
            engine.say("Нужен уровень ${expandLevel(ns)}")
            return false
        }
        if (!engine.economy.spend(expandCost(ns))) {
            engine.say("Не хватает денег")
            return false
        }
        s.territory[s.currentFactory] = ns
        engine.economy.addXp(50)
        engine.say("🗺 Территория расширена до ${ns}×${ns}")
        engine.ui?.onStateChanged()
        return true
    }
}
