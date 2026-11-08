package com.stepan.minifactory

import android.content.Context

class ProductionScreen(ctx: Context, engine: GameEngine) : GameScreen(ctx, engine) {
    override val title = "🏭 Производство и финансы"

    override fun signature(): String {
        val s = engine.state
        var lv = 0
        for (m in s.machines) lv += m.level
        return "${s.money}|${s.machines.size}|$lv|${s.powerSupply.toInt()}|${s.powerDemand.toInt()}|${(engine.economy.netPerSec() * 10).toInt()}|${s.staffTotal()}|${s.currentFactory}"
    }

    override fun render() {
        val s = engine.state
        val eco = engine.economy
        val fd = s.factoryDef()

        // специализация
        val spec = ctx.card()
        val ic0 = ctx.icon()
        ic0.showEmoji("🎯")
        spec.addView(ctx.infoRow(ic0, fd.title, "×${fd.bonus}", Pal.ACCENT, "Специализация — " + fd.specText()))
        content.addView(spec, ctx.lp(4))

        // финансы
        val fin = ctx.card()
        fin.addView(ctx.label("💹 Деньги и доход в секунду", 16f, Pal.TEXT, true))
        fin.addView(ctx.kv("Доход от продаж", "+" + usd(eco.incomePerSec()) + "/с", Pal.GOOD), ctx.lp(8))
        fin.addView(ctx.kv("🔧 Обслуживание зданий", "−" + usd(eco.maintRate) + "/с", Pal.BAD), ctx.lp(3))
        fin.addView(ctx.kv("⚡ Электричество (топливо)", "−" + usd(eco.fuelRate) + "/с", Pal.BAD), ctx.lp(3))
        fin.addView(ctx.kv("👷 Зарплаты персонала", "−" + usd(eco.salaryRate) + "/с", Pal.BAD), ctx.lp(3))
        val net = eco.netPerSec()
        fin.addView(ctx.kv("Итого прибыль", (if (net >= 0f) "+" else "−") + usd(Math.abs(net)) + "/с", if (net >= 0f) Pal.GOOD else Pal.BAD), ctx.lp(6))
        fin.addView(ctx.label("Расходы списываются постоянно. Инженеры и исследования снижают их, менеджеры повышают доход.", 12f, Pal.MUTED), ctx.lp(6))
        content.addView(fin, ctx.lp(12))

        // электросеть
        val pw = ctx.card()
        val icp = ctx.icon()
        icp.showMachine(MachineType.POWER_PLANT)
        val deficit = s.powerDemand > s.powerSupply + 0.01f
        pw.addView(ctx.infoRow(icp, "Электросеть", "${f1(s.powerDemand)} / ${f1(s.powerSupply)} МВт", if (deficit) Pal.BAD else Pal.ACCENT2,
            "Электростанций: ${s.countOf(MachineType.POWER_PLANT)}. Здания получают питание на ${(s.powerFactor * 100).toInt()}%."))
        pw.addView(ctx.progress(if (s.powerSupply > 0f) s.powerDemand / s.powerSupply else 0f, if (deficit) Pal.BAD else Pal.ACCENT2), ctx.lp(10))
        if (deficit) {
            pw.addView(ctx.label("⚠ Нехватка энергии: здания работают медленнее. Постройте или улучшите электростанцию.", 12.5f, Pal.BAD, true), ctx.lp(8))
        } else if (s.powerDemand <= 0.01f) {
            pw.addView(ctx.label("Пока нет зданий, потребляющих электричество.", 12.5f, Pal.MUTED), ctx.lp(8))
        }
        content.addView(pw, ctx.lp(12))

        // линии
        val lines = LineAnalyzer.analyze(engine)
        val head = ctx.card()
        head.addView(ctx.label("🏭 Производственные линии: ${lines.size}", 16f, Pal.TEXT, true))
        head.addView(ctx.label("Линия — это группа зданий, соединённых лентами. Улучшение линии повышает уровень всех её зданий сразу со скидкой 10%.", 12.5f, Pal.MUTED), ctx.lp(4))
        content.addView(head, ctx.lp(12))
        var n = 1
        for (ln in lines) {
            val card = ctx.card()
            val ic = ctx.icon()
            ic.showItem(ItemType.values()[ln.tier])
            card.addView(ctx.infoRow(ic, "Линия $n: ${ln.title}", "≈ ${money(ln.valuePerMin.toLong())}/мин", Pal.GOOD, ln.summary))
            card.addView(ctx.label("Зданий: ${ln.machines.size}   •   можно улучшить: ${ln.upgradable}", 12f, Pal.MUTED), ctx.lp(6))
            val label = if (ln.upgradable == 0) "Все здания на максимуме ★" else "Улучшить линию за ${money(ln.upgradeCost)}"
            val ok = ln.upgradable > 0 && s.money >= ln.upgradeCost
            card.addView(ctx.pillButton(if (ln.upgradable > 0 && !ok) "Не хватает денег (${money(ln.upgradeCost)})" else label, Pal.ACCENT, ok) {
                LineAnalyzer.upgradeLine(engine, ln)
                refreshNow()
            }, ctx.lp(8))
            content.addView(card, ctx.lp(12))
            n++
        }
        if (lines.isEmpty()) {
            content.addView(ctx.label("Линий пока нет — постройте добывающее здание и подключите ленты.", 13f, Pal.MUTED), ctx.lp(12))
        }
    }
}
