package com.stepan.minifactory

import android.content.Context

class OrdersScreen(ctx: Context, engine: GameEngine) : GameScreen(ctx, engine) {
    override val title = "📜 Заказы клиентов"

    override fun signature(): String {
        val s = engine.state
        val sb = StringBuilder()
        sb.append(s.contracts.size).append('|').append(s.stats.contractsDone)
        for (ty in ItemType.values()) sb.append('|').append(s.totalStored(ty))
        for (c in s.contracts) sb.append('|').append(c.id).append(':').append(c.delivered).append(':').append((c.timeLeft / 10f).toInt())
        return sb.toString()
    }

    override fun render() {
        val s = engine.state
        val om = engine.orders

        val head = ctx.card()
        val ic0 = ctx.icon()
        ic0.showMachine(MachineType.LOGISTICS)
        head.addView(ctx.infoRow(ic0, "Активные заказы", "${s.contracts.size} / ${om.maxOrders()}", Pal.ACCENT2,
            "Выполнено заказов: ${s.stats.contractsDone}. Центры логистики и исследования увеличивают число слотов."))
        head.addView(ctx.label("Сдавайте товар со склада вручную или подведите ленту к Центру логистики — он закрывает заказы автоматически. Быстрая сдача (больше половины времени) даёт +10%.", 12f, Pal.MUTED), ctx.lp(8))
        content.addView(head, ctx.lp(4))

        for (c in ArrayList(s.contracts)) {
            val stock = s.totalStored(c.item)
            val remaining = c.amount - c.delivered
            val card = ctx.card()
            val ic = ctx.icon()
            ic.showItem(c.item)
            card.addView(ctx.infoRow(ic, c.customer, money(om.expectedPay(c)), Pal.ACCENT,
                "${c.amount} × ${c.item.label}   •   доставлено ${c.delivered}/${c.amount}"))
            val urgent = c.timeLeft < 60f
            val fast = c.timeLeft > c.total * 0.5f
            card.addView(ctx.label("⏳ Осталось ${fmtTime(c.timeLeft)}" + (if (fast) "   •   +10% за скорость" else ""),
                12.5f, if (urgent) Pal.BAD else Pal.GOOD, true), ctx.lp(8))
            card.addView(ctx.progress(c.delivered.toFloat() / c.amount, Pal.GOOD), ctx.lp(6))
            card.addView(ctx.label("На складе: $stock   •   награда +${c.xp} XP", 12f, Pal.MUTED), ctx.lp(4))
            val can = stock > 0
            val take = minOf(stock, remaining)
            card.addView(ctx.pillButton(if (can) "Сдать со склада ($take шт)" else "На складе пусто", Pal.GOOD, can) {
                om.deliver(c)
                refreshNow()
            }, ctx.lp(8))
            content.addView(card, ctx.lp(12))
        }
    }
}
