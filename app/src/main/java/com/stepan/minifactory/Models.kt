package com.stepan.minifactory

import android.graphics.Color

val DX = intArrayOf(1, 0, -1, 0)
val DY = intArrayOf(0, 1, 0, -1)
val DIR_ARROWS = arrayOf("↘", "↙", "↖", "↗")

enum class ItemType(val label: String, val color: Int, val basePrice: Int, val info: String) {
    ORE("Руда", Color.rgb(160, 112, 72), 2, "Сырьё. Добывается Extractor и Шахтой. Можно переплавить в металл или продать."),
    METAL("Металл", Color.rgb(176, 196, 222), 6, "Выплавляется в Smelter или на Перерабатывающем заводе из руды."),
    PART("Деталь", Color.rgb(255, 190, 40), 14, "Компонент. Делается в Part Maker из металла. Нужен для сборки изделий."),
    GADGET("Изделие", Color.rgb(70, 200, 240), 48, "Готовая продукция. Собирается в Сборочном цехе из 2 деталей и металла. Самый дорогой товар.")
}

enum class MachineType(
    val title: String,
    val icon: String,
    val cost: Int,
    val desc: String,
    val io: String,
    val power: Int,
    val unlockLevel: Int,
    val unlockStage: Int,
    val unlockAch: String?
) {
    EXTRACTOR("Ore Extractor", "⛏", 150, "Бур качает руду из-под земли и выдаёт её через оранжевый порт.", "Ничего → Руда (1 шт / 2.2 с)", 0, 1, 0, null),
    MINE("Шахта", "🏔", 700, "Глубокая шахта с подъёмником: добывает вдвое больше руды за цикл. Требует электричество.", "Электричество → Руда ×2 (за 2.6 с)", 4, 3, 0, null),
    SMELTER("Smelter", "🔥", 250, "Печь с трубой. Принимает руду с ленты и выплавляет металл.", "Руда → Металл (1 шт / 2.6 с)", 0, 1, 0, null),
    REFINERY("Перерабатывающий завод", "🏭", 900, "Современный завод с ёмкостями: из 2 руды получает 3 металла. Требует электричество.", "Руда ×2 → Металл ×3 (за 3.6 с)", 5, 4, 1, null),
    PART_MAKER("Part Maker", "🤖", 400, "Робот-манипулятор штампует из металла детали.", "Металл → Деталь (1 шт / 3.2 с)", 0, 1, 0, null),
    ASSEMBLY("Сборочный цех", "🔧", 1500, "Цех со сборочным краном: делает готовые изделия из деталей и металла. Требует электричество.", "Деталь ×2 + Металл → Изделие (за 5.5 с)", 6, 6, 1, null),
    CONVEYOR("Conveyor", "➡", 10, "Лента возит предметы по стрелке. Поворачивайте её кнопкой «Поворот».", "Перевозит любые предметы вперёд", 0, 1, 0, null),
    STORAGE("Storage", "📦", 200, "Склад принимает всё подряд. Продажа и заказы берут товар отсюда.", "Хранит предметы, ёмкость растёт с уровнем", 0, 1, 0, null),
    POWER_PLANT("Электростанция", "⚡", 800, "Вырабатывает электричество для зданий. Топливо стоит денег, но сжигается только нужное количество.", "Даёт 10 МВт (+6 МВт за уровень)", 0, 2, 0, null),
    RESEARCH_CENTER("Исследовательский центр", "🔬", 1200, "Вырабатывает очки исследований (ОИ) для продвинутых технологий. Требует электричество.", "Электричество → ОИ (0.4/с за уровень)", 4, 4, 0, "earn5k"),
    LOGISTICS("Центр логистики", "🚚", 1000, "Принимает товар с ленты, закрывает заказы клиентов и продаёт остаток с наценкой. Требует электричество.", "Товар → заказы / продажа (рейс каждые 4 с)", 3, 5, 0, "contracts5");

    val refund: Int get() = cost / 2
}

enum class Mode { IDLE, PLACE, MOVE }

enum class StaffType(val title: String, val icon: String, val hireBase: Int, val desc: String) {
    WORKER("Рабочие", "👷", 120, "Каждый рабочий ускоряет все станки на 4%."),
    ENGINEER("Инженеры", "🔧", 300, "Каждый инженер снижает расходы на энергию и обслуживание на 3% и даёт шанс бонусной продукции."),
    MANAGER("Менеджеры", "👔", 500, "Каждый менеджер повышает цены продажи и награды за заказы на 3%.")
}

class FactoryDef(
    val id: Int,
    val title: String,
    val gridSize: Int,
    val implemented: Boolean,
    val needMoney: Long,
    val needLevel: Int,
    val needContracts: Int,
    val needAchievements: Int,
    val bonus: Float,
    val desc: String,
    val specTitle: String,
    val specItem: ItemType?,
    val specPrice: Float,
    val specTypes: Set<MachineType>,
    val specSpeed: Float
)

class Contract(
    val id: Int,
    val item: ItemType,
    val amount: Int,
    val reward: Int,
    val xp: Int,
    val customer: String,
    val total: Float,
    var timeLeft: Float,
    var delivered: Int = 0
)

class Stats {
    var totalEarned = 0L
    var totalSpent = 0L
    var partsProduced = 0
    var metalProduced = 0
    var oreMined = 0
    var gadgetsProduced = 0
    var contractsDone = 0
    var itemsSold = 0
}

interface ProductionListener {
    fun onProduced(m: Machine, item: ItemType)
}

interface UiListener {
    fun onMessage(text: String)
    fun onStateChanged()
}

class ResearchDef(
    val id: String,
    val icon: String,
    val title: String,
    val desc: String,
    val maxLevel: Int,
    val baseCost: Int,
    val growth: Double,
    val usesRp: Boolean = false
) {
    fun costAt(level: Int): Long = (baseCost * Math.pow(growth, level.toDouble())).toLong()
}

object ResearchCatalog {
    val all = listOf(
        ResearchDef("speed", "⚙", "Скорость производства", "+10% к скорости всех станков за уровень", 10, 300, 1.55),
        ResearchDef("belt", "➡", "Скоростные конвейеры", "+10% к скорости лент за уровень", 8, 200, 1.5),
        ResearchDef("storage", "📦", "Вместимость склада", "+30 к ёмкости каждого склада за уровень", 10, 250, 1.45),
        ResearchDef("price", "💲", "Выгодные цены", "+5% к цене продажи за уровень", 10, 400, 1.6),
        ResearchDef("contract", "📜", "Контрактный отдел", "+10% к наградам заказов за уровень", 5, 500, 1.7),
        ResearchDef("grid", "🔌", "Умная электросеть", "+10% выработки всех электростанций за уровень", 5, 25, 1.6, true),
        ResearchDef("lean", "📉", "Бережливое производство", "−5% к расходам на обслуживание за уровень", 5, 30, 1.6, true),
        ResearchDef("auto", "🤖", "Автоматизация", "+8% к скорости всех станков за уровень", 5, 40, 1.7, true),
        ResearchDef("market", "📣", "Маркетинг", "+4% к ценам продажи за уровень", 5, 35, 1.65, true),
        ResearchDef("orders", "🤝", "Корпоративные клиенты", "+10% к наградам заказов и дополнительный слот заказов за 2 уровня", 5, 45, 1.7, true)
    )
}
