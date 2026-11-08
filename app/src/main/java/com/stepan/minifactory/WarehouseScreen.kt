package com.stepan.minifactory

import android.content.Context

class WarehouseScreen(ctx: Context, engine: GameEngine) : GameScreen(ctx, engine) {
    override val title = "📦 Склад и рынок"

    override fun signature(): String {
        val s = engine.state
        val sb = StringBuilder()
        sb.append(s.money).append('|').append(s.totalCapacity()).append('|').append(s.autoSell).append('|').append(s.currentFactory)
        for (ty in ItemType.values()) sb.append('|').append(s.totalStored(ty)).append(':').append(engine.economy.priceOf(ty))
        return sb.toString()
    }

    override fun render() {
        val s = engine.state
        val eco = engine.economy
        val fac = s.factoryDef()

        val sum = ctx.card()
        val ic0 = ctx.icon()
        ic0.showMachine(MachineType.STORAGE)
        sum.addView(ctx.infoRow(ic0, "Складов: ${s.storageCount()}", "${s.totalStoredAll()} / ${s.totalCapacity()}", Pal.ACCENT2,
            "Фабрика: ${fac.title}. Базовый множитель цен ×${fac.bonus}"))
        sum.addView(ctx.progress(if (s.totalCapacity() > 0) s.totalStoredAll().toFloat() / s.totalCapacity() else 0f, Pal.ACCENT2), ctx.lp(10))
        if (s.storageCount() == 0) {
            sum.addView(ctx.label("Постройте Storage и подведите к нему конвейер, чтобы хранить продукцию.", 12.5f, Pal.BAD, true), ctx.lp(8))
        }
        content.addView(sum, ctx.lp(4))

        for (ty in ItemType.values()) {
            val card = ctx.card()
            val cnt = s.totalStored(ty)
            val ic = ctx.icon()
            ic.showItem(ty)
            val tr = eco.marketTrend(ty)
            val arrow = if (tr > 0) "▲" else if (tr < 0) "▼" else "•"
            card.addView(ctx.infoRow(ic, "${ty.label}: $cnt шт.", "${money(eco.priceOf(ty).toLong())}/шт", Pal.ACCENT,
                "Рынок ×${f2(eco.market[ty.ordinal])} $arrow  •  ${ty.info}"))
            val value = eco.sellValue(ty)
            card.addView(ctx.pillButton("Продать (+${money(value)})", Pal.GOOD, cnt > 0) {
                val got = eco.sell(ty, Int.MAX_VALUE)
                engine.say("💰 +${money(got)}")
                refreshNow()
            }, ctx.lp(10))
            content.addView(card, ctx.lp(12))
        }

        var total = 0L
        for (ty in ItemType.values()) total += eco.sellValue(ty)
        content.addView(ctx.pillButton("💰 Продать всё (+${money(total)})", Pal.ACCENT, total > 0, 16f) {
            val got = eco.sellAll()
            engine.say("💰 +${money(got)}")
            refreshNow()
        }, ctx.lp(14))

        val auto = ctx.card()
        val ic2 = ctx.icon()
        ic2.showItem(ItemType.PART)
        auto.addView(ctx.infoRow(ic2, "Автопродажа деталей", if (s.autoSell) "ВКЛ" else "ВЫКЛ", if (s.autoSell) Pal.GOOD else Pal.MUTED,
            "Готовые детали продаются со склада автоматически раз в секунду."))
        auto.addView(ctx.pillButton(if (s.autoSell) "Выключить" else "Включить", if (s.autoSell) Pal.BAD else Pal.GOOD, true) {
            s.autoSell = !s.autoSell
            refreshNow()
        }, ctx.lp(10))
        content.addView(auto, ctx.lp(12))

        val stats = ctx.card()
        stats.addView(ctx.label("📊 Статистика", 16f, Pal.TEXT, true))
        stats.addView(ctx.label("Доход за минуту: ${money(eco.perMinute(true))}", 13f, Pal.GOOD, true), ctx.lp(6))
        stats.addView(ctx.label("Расходы за минуту (обслуживание, энергия, зарплаты): ${money(eco.perMinute(false))}", 13f, Pal.BAD, true), ctx.lp(2))
        stats.addView(ctx.label("Всего заработано: ${money(s.stats.totalEarned)}", 13f, Pal.TEXT), ctx.lp(4))
        stats.addView(ctx.label("Всего потрачено: ${money(s.stats.totalSpent)}", 13f, Pal.TEXT), ctx.lp(2))
        stats.addView(ctx.label("Добыто руды: ${s.stats.oreMined}  •  Металла: ${s.stats.metalProduced}", 12.5f, Pal.MUTED), ctx.lp(4))
        stats.addView(ctx.label("Деталей: ${s.stats.partsProduced}  •  Изделий: ${s.stats.gadgetsProduced}", 12.5f, Pal.MUTED), ctx.lp(2))
        content.addView(stats, ctx.lp(12))
    }
}
