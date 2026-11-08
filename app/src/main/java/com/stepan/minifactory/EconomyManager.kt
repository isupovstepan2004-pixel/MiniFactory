package com.stepan.minifactory

class EconomyManager(private val engine: GameEngine) {
    private var clock = 0f
    private val incomeLog = ArrayList<FloatArray>()
    private val opLog = ArrayList<FloatArray>()
    private var autoTimer = 0f
    private var rateTimer = 0f
    private var opAcc = 0f
    private var lastWarn = -100f
    private var marketTimer = 0f

    var maintRate = 0f
    var fuelRate = 0f
    var salaryRate = 0f

    val market = FloatArray(ItemType.values().size) { 1f }
    private val target = FloatArray(ItemType.values().size) { 1f }

    private val state: GameState get() = engine.state

    fun priceOf(item: ItemType, withManager: Boolean = true): Int {
        val s = state
        val f = s.factoryDef()
        var p = item.basePrice * f.bonus * market[item.ordinal]
        if (f.specItem == null || f.specItem == item) p *= f.specPrice
        p *= 1f + 0.05f * s.researchLevel("price") + 0.04f * s.researchLevel("market")
        if (withManager) p *= s.managerMult()
        return maxOf(1, Math.round(p))
    }

    /** 1 — цена растёт, -1 — падает, 0 — стабильна. */
    fun marketTrend(item: ItemType): Int {
        val o = item.ordinal
        return if (target[o] > market[o] + 0.02f) 1 else if (target[o] < market[o] - 0.02f) -1 else 0
    }

    fun spend(c: Long): Boolean {
        if (state.money < c) return false
        state.money -= c
        state.stats.totalSpent += c
        return true
    }

    fun earn(c: Long, xp: Int = 0) {
        state.money += c
        state.stats.totalEarned += c
        incomeLog.add(floatArrayOf(clock, c.toFloat()))
        if (xp > 0) addXp(xp)
    }

    fun refund(c: Long) {
        state.money += c
    }

    fun perMinute(income: Boolean): Long {
        var sum = 0f
        val log = if (income) incomeLog else opLog
        for (e in log) sum += e[1]
        return sum.toLong()
    }

    private fun window(): Float = minOf(60f, maxOf(10f, clock))

    fun incomePerSec(): Float = perMinute(true) / window()

    fun opPerSec(): Float = maintRate + fuelRate + salaryRate

    fun netPerSec(): Float = incomePerSec() - opPerSec()

    fun xpForNext(level: Int): Int = 80 + level * 70

    fun addXp(a: Int) {
        val s = state
        s.xp += a
        while (s.xp >= xpForNext(s.level)) {
            s.xp -= xpForNext(s.level)
            s.level++
            engine.say("⭐ Новый уровень: ${s.level}!")
        }
    }

    fun sellValue(item: ItemType): Long = state.totalStored(item).toLong() * priceOf(item)

    fun sell(item: ItemType, count: Int): Long {
        val n = state.takeFromStorages(item, count)
        if (n <= 0) return 0L
        val total = n.toLong() * priceOf(item)
        state.stats.itemsSold += n
        earn(total, maxOf(1, (total / 10).toInt()))
        return total
    }

    fun sellAll(): Long {
        var t = 0L
        for (ty in ItemType.values()) t += sell(ty, Int.MAX_VALUE)
        return t
    }

    fun buyResearch(def: ResearchDef): Boolean {
        val s = state
        val lvl = s.researchLevel(def.id)
        if (lvl >= def.maxLevel) return false
        val cost = def.costAt(lvl)
        if (def.usesRp) {
            if (s.researchPoints < cost) {
                engine.say("Не хватает очков исследований")
                return false
            }
            s.researchPoints -= cost.toFloat()
        } else if (!spend(cost)) {
            engine.say("Не хватает денег")
            return false
        }
        s.research[def.id] = lvl + 1
        addXp(25)
        engine.say("🔬 ${def.title}: ур. ${lvl + 1}")
        return true
    }

    private fun trim(log: ArrayList<FloatArray>) {
        var i = 0
        while (i < log.size) {
            if (clock - log[i][0] > 60f) log.removeAt(i) else i++
        }
    }

    private fun recomputeRates() {
        val s = state
        var m = 0f
        for (mc in s.machines) m += s.maintenance(mc)
        maintRate = m
        fuelRate = s.powerUsed * 0.08f
        salaryRate = s.salaryPerSec()
    }

    fun update(dt: Float) {
        clock += dt
        trim(incomeLog)
        trim(opLog)

        rateTimer += dt
        if (rateTimer >= 0.5f) {
            rateTimer = 0f
            recomputeRates()
        }

        opAcc += opPerSec() * dt
        if (opAcc >= 1f) {
            val due = opAcc.toLong()
            opAcc -= due
            val s = state
            val pay = minOf(due, s.money)
            s.money -= pay
            s.stats.totalSpent += pay
            opLog.add(floatArrayOf(clock, pay.toFloat()))
            if (pay < due && clock - lastWarn > 30f) {
                lastWarn = clock
                engine.say("⚠ Не хватает денег на расходы!")
            }
        }

        marketTimer += dt
        if (marketTimer >= 25f) {
            marketTimer = 0f
            for (i in target.indices) target[i] = 0.82f + state.rng.nextFloat() * 0.5f
        }
        val k = minOf(1f, dt * 0.15f)
        for (i in market.indices) market[i] += (target[i] - market[i]) * k

        autoTimer += dt
        if (autoTimer >= 1f) {
            autoTimer = 0f
            if (state.autoSell) sell(ItemType.PART, Int.MAX_VALUE)
        }
    }
}
