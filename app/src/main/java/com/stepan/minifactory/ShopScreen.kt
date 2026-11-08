package com.stepan.minifactory

import android.content.Context

class ShopScreen(ctx: Context, engine: GameEngine, private val closeHost: () -> Unit) : GameScreen(ctx, engine) {
    override val title = "🛒 Магазин"

    override fun signature(): String {
        val s = engine.state
        return "${s.money}|${s.machines.size}|${s.level}|${s.stage()}|${s.achievements.size}"
    }

    override fun render() {
        val st = engine.state

        val bal = ctx.card()
        val br = ctx.row()
        br.addView(ctx.label("🪙 Ваш баланс", 15f, Pal.MUTED, true), ctx.weightLp())
        br.addView(ctx.label(money(st.money), 20f, Pal.ACCENT, true))
        bal.addView(br)
        bal.addView(ctx.label("Выберите здание и нажмите на пустую клетку открытой территории. Новые здания открываются по уровню, достижениям и фабрикам.", 12.5f, Pal.MUTED), ctx.lp(6))
        content.addView(bal, ctx.lp(4))

        for (t in MachineType.values()) {
            val why = Unlocks.reason(t, st)
            val card = ctx.card()
            val ic = ctx.icon()
            ic.showMachine(t)
            card.addView(ctx.infoRow(ic, t.title, if (why == null) money(t.cost.toLong()) else "🔒", Pal.ACCENT, t.desc))
            card.addView(ctx.label("⚙ " + t.io, 12.5f, Pal.ACCENT2, true), ctx.lp(8))
            val maint = t.cost * 0.0006f * 60f
            var line = "Обслуживание: ${money(maint.toLong())}/мин"
            if (t.power > 0) line += "   •   ⚡ Потребляет ${t.power} МВт"
            if (t == MachineType.POWER_PLANT) line += "   •   🔥 Топливо ~$0.08 в секунду за МВт"
            card.addView(ctx.label(line, 12f, Pal.MUTED), ctx.lp(2))
            card.addView(ctx.label("Построено: ${st.countOf(t)}", 12f, Pal.MUTED), ctx.lp(2))
            if (why != null) {
                card.addView(ctx.label("🔒 $why", 12.5f, Pal.BAD, true), ctx.lp(4))
                card.addView(ctx.pillButton("Закрыто", Pal.DISABLED, false) {}, ctx.lp(8))
            } else {
                val afford = st.money >= t.cost
                val text = if (afford) "Купить и поставить" else "Не хватает денег"
                card.addView(ctx.pillButton(text, Pal.GOOD, afford) {
                    engine.startPlacing(t)
                    closeHost()
                }, ctx.lp(8))
            }
            content.addView(card, ctx.lp(12))
        }
    }
}
