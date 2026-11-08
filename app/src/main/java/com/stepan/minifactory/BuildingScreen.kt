package com.stepan.minifactory

import android.content.Context

class BuildingScreen(ctx: Context, engine: GameEngine, private val closeHost: () -> Unit) : GameScreen(ctx, engine) {
    override val title = "🏗 Характеристики здания"

    override fun signature(): String {
        val m = engine.selected ?: return "none"
        val s = engine.state
        return "${m.id}|${m.level}|${m.working}|${m.outCount}|${s.money / 10}|${m.storedTotal()}|${(s.powerFactor * 20).toInt()}|${m.dir}|${m.x},${m.y}"
    }

    private fun nextLevelText(m: Machine): String? {
        if (!engine.canUpgrade(m)) return null
        val s = engine.state
        val nl = m.level + 1
        val r = m.recipe
        return when {
            r != null -> {
                val cur = s.speedMult(m)
                val nxt = cur * (1f + 0.25f * (nl - 1)) / (1f + 0.25f * (m.level - 1))
                "Скорость ${(cur * 100).toInt()}% → ${(nxt * 100).toInt()}%"
            }
            m.type == MachineType.POWER_PLANT ->
                "Выработка ${f1(s.plantOutput(m))} → ${f1((10f + 6f * (nl - 1)) * (1f + 0.10f * s.researchLevel("grid")))} МВт"
            m.type == MachineType.RESEARCH_CENTER -> "ОИ/с ${f2(RP_RATE * m.level)} → ${f2(RP_RATE * nl)}"
            m.type == MachineType.LOGISTICS -> "Рейс до ${6 * m.level} → ${6 * nl} шт, наценка +${5 * m.level}% → +${5 * nl}%"
            m.type == MachineType.STORAGE -> "Ёмкость ${s.storageCapacity(m)} → ${60 + 40 * (nl - 1) + 30 * s.researchLevel("storage")}"
            else -> null
        }
    }

    private fun statusOf(m: Machine): Pair<String, Int> {
        val s = engine.state
        if (m.type == MachineType.CONVEYOR) return Pair("Предметов: ${(m as Conveyor).items.size}", Pal.ACCENT2)
        if (m.type == MachineType.STORAGE) return Pair("Принимает товар", Pal.ACCENT2)
        if (m.working) return Pair("Работает", Pal.GOOD)
        if (m.type.power > 0 && s.powerFactor < 0.05f) return Pair("Нет питания", Pal.BAD)
        if (m.recipe != null && m.outCount >= m.recipe.outCap) return Pair("Выход заблокирован", Pal.BAD)
        if (m.type == MachineType.POWER_PLANT) return Pair("Простаивает: нет нагрузки", Pal.MUTED)
        return Pair("Ожидает ресурсы", Pal.MUTED)
    }

