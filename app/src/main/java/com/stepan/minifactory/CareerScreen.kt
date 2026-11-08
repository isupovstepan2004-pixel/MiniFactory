package com.stepan.minifactory

import android.content.Context

class CareerScreen(ctx: Context, engine: GameEngine) : GameScreen(ctx, engine) {
    override val title = "🏆 Карьера"

    override fun signature(): String {
        val s = engine.state
        return "${s.money}|${s.level}|${s.achievements.size}|${s.currentFactory}|${s.unlockedFactories.size}|${s.territorySize()}|${s.stats.contractsDone}"
    }

    override fun render() {
        val s = engine.state
        val career = engine.career

        // территория
        val terr = ctx.card()
        val ict = ctx.icon()
        ict.showEmoji("🗺")
        terr.addView(ctx.infoRow(ict, "Территория", "${s.territorySize()}×${s.territorySize()} из ${s.gridSize}×${s.gridSize}", Pal.ACCENT2,
            "Закрытые клетки на карте (красные, за забором) недоступны для строительства."))
        val ns = career.nextTerritory()
        if (ns < 0) {
            terr.addView(ctx.label("Вся территория этой фабрики открыта ★", 13f, Pal.GOOD, true), ctx.lp(8))
        } else {
            val cost = career.expandCost(ns)
            val lvlNeed = career.expandLevel(ns)
            val lvlOk = s.level >= lvlNeed
            terr.addView(ctx.label((if (lvlOk) "✓ " else "✗ ") + "Уровень: ${s.level} / $lvlNeed", 13f, if (lvlOk) Pal.GOOD else Pal.BAD, true), ctx.lp(8))
            val can = lvlOk && s.money >= cost
            terr.addView(ctx.pillButton("Расширить до $ns×$ns за ${money(cost)}", Pal.GOOD, can) {
                career.expandTerritory()
                refreshNow()
            }, ctx.lp(8))
        }
        content.addView(terr, ctx.lp(4))

        content.addView(ctx.label("🏭 Стадии развития", 15f, Pal.TEXT, true), ctx.lp(14))
        content.addView(ctx.label("Каждая фабрика — отдельная территория со своим складом, специализацией и множителем цен. Для открытия нужны деньги (тратятся), уровень, заказы и достижения.", 12.5f, Pal.MUTED), ctx.lp(4))

        for (f in career.factories) {
            val card = ctx.card()
            val unlocked = s.unlockedFactories.contains(f.id)
            val ic = ctx.icon()
            ic.showEmoji("🏭")
            val tag = if (unlocked) (if (f.id == s.currentFactory) "★ здесь" else "✓ открыта") else "🔒"
            card.addView(ctx.infoRow(ic, f.title, tag, if (unlocked) Pal.GOOD else Pal.MUTED,
                "${f.desc} Сетка ${f.gridSize}×${f.gridSize}, цены ×${f.bonus}."))
            card.addView(ctx.label("🎯 " + f.specText(), 12f, Pal.ACCENT2), ctx.lp(6))
            if (!unlocked) {
                for (r in career.requirements(f)) {
                    card.addView(ctx.label((if (r.ok) "✓ " else "✗ ") + r.text, 13f, if (r.ok) Pal.GOOD else Pal.BAD, true), ctx.lp(4))
                }
                val met = career.requirementsMet(f)
                card.addView(ctx.pillButton(if (met) "Открыть за ${money(f.needMoney)}" else "Требования не выполнены", Pal.GOOD, met) {
                    career.unlock(f)
                    refreshNow()
                }, ctx.lp(10))
            } else if (f.id != s.currentFactory) {
                card.addView(ctx.pillButton("Перейти на фабрику", Pal.BLUE, true) {
                    engine.switchFactory(f.id)
                    refreshNow()
                }, ctx.lp(10))
            }
            content.addView(card, ctx.lp(12))
        }

        val ach = ctx.card()
        ach.addView(ctx.label("🏆 Достижения (${s.achievements.size}/${Achievements.all.size})", 16f, Pal.TEXT, true))
        for (a in Achievements.all) {
            val got = s.achievements.contains(a.id)
            ach.addView(ctx.label((if (got) "✓ " else "○ ") + a.title + " — " + a.desc, 13f, if (got) Pal.GOOD else Pal.MUTED, got), ctx.lp(5))
        }
        content.addView(ach, ctx.lp(12))
    }
}
