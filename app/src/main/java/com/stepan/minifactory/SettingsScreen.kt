package com.stepan.minifactory

import android.content.Context

class SettingsScreen(ctx: Context, engine: GameEngine, private val closeHost: () -> Unit) : GameScreen(ctx, engine) {
    override val title = "⚙ Настройки"
    private var confirmReset = false

    override fun signature(): String = "${engine.state.effectsOn}|$confirmReset"

    override fun render() {
        val s = engine.state

        val help = ctx.card()
        help.addView(ctx.label("📖 Как играть", 16f, Pal.TEXT, true))
        val steps = arrayOf(
            "1. Добыча: Ore Extractor или Шахта → лента везёт руду дальше.",
            "2. Переработка: Smelter или Перерабатывающий завод делают металл.",
            "3. Компоненты: Part Maker делает детали из металла.",
            "4. Сборка: Сборочный цех делает дорогие изделия из деталей и металла.",
            "5. Продажа: в Storage со склада, через Центр логистики или по заказам клиентов.",
            "⚡ Шахта, завод, цех, центры исследований и логистики требуют электричество — стройте Электростанцию. Топливо стоит денег.",
            "🔧 Все здания требуют обслуживания, персонал получает зарплату. Следите за прибылью в секунду на вкладке «Линии».",
            "👷 Рабочие ускоряют станки, инженеры снижают расходы, менеджеры повышают цены.",
            "🗺 Расширяйте территорию и открывайте новые фабрики во вкладке «Карьера».",
            "Подсказка: оранжевый порт здания — это выход. Кнопка ⟳ поворачивает здание, ℹ открывает его характеристики."
        )
        for (t in steps) help.addView(ctx.label(t, 12.5f, Pal.MUTED), ctx.lp(5))
        content.addView(help, ctx.lp(4))

        val fx = ctx.card()
        val ic = ctx.icon()
        ic.showEmoji("✨")
        fx.addView(ctx.infoRow(ic, "Визуальные эффекты", if (s.effectsOn) "ВКЛ" else "ВЫКЛ", if (s.effectsOn) Pal.GOOD else Pal.MUTED,
            "Дым, искры и пыль. Отключите ради максимальной производительности."))
        fx.addView(ctx.pillButton(if (s.effectsOn) "Выключить эффекты" else "Включить эффекты", if (s.effectsOn) Pal.SLATE else Pal.GOOD, true) {
            s.effectsOn = !s.effectsOn
            refreshNow()
        }, ctx.lp(10))
        content.addView(fx, ctx.lp(12))

        val sv = ctx.card()
        val ic2 = ctx.icon()
        ic2.showEmoji("💾")
        sv.addView(ctx.infoRow(ic2, "Сохранение", null, Pal.TEXT, "Игра сохраняется автоматически каждые 10 секунд и при выходе."))
        sv.addView(ctx.pillButton("Сохранить сейчас", Pal.ACCENT2, true) {
            engine.saveNow()
            engine.say("💾 Сохранено")
        }, ctx.lp(10))
        content.addView(sv, ctx.lp(12))

        val rs = ctx.card()
        val ic3 = ctx.icon()
        ic3.showEmoji("🗑")
        rs.addView(ctx.infoRow(ic3, "Сброс прогресса", null, Pal.TEXT, "Удалит все станки, деньги и фабрики и начнёт новую игру."))
        val text = if (confirmReset) "Точно сбросить? Нажмите ещё раз" else "Сбросить игру"
        rs.addView(ctx.pillButton(text, Pal.BAD, true) {
            if (confirmReset) {
                confirmReset = false
                engine.resetGame()
                engine.say("Начата новая игра")
                closeHost()
            } else {
                confirmReset = true
                refreshNow()
            }
        }, ctx.lp(10))
        content.addView(rs, ctx.lp(12))

        content.addView(ctx.label("MiniFactory 2.0 — Production & Progression • работает полностью офлайн", 12f, Pal.MUTED), ctx.lp(14))
    }
}
