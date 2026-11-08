package com.stepan.minifactory

import android.content.Context

class ResearchScreen(ctx: Context, engine: GameEngine) : GameScreen(ctx, engine) {
    override val title = "🔬 Исследования"

    override fun signature(): String {
        val sb = StringBuilder()
        sb.append(engine.state.money).append('|').append(engine.state.researchPoints.toInt())
        for (d in ResearchCatalog.all) sb.append('|').append(engine.state.researchLevel(d.id))
        return sb.toString()
    }

    private fun addDef(d: ResearchDef) {
        val st = engine.state
        val lvl = st.researchLevel(d.id)
        val card = ctx.card()
        val ic = ctx.icon()
        ic.showEmoji(d.icon)
        card.addView(ctx.infoRow(ic, d.title, "ур. $lvl/${d.maxLevel}", Pal.ACCENT2, d.desc))
        card.addView(ctx.pips(lvl, d.maxLevel, if (d.usesRp) Pal.PURPLE else Pal.ACCENT), ctx.lp(10))
        if (lvl >= d.maxLevel) {
            card.addView(ctx.pillButton("Максимальный уровень ★", Pal.DISABLED, false) {}, ctx.lp(10))
        } else {
            val cost = d.costAt(lvl)
            val afford = if (d.usesRp) st.researchPoints >= cost else st.money >= cost
            val text = if (d.usesRp) "Изучить за $cost ОИ" else "Улучшить за ${money(cost)}"
            card.addView(ctx.pillButton(text, if (d.usesRp) Pal.PURPLE else Pal.ACCENT, afford) {
                engine.economy.buyResearch(d)
                refreshNow()
            }, ctx.lp(10))
        }
        content.addView(card, ctx.lp(12))
    }

    override fun render() {
        val st = engine.state

        val bal = ctx.card()
        val br = ctx.row()
        br.addView(ctx.label("🪙 Баланс", 15f, Pal.MUTED, true), ctx.weightLp())
        br.addView(ctx.label(money(st.money), 18f, Pal.ACCENT, true))
        bal.addView(br)
        val br2 = ctx.row()
        br2.addView(ctx.label("🔬 Очки исследований", 15f, Pal.MUTED, true), ctx.weightLp())
        br2.addView(ctx.label("${st.researchPoints.toInt()} ОИ", 18f, Pal.PURPLE, true))
        bal.addView(br2, ctx.lp(4))
        val centers = st.countOf(MachineType.RESEARCH_CENTER)
        val msg = if (centers == 0) "Постройте Исследовательский центр, чтобы получать ОИ для продвинутых технологий."
        else "Центров: $centers • приток ${f2(st.researchRate())} ОИ/с (нужно электричество)."
        bal.addView(ctx.label(msg, 12.5f, if (centers == 0) Pal.BAD else Pal.MUTED), ctx.lp(6))
        content.addView(bal, ctx.lp(4))

        content.addView(ctx.label("💰 Улучшения за деньги", 15f, Pal.TEXT, true), ctx.lp(12))
        for (d in ResearchCatalog.all) if (!d.usesRp) addDef(d)
        content.addView(ctx.label("🔬 Технологии за очки исследований", 15f, Pal.TEXT, true), ctx.lp(14))
        for (d in ResearchCatalog.all) if (d.usesRp) addDef(d)
    }
}
