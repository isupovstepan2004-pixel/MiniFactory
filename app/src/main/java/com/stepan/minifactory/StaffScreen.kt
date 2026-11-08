package com.stepan.minifactory

import android.content.Context
import android.widget.LinearLayout

class StaffScreen(ctx: Context, engine: GameEngine) : GameScreen(ctx, engine) {
    override val title = "👷 Персонал"

    override fun signature(): String {
        val s = engine.state
        return "${s.money}|${s.staffCount.joinToString(",")}|${s.staffLevel.joinToString(",")}|${s.stage()}"
    }

    private fun effectText(i: Int): String {
        val s = engine.state
        return when (i) {
            0 -> "Сейчас: +${((s.workerMult() - 1f) * 100).toInt()}% к скорости всех станков"
            1 -> "Сейчас: −${((1f - s.costFactor()) * 100).toInt()}% расходов, шанс бонуса ${(s.bonusChance() * 100).toInt()}%"
            else -> "Сейчас: +${((s.managerMult() - 1f) * 100).toInt()}% к ценам и наградам"
        }
    }

    override fun render() {
        val s = engine.state
        val sm = engine.staff

        val sum = ctx.card()
        sum.addView(ctx.label("Сотрудников: ${s.staffTotal()}", 16f, Pal.TEXT, true))
        sum.addView(ctx.label("Лимит на каждую профессию: ${s.staffCap()} (растёт с открытием фабрик)", 12.5f, Pal.MUTED), ctx.lp(3))
        sum.addView(ctx.label("Зарплаты: ${money((s.salaryPerSec() * 60f).toLong())}/мин", 13f, Pal.BAD, true), ctx.lp(3))
        content.addView(sum, ctx.lp(4))

        for (i in 0..2) {
            val t = StaffType.values()[i]
            val cnt = s.staffCount[i]
            val lvl = s.staffLevel[i]
            val card = ctx.card()
            val ic = ctx.icon()
            ic.showEmoji(t.icon)
            card.addView(ctx.infoRow(ic, t.title, "$cnt шт • ур. $lvl", Pal.ACCENT2, t.desc))
            card.addView(ctx.label(effectText(i), 12.5f, Pal.GOOD, true), ctx.lp(8))
            val wage = WAGES[i] * (1f + 0.2f * (lvl - 1)) * 60f
            card.addView(ctx.label("Зарплата одного: ${money(wage.toLong())}/мин", 12f, Pal.MUTED), ctx.lp(2))
            card.addView(ctx.pips(lvl, sm.maxLevel, Pal.ACCENT), ctx.lp(8))

            val rowB = LinearLayout(ctx)
            rowB.orientation = LinearLayout.HORIZONTAL
            val hc = sm.hireCost(i)
            val canHire = sm.canHire(i) && s.money >= hc
            val hireText = if (!sm.canHire(i)) "Лимит" else "Нанять ${money(hc)}"
            rowB.addView(ctx.pillButton(hireText, Pal.GOOD, canHire, 13f) {
                sm.hire(i)
                refreshNow()
            }, ctx.weightLp())
            val tc = sm.trainCost(i)
            val canTrain = cnt > 0 && lvl < sm.maxLevel && s.money >= tc
            val trainText = if (lvl >= sm.maxLevel) "Макс. ур." else "Обучить ${money(tc)}"
            rowB.addView(ctx.pillButton(trainText, Pal.ACCENT, canTrain, 13f) {
                sm.train(i)
                refreshNow()
            }, ctx.weightLp())
            rowB.addView(ctx.pillButton("Уволить", Pal.BAD, cnt > 0, 13f) {
                sm.fire(i)
                refreshNow()
            }, ctx.weightLp())
            card.addView(rowB, ctx.lp(8))
            content.addView(card, ctx.lp(12))
        }
    }
}
