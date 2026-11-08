package com.stepan.minifactory

class GameState {
    var money = 400L
    var xp = 0
    var level = 1
    val stats = Stats()
    val machines = ArrayList<Machine>()
    var nextMachineId = 1
    val research = HashMap<String, Int>()
    val contracts = ArrayList<Contract>()
    var contractNonce = 1
    val achievements = HashSet<String>()
    var currentFactory = 0
    val unlockedFactories = HashSet<Int>()
    var autoSell = false
    var effectsOn = true
    var gridSize = 12

    var researchPoints = 0f
    val staffCount = IntArray(3)
    val staffLevel = intArrayOf(1, 1, 1)
    val territory = HashMap<Int, Int>()

    var powerSupply = 0f
    var powerDemand = 0f
    var powerUsed = 0f
    var powerFactor = 1f
    val rng = java.util.Random()

    val layouts = HashMap<Int, ArrayList<Machine>>()
    private var grid: Array<Machine?> = arrayOfNulls(144)

    init {
        unlockedFactories.add(0)
    }

    fun setGrid(size: Int) {
        gridSize = size
        grid = arrayOfNulls(size * size)
        for (m in machines) {
            if (inBounds(m.x, m.y)) grid[m.y * size + m.x] = m
        }
    }

    fun inBounds(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < gridSize && y < gridSize

    fun territorySize(): Int = minOf(territory[currentFactory] ?: 99, gridSize)

    fun isBuildable(x: Int, y: Int): Boolean {
        val t = territorySize()
        return inBounds(x, y) && x < t && y < t
    }

    fun stage(): Int {
        var m = 0
        for (f in unlockedFactories) if (f > m) m = f
        return m
    }

    fun machineAt(x: Int, y: Int): Machine? = if (inBounds(x, y)) grid[y * gridSize + x] else null

    fun addMachine(m: Machine) {
        machines.add(m)
        grid[m.y * gridSize + m.x] = m
    }

    fun removeMachine(m: Machine) {
        machines.remove(m)
        if (inBounds(m.x, m.y) && grid[m.y * gridSize + m.x] === m) grid[m.y * gridSize + m.x] = null
    }

    fun moveMachine(m: Machine, nx: Int, ny: Int) {
        if (inBounds(m.x, m.y) && grid[m.y * gridSize + m.x] === m) grid[m.y * gridSize + m.x] = null
        m.x = nx
        m.y = ny
        grid[ny * gridSize + nx] = m
    }

    fun switchTo(id: Int, size: Int) {
        layouts[currentFactory] = ArrayList(machines)
        machines.clear()
        val next = layouts.remove(id)
        if (next != null) machines.addAll(next)
        currentFactory = id
        setGrid(size)
    }

    fun researchLevel(id: String): Int = research[id] ?: 0

    fun conveyorSpeed(): Float = 1.3f * (1f + 0.10f * researchLevel("belt"))

    fun storageCapacity(m: Machine): Int = 60 + 40 * (m.level - 1) + 30 * researchLevel("storage")

    fun totalStored(item: ItemType): Int {
        var s = 0
        for (m in machines) if (m.type == MachineType.STORAGE) s += m.stored[item.ordinal]
        return s
    }

    fun takeFromStorages(item: ItemType, count: Int): Int {
        var left = count
        for (m in machines) {
            if (left <= 0) break
            if (m.type == MachineType.STORAGE) {
                val t = minOf(left, m.stored[item.ordinal])
                m.stored[item.ordinal] -= t
                left -= t
            }
        }
        return count - left
    }

    fun storageCount(): Int = countOf(MachineType.STORAGE)

    fun totalCapacity(): Int {
        var n = 0
        for (m in machines) if (m.type == MachineType.STORAGE) n += storageCapacity(m)
        return n
    }

    fun totalStoredAll(): Int {
        var n = 0
        for (m in machines) if (m.type == MachineType.STORAGE) n += m.storedTotal()
        return n
    }

    fun countOf(type: MachineType): Int {
        var n = 0
        for (m in machines) if (m.type == type) n++
        return n
    }
}