    override fun render() {
        val m = engine.selected
        if (m == null) {
            content.addView(ctx.label("Здание не выбрано. Нажмите на здание на карте, затем кнопку «ℹ».", 14f, Pal.MUTED), ctx.lp(10))
            return
        }
        val s = engine.state
        val eco = engine.economy

        val head = ctx.card()
        val ic = ctx.icon()
        ic.showMachine(m.type)
        head.addView(ctx.infoRow(ic, m.type.title, "ур. ${m.level}/5", Pal.ACCENT2, m.type.desc))
        head.addView(ctx.pips(m.level, 5, Pal.ACCENT), ctx.lp(10))
        content.addView(head, ctx.lp(4))

        val st = ctx.card()
        st.addView(ctx.label("📋 Характеристики", 16f, Pal.TEXT, true))
        val status = statusOf(m)
        st.addView(ctx.kv("Состояние", status.first, status.second), ctx.lp(8))
        st.addView(ctx.kv("Позиция", "${m.x + 1}, ${m.y + 1}"), ctx.lp(3))
        if (m.recipe != null || m.type == MachineType.CONVEYOR) st.addView(ctx.kv("Направление выхода", DIR_ARROWS[m.dir]), ctx.lp(3))

        val r = m.recipe
        if (r != null) {
            val sp = s.speedMult(m)
            st.addView(ctx.kv("Скорость", "${(sp * 100).toInt()}%"), ctx.lp(3))
            val perMin = 60f / r.cycle * sp * r.outQty
            st.addView(ctx.kv("Производство", "${f1(perMin)} × ${r.out.label}/мин", Pal.GOOD), ctx.lp(3))
            val sb = StringBuilder()
            if (r.inItems.isEmpty()) sb.append("—")
            for (k in r.inItems.indices) {
                if (k > 0) sb.append(" + ")
                sb.append(r.inItems[k].label).append('×').append(r.inQty[k])
            }
            sb.append(" → ").append(r.out.label).append('×').append(r.outQty)
            st.addView(ctx.kv("Рецепт", sb.toString()), ctx.lp(3))
            for (k in r.inItems.indices) {
                st.addView(ctx.kv("Вход: ${r.inItems[k].label}", "${m.inBuf[r.inItems[k].ordinal]} / ${r.cap(k)}"), ctx.lp(3))
            }
            st.addView(ctx.kv("Выходной буфер", "${m.outCount} / ${r.outCap}"), ctx.lp(3))
            val valueMin = perMin * eco.priceOf(r.out)
            st.addView(ctx.kv("Стоимость выпуска", "≈ ${money(valueMin.toLong())}/мин", Pal.ACCENT), ctx.lp(3))
        }
        if (m.type.power > 0) {
            st.addView(ctx.kv("⚡ Потребление", "${f1(s.machinePower(m))} МВт"), ctx.lp(3))
            st.addView(ctx.kv("Питание сети", "${(s.powerFactor * 100).toInt()}%", if (s.powerFactor < 0.95f) Pal.BAD else Pal.GOOD), ctx.lp(3))
        }
        when (m.type) {
            MachineType.POWER_PLANT -> {
                st.addView(ctx.kv("⚡ Выработка", "${f1(s.plantOutput(m))} МВт", Pal.GOOD), ctx.lp(3))
                st.addView(ctx.kv("Нагрузка сети", "${f1(s.powerUsed)} / ${f1(s.powerSupply)} МВт"), ctx.lp(3))
                st.addView(ctx.kv("Топливо (вся сеть)", usd(eco.fuelRate) + "/с", Pal.BAD), ctx.lp(3))
            }
            MachineType.RESEARCH_CENTER -> {
                st.addView(ctx.kv("Приток ОИ", f2(RP_RATE * m.level * s.powerFactor) + "/с", Pal.PURPLE), ctx.lp(3))
                st.addView(ctx.kv("Накоплено ОИ", "${s.researchPoints.toInt()}", Pal.PURPLE), ctx.lp(3))
            }
            MachineType.LOGISTICS -> {
                st.addView(ctx.kv("Рейс", "каждые 4 с, до ${6 * m.level} шт"), ctx.lp(3))
                st.addView(ctx.kv("Наценка продажи", "+${5 * m.level}%", Pal.GOOD), ctx.lp(3))
                st.addView(ctx.kv("В очереди", "${m.storedTotal()} / $LOGI_CAP"), ctx.lp(3))
            }
            MachineType.STORAGE -> {
                st.addView(ctx.kv("Заполнено", "${m.storedTotal()} / ${s.storageCapacity(m)}"), ctx.lp(3))
            }
            MachineType.CONVEYOR -> {
                st.addView(ctx.kv("Скорость ленты", "${f1(s.conveyorSpeed())} клетки/с"), ctx.lp(3))
            }
            else -> {}
        }
        st.addView(ctx.kv("🔧 Обслуживание", usd(s.maintenance(m) * 60f) + "/мин", Pal.BAD), ctx.lp(3))
        content.addView(st, ctx.lp(12))

        val up = ctx.card()
        val next = nextLevelText(m)
        if (next != null) {
            val cost = engine.upgradeCost(m)
            up.addView(ctx.label("⬆ Следующий уровень", 16f, Pal.TEXT, true))
            up.addView(ctx.label(next, 13f, Pal.GOOD, true), ctx.lp(4))
            up.addView(ctx.pillButton("Улучшить за ${money(cost)}", Pal.GOOD, s.money >= cost) {
                engine.upgradeSelected()
                refreshNow()
            }, ctx.lp(8))
        } else if (m.type != MachineType.CONVEYOR) {
            up.addView(ctx.label("Достигнут максимальный уровень ★", 14f, Pal.GOOD, true))
        } else {
            up.addView(ctx.label("Ленты улучшаются исследованием «Скоростные конвейеры».", 13f, Pal.MUTED))
        }
        up.addView(ctx.pillButton("🗑 Продать за ${money(m.type.refund.toLong())}", Pal.ACCENT, true) {
            engine.removeSelected()
            closeHost()
        }, ctx.lp(8))
        content.addView(up, ctx.lp(12))
    }
}
