package com.stepan.minifactory

class StaffManager(private val engine: GameEngine) {
    val maxLevel = 5

    fun hireCost(i: Int): Long =
        (StaffType.values()[i].hireBase * Math.pow(1.3, engine.state.staffCount[i].toDouble())).toLong()

    fun trainCost(i: Int): Long =
        (StaffType.values()[i].hireBase * 3.0 * Math.pow(1.9, (engine.state.staffLevel[i] - 1).toDouble())).toLong()

    fun canHire(i: Int): Boolean = engine.state.staffCount[i] < engine.state.staffCap()

    fun hire(i: Int): Boolean {
        val s = engine.state
        if (!canHire(i)) {
            engine.say("Достигнут лимит персонала — откройте новые фабрики")
            return false
        }
        if (!engine.economy.spend(hireCost(i))) {
            engine.say("Не хватает денег")
            return false
        }
        s.staffCount[i]++
        engine.say("${StaffType.values()[i].icon} Нанят сотрудник: ${StaffType.values()[i].title}")
        return true
    }

    fun fire(i: Int): Boolean {
        val s = engine.state
        if (s.staffCount[i] <= 0) return false
        s.staffCount[i]--
        engine.say("Сотрудник уволен")
        return true
    }

    fun train(i: Int): Boolean {
        val s = engine.state
        if (s.staffCount[i] <= 0) {
            engine.say("Сначала наймите сотрудников")
            return false
        }
        if (s.staffLevel[i] >= maxLevel) return false
        if (!engine.economy.spend(trainCost(i))) {
            engine.say("Не хватает денег")
            return false
        }
        s.staffLevel[i]++
        engine.economy.addXp(30)
        engine.say("🎓 ${StaffType.values()[i].title}: уровень ${s.staffLevel[i]}")
        return true
    }
}
