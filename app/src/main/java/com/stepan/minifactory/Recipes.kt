package com.stepan.minifactory

class Recipe(
    val inItems: Array<ItemType>,
    val inQty: IntArray,
    val out: ItemType,
    val outQty: Int,
    val cycle: Float
) {
    fun cap(i: Int): Int = maxOf(4, inQty[i] * 3)
    val outCap: Int get() = maxOf(3, outQty * 2)
}

object Recipes {
    private val map = HashMap<MachineType, Recipe>()

    init {
        map[MachineType.EXTRACTOR] = Recipe(arrayOf<ItemType>(), intArrayOf(), ItemType.ORE, 1, 2.2f)
        map[MachineType.MINE] = Recipe(arrayOf<ItemType>(), intArrayOf(), ItemType.ORE, 2, 2.6f)
        map[MachineType.SMELTER] = Recipe(arrayOf(ItemType.ORE), intArrayOf(1), ItemType.METAL, 1, 2.6f)
        map[MachineType.REFINERY] = Recipe(arrayOf(ItemType.ORE), intArrayOf(2), ItemType.METAL, 3, 3.6f)
        map[MachineType.PART_MAKER] = Recipe(arrayOf(ItemType.METAL), intArrayOf(1), ItemType.PART, 1, 3.2f)
        map[MachineType.ASSEMBLY] = Recipe(arrayOf(ItemType.PART, ItemType.METAL), intArrayOf(2, 1), ItemType.GADGET, 1, 5.5f)
    }

    fun of(t: MachineType): Recipe? = map[t]
}

object Factories {
    val all = listOf(
        FactoryDef(
            0, "Малая фабрика", 12, true, 0L, 1, 0, 0, 1.0f,
            "Маленький гараж — отличное место для старта.",
            "Сырьё", ItemType.ORE, 1.3f, setOf(MachineType.EXTRACTOR, MachineType.MINE), 1.15f
        ),
        FactoryDef(
            1, "Производственный комплекс", 14, true, 4000L, 4, 4, 2, 1.15f,
            "Просторный цех с большой сеткой.",
            "Металлургия", ItemType.METAL, 1.3f, setOf(MachineType.SMELTER, MachineType.REFINERY), 1.15f
        ),
        FactoryDef(
            2, "Промышленная зона", 16, true, 15000L, 7, 10, 4, 1.3f,
            "Целый квартал заводов с отдельной электросетью.",
            "Компоненты", ItemType.PART, 1.3f, setOf(MachineType.PART_MAKER), 1.2f
        ),
        FactoryDef(
            3, "Большой завод", 18, true, 50000L, 10, 20, 6, 1.5f,
            "Крупный завод полного цикла.",
            "Сборка изделий", ItemType.GADGET, 1.3f, setOf(MachineType.ASSEMBLY), 1.2f
        ),
        FactoryDef(
            4, "Мегапромышленный комплекс", 20, true, 150000L, 13, 35, 8, 1.8f,
            "Вершина карьеры: универсальный гигант.",
            "Универсал", null, 1.12f,
            setOf(
                MachineType.EXTRACTOR, MachineType.MINE, MachineType.SMELTER, MachineType.REFINERY,
                MachineType.PART_MAKER, MachineType.ASSEMBLY
            ),
            1.1f
        )
    )
}

class Achievement(val id: String, val title: String, val desc: String, val check: (GameState) -> Boolean)

object Achievements {
    val all = listOf(
        Achievement("parts10", "Первая партия", "Произведите 10 деталей") { it.stats.partsProduced >= 10 },
        Achievement("parts100", "Мастер деталей", "Произведите 100 деталей") { it.stats.partsProduced >= 100 },
        Achievement("earn5k", "Первые 5000", "Заработайте суммарно 5000 монет") { it.stats.totalEarned >= 5000L },
        Achievement("earn50k", "Крупный бизнес", "Заработайте суммарно 50000 монет") { it.stats.totalEarned >= 50000L },
        Achievement("sold200", "Торговец", "Продайте 200 единиц продукции") { it.stats.itemsSold >= 200 },
        Achievement("contracts5", "Надёжный партнёр", "Выполните 5 заказов клиентов") { it.stats.contractsDone >= 5 },
        Achievement("orders20", "Любимый поставщик", "Выполните 20 заказов клиентов") { it.stats.contractsDone >= 20 },
        Achievement("builder", "Строитель", "Постройте 15 объектов") { it.machines.size >= 15 },
        Achievement("upgrade3", "Тюнинг", "Улучшите здание до уровня 3") { s -> s.machines.any { it.level >= 3 } },
        Achievement("power1", "Да будет свет", "Постройте электростанцию") { it.countOf(MachineType.POWER_PLANT) > 0 },
        Achievement("gadget1", "Первое изделие", "Соберите готовое изделие") { it.stats.gadgetsProduced >= 1 },
        Achievement("gadgets50", "Конвейер изделий", "Соберите 50 изделий") { it.stats.gadgetsProduced >= 50 },
        Achievement("staff5", "Команда", "Наймите 5 сотрудников") { it.staffCount[0] + it.staffCount[1] + it.staffCount[2] >= 5 },
        Achievement("research1", "Учёный", "Изучите технологию за очки исследований") { s ->
            ResearchCatalog.all.any { it.usesRp && s.researchLevel(it.id) > 0 }
        },
        Achievement("stage2", "Растём", "Откройте вторую фабрику") { it.unlockedFactories.size >= 2 }
    )

    fun title(id: String): String {
        for (a in all) if (a.id == id) return a.title
        return id
    }
}

object Unlocks {
    fun reason(t: MachineType, s: GameState): String? {
        if (s.level < t.unlockLevel) return "Нужен уровень ${t.unlockLevel}"
        if (s.stage() < t.unlockStage) return "Нужно открыть: ${Factories.all[t.unlockStage].title}"
        val a = t.unlockAch
        if (a != null && !s.achievements.contains(a)) return "Нужно достижение: ${Achievements.title(a)}"
        return null
    }
}

fun FactoryDef.specText(): String {
    val si = specItem
    val what = if (si == null) "цены на всё ×$specPrice" else "цены на ${si.label.lowercase()} ×$specPrice"
    val names = StringBuilder()
    for (t in specTypes) {
        if (names.isNotEmpty()) names.append(", ")
        names.append(t.title)
    }
    val pct = ((specSpeed - 1f) * 100).toInt()
    return "$specTitle: $what; быстрее на $pct%: $names"
}
