package com.stepan.minifactory

/** Заказы покупателей и рейсы центров логистики. */
class OrderManager(private val engine: GameEngine) {
    private val customers = arrayOf(
        "ТехноСтрой", "АвтоМир", "ГородМаркет", "МегаСтрой", "ЭлектроПлюс",
        "РоботТех", "СтройГарант", "ПромСнаб", "ЗаводСервис", "Северный Альянс"
    )

    fun maxOrders(): Int {
        val s = engine.state
        return minOf(7, 3 + s.countOf(MachineType.LOGISTICS) + s.researchLevel("orders") / 2)
    }

    fun ensureOrders() {
        val s = engine.state
        while (s.contracts.size < maxOrders()) s.contracts.add(newOrder())
    }

    fun readyCount(): Int {
        val s = engine.state
        var n = 0
        for (c in s.contracts) if (s.totalStored(c.item) > 0) n++
        return n
    }

    private fun newOrder(): Contract {
        val s = engine.state
        val id = s.contractNonce
        s.contractNonce++
        val rnd = java.util.Random(id.toLong() * 7919L + 13L)
        val lvl = s.level
        val p = rnd.nextInt(100)
        val item = when {
            lvl >= 6 && p < 20 -> ItemType.GADGET
            lvl >= 2 && p < 55 -> ItemType.PART
            p < 80 -> ItemType.METAL
            else -> ItemType.ORE
        }
        val amount = when (item) {
            ItemType.ORE -> 12 + rnd.nextInt(15) + lvl * 3
            ItemType.METAL -> 8 + rnd.nextInt(8) + lvl * 2
            ItemType.PART -> 5 + rnd.nextInt(6) + lvl
            ItemType.GADGET -> 2 + rnd.nextInt(3) + lvl / 3
        }
        val price = engine.economy.priceOf(item, false)
        val premium = 1.3f + rnd.nextFloat() * 0.5f
        val reward = maxOf(10, (amount * price * premium).toInt())
        val xp = 15 + amount * 2
        val total = (360 + rnd.nextInt(360) + (if (item == ItemType.GADGET) 240 else 0)).toFloat()
        val customer = customers[rnd.nextInt(customers.size)]
        return Contract(id, item, amount, reward, xp, customer, total, total, 0)
    }

    fun update(dt: Float) {
        val s = engine.state
        var i = 0
        while (i < s.contracts.size) {
            val c = s.contracts[i]
            c.timeLeft -= dt
            if (c.timeLeft <= 0f) {
                engine.say("⌛ Заказ «${c.customer}» просрочен")
                s.contracts[i] = newOrder()
            }
            i++
        }
        if (s.contracts.size < maxOrders()) s.contracts.add(newOrder())
        shipping(dt)
    }

    /** Сдать товар со складов в заказ. Возвращает количество доставленных единиц. */
    fun deliver(c: Contract): Int {
        val s = engine.state
        val need = c.amount - c.delivered
        val got = s.takeFromStorages(c.item, need)
        if (got <= 0) return 0
        c.delivered += got
        if (c.delivered >= c.amount) complete(c) else engine.say("📦 ${c.customer}: ${c.delivered}/${c.amount}")
        return got
    }

    /** Ожидаемая награда с учётом менеджеров и исследований (без бонуса за скорость). */
    fun expectedPay(c: Contract): Long {
        val s = engine.state
        return (c.reward * s.managerMult() *
            (1f + 0.10f * s.researchLevel("contract") + 0.10f * s.researchLevel("orders"))).toLong()
    }

    private fun complete(c: Contract) {
        val s = engine.state
        var pay = expectedPay(c).toFloat()
        if (c.timeLeft > c.total * 0.5f) pay *= 1.1f
        val total = pay.toLong()
        engine.economy.earn(total, c.xp)
        s.stats.contractsDone++
        val idx = s.contracts.indexOf(c)
        if (idx >= 0) s.contracts[idx] = newOrder()
        engine.say("✅ ${c.customer}: +${money(total)}")
    }

    private fun shipping(dt: Float) {
        val s = engine.state
        for (m in s.machines) {
            if (m.type != MachineType.LOGISTICS) continue
            val f = s.powerFactor
            if (m.storedTotal() > 0 && f > 0.05f) {
                m.working = true
                m.timer += dt * f
                if (m.timer >= 4f) {
                    m.timer = 0f
                    ship(m)
                }
            } else {
                m.working = false
            }
        }
    }

    private fun ship(m: Machine) {
        val s = engine.state
        var cap = 6 * m.level
        for (ty in ItemType.values()) {
            val o = ty.ordinal
            if (cap <= 0) break
            // сначала закрываем заказы
            for (c in ArrayList(s.contracts)) {
                if (cap <= 0 || m.stored[o] <= 0) break
                if (c.item == ty && c.delivered < c.amount) {
                    val take = minOf(minOf(m.stored[o], cap), c.amount - c.delivered)
                    c.delivered += take
                    m.stored[o] -= take
                    cap -= take
                    if (c.delivered >= c.amount) complete(c)
                }
            }
            // остаток продаём с наценкой
            val sell = minOf(m.stored[o], cap)
            if (sell > 0) {
                m.stored[o] -= sell
                cap -= sell
                val total = (sell * engine.economy.priceOf(ty) * (1f + 0.05f * m.level)).toLong()
                s.stats.itemsSold += sell
                engine.economy.earn(total, maxOf(1, (total / 10).toInt()))
            }
        }
    }
}
