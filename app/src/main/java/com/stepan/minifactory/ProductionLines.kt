package com.stepan.minifactory

class Line(
    val title: String,
    val machines: List<Machine>,
    val valuePerMin: Float,
    val upgradeCost: Long,
    val upgradable: Int,
    val summary: String,
    val tier: Int
)

object LineAnalyzer {
    /** Находит производственные линии: связные группы зданий, соединённых выходами и лентами. */
    fun analyze(engine: GameEngine): List<Line> {
        val s = engine.state
        val eco = engine.economy
        val list = s.machines
        val n = list.size
        val idx = HashMap<Machine, Int>()
        for (i in 0 until n) idx[list[i]] = i
        val parent = IntArray(n) { it }

        fun find(a: Int): Int {
            var x = a
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]
                x = parent[x]
            }
            return x
        }

        for (m in list) {
            if (m.type == MachineType.CONVEYOR || m.recipe != null) {
                val t = s.machineAt(m.x + DX[m.dir], m.y + DY[m.dir])
                val mi = idx[m]
                val ti = if (t != null) idx[t] else null
                if (mi != null && ti != null) {
                    val a = find(mi)
                    val b = find(ti)
                    if (a != b) parent[a] = b
                }
            }
        }

        val groups = LinkedHashMap<Int, ArrayList<Machine>>()
        for (i in 0 until n) groups.getOrPut(find(i)) { ArrayList() }.add(list[i])

        val result = ArrayList<Line>()
        for (g in groups.values) {
            var tier = -1
            var value = 0f
            for (m in g) {
                val r = m.recipe ?: continue
                if (r.out.ordinal > tier) tier = r.out.ordinal
                val rate = 60f / r.cycle * s.speedMult(m)
                var v = r.outQty * eco.priceOf(r.out).toFloat()
                for (k in r.inItems.indices) v -= r.inQty[k] * eco.priceOf(r.inItems[k]).toFloat()
                value += rate * v
            }
            if (tier < 0) continue
            var cost = 0L
            var upg = 0
            for (m in g) {
                if (engine.canUpgrade(m)) {
                    cost += engine.upgradeCost(m)
                    upg++
                }
            }
            cost = (cost * 0.9f).toLong()
            val counts = LinkedHashMap<MachineType, Int>()
            for (m in g) counts[m.type] = (counts[m.type] ?: 0) + 1
            val sb = StringBuilder()
            for ((t, c) in counts) sb.append(t.icon).append('×').append(c).append("  ")
            val name = when (ItemType.values()[tier]) {
                ItemType.ORE -> "Добыча руды"
                ItemType.METAL -> "Выплавка металла"
                ItemType.PART -> "Производство деталей"
                ItemType.GADGET -> "Сборка изделий"
            }
            result.add(Line(name, g, value, cost, upg, sb.toString().trim(), tier))
        }
        result.sortWith(Comparator<Line> { a, b -> if (a.tier != b.tier) b.tier - a.tier else b.machines.size - a.machines.size })
        return result
    }

    fun upgradeLine(engine: GameEngine, line: Line): Boolean {
        if (line.upgradable <= 0) return false
        if (!engine.economy.spend(line.upgradeCost)) {
            engine.say("Не хватает денег")
            return false
        }
        for (m in line.machines) if (engine.canUpgrade(m)) m.level++
        engine.economy.addXp(20 * line.upgradable)
        engine.say("⬆ Линия «${line.title}» улучшена")
        return true
    }
}
