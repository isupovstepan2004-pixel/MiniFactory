package com.stepan.minifactory

const val RP_RATE = 0.4f
const val LOGI_CAP = 30
val WAGES = floatArrayOf(0.06f, 0.14f, 0.25f)

fun GameState.staffPower(i: Int): Float = staffCount[i] * (1f + 0.25f * (staffLevel[i] - 1))
fun GameState.workerMult(): Float = 1f + 0.04f * staffPower(0)
fun GameState.efficiency(): Float = 0.03f * staffPower(1)
fun GameState.costFactor(): Float = 1f / (1f + efficiency())
fun GameState.bonusChance(): Float = minOf(0.4f, efficiency() * 0.5f)
fun GameState.managerMult(): Float = 1f + 0.03f * staffPower(2)
fun GameState.staffCap(): Int = 3 + 3 * stage()
fun GameState.staffTotal(): Int = staffCount[0] + staffCount[1] + staffCount[2]

fun GameState.factoryDef(): FactoryDef = Factories.all[currentFactory.coerceIn(0, Factories.all.size - 1)]

fun GameState.factorySpeed(t: MachineType): Float {
    val f = factoryDef()
    return if (f.specTypes.contains(t)) f.specSpeed else 1f
}

fun GameState.speedMult(m: Machine): Float {
    var v = (1f + 0.25f * (m.level - 1)) *
        (1f + 0.10f * researchLevel("speed") + 0.08f * researchLevel("auto")) *
        workerMult() * factorySpeed(m.type)
    if (m.type.power > 0) v *= powerFactor
    return v
}

fun GameState.machinePower(m: Machine): Float = m.type.power * (1f + 0.2f * (m.level - 1)) * costFactor()

fun GameState.plantOutput(m: Machine): Float = (10f + 6f * (m.level - 1)) * (1f + 0.10f * researchLevel("grid"))

fun GameState.maintenance(m: Machine): Float {
    val lean = maxOf(0.4f, 1f - 0.05f * researchLevel("lean"))
    return m.type.cost * 0.0006f * (1f + 0.3f * (m.level - 1)) * costFactor() * lean
}

fun GameState.salaryPerSec(): Float {
    var s = 0f
    for (i in 0..2) s += staffCount[i] * WAGES[i] * (1f + 0.2f * (staffLevel[i] - 1))
    return s
}

fun GameState.researchRate(): Float {
    var r = 0f
    for (m in machines) if (m.type == MachineType.RESEARCH_CENTER) r += RP_RATE * m.level * powerFactor
    return r
}
