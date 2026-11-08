package com.stepan.minifactory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SaveManager(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("minifactory_save", Context.MODE_PRIVATE)

    fun save(s: GameState) {
        try {
            prefs.edit().putString("state", toJson(s).toString()).apply()
        } catch (e: Exception) {
            // сохранение не должно ронять игру
        }
    }

    fun load(): GameState? {
        val str = prefs.getString("state", null) ?: return null
        return try {
            fromJson(JSONObject(str))
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().remove("state").apply()
    }

    private fun toJson(s: GameState): JSONObject {
        val o = JSONObject()
        o.put("v", 1)
        o.put("money", s.money)
        o.put("xp", s.xp)
        o.put("level", s.level)
        o.put("nid", s.nextMachineId)
        o.put("nonce", s.contractNonce)
        o.put("factory", s.currentFactory)
        o.put("grid", s.gridSize)
        o.put("auto", s.autoSell)
        o.put("fx", s.effectsOn)
        o.put("rp", s.researchPoints.toDouble())
        val sc = JSONArray()
        val sl = JSONArray()
        for (i in 0..2) {
            sc.put(s.staffCount[i])
            sl.put(s.staffLevel[i])
        }
        o.put("staffc", sc)
        o.put("staffl", sl)
        val tr = JSONObject()
        for ((fid, v) in s.territory) tr.put(fid.toString(), v)
        o.put("territory", tr)

        val st = JSONObject()
        st.put("earned", s.stats.totalEarned)
        st.put("spent", s.stats.totalSpent)
        st.put("parts", s.stats.partsProduced)
        st.put("metal", s.stats.metalProduced)
        st.put("ore", s.stats.oreMined)
        st.put("contracts", s.stats.contractsDone)
        st.put("sold", s.stats.itemsSold)
        st.put("gadgets", s.stats.gadgetsProduced)
        o.put("stats", st)

        val rs = JSONObject()
        for ((k, v) in s.research) rs.put(k, v)
        o.put("research", rs)

        val cs = JSONArray()
        for (c in s.contracts) {
            val co = JSONObject()
            co.put("id", c.id)
            co.put("item", c.item.name)
            co.put("amount", c.amount)
            co.put("reward", c.reward)
            co.put("xp", c.xp)
            co.put("cust", c.customer)
            co.put("tot", c.total.toDouble())
            co.put("tl", c.timeLeft.toDouble())
            co.put("del", c.delivered)
            cs.put(co)
        }
        o.put("contracts", cs)

        val ach = JSONArray()
        for (a in s.achievements) ach.put(a)
        o.put("ach", ach)

        val uf = JSONArray()
        for (f in s.unlockedFactories) uf.put(f)
        o.put("unlocked", uf)

        val ms = JSONArray()
        for (m in s.machines) ms.put(machineToJson(m))
        o.put("machines", ms)
        val lo = JSONObject()
        for ((fid, list) in s.layouts) {
            val arr = JSONArray()
            for (m in list) arr.put(machineToJson(m))
            lo.put(fid.toString(), arr)
        }
        o.put("layouts", lo)
        return o
    }

    private fun machineToJson(m: Machine): JSONObject {
        val mo = JSONObject()
        mo.put("id", m.id)
        mo.put("t", m.type.name)
        mo.put("x", m.x)
        mo.put("y", m.y)
        mo.put("d", m.dir)
        mo.put("lv", m.level)
        mo.put("tm", m.timer.toDouble())
        val ib = JSONArray()
        for (v in m.inBuf) ib.put(v)
        mo.put("inb", ib)
        mo.put("out", m.outCount)
        val sa = JSONArray()
        for (v in m.stored) sa.put(v)
        mo.put("st", sa)
        if (m is Conveyor) {
            val ia = JSONArray()
            for (bi in m.items) {
                val e = JSONArray()
                e.put(bi.type.name)
                e.put(bi.pos.toDouble())
                e.put(bi.inDir)
                ia.put(e)
            }
            mo.put("items", ia)
        }
        return mo
    }

    private fun machineFromJson(mo: JSONObject): Machine {
        val type = MachineType.valueOf(mo.getString("t"))
        val m = createMachine(mo.getInt("id"), type, mo.getInt("x"), mo.getInt("y"), mo.getInt("d") and 3)
        m.level = mo.optInt("lv", 1)
        m.timer = mo.optDouble("tm", 0.0).toFloat()
        val ib = mo.optJSONArray("inb")
        if (ib != null) {
            for (j in 0 until minOf(ib.length(), m.inBuf.size)) m.inBuf[j] = ib.getInt(j)
        } else {
            val legacy = mo.optInt("in", 0)
            val r = m.recipe
            if (r != null && r.inItems.isNotEmpty()) m.inBuf[r.inItems[0].ordinal] = legacy
        }
        m.outCount = mo.optInt("out", 0)
        val sa = mo.optJSONArray("st")
        if (sa != null) {
            for (j in 0 until minOf(sa.length(), m.stored.size)) m.stored[j] = sa.getInt(j)
        }
        if (m is Conveyor) {
            val ia = mo.optJSONArray("items")
            if (ia != null) {
                for (j in 0 until ia.length()) {
                    val e = ia.getJSONArray(j)
                    m.items.add(BeltItem(ItemType.valueOf(e.getString(0)), e.getDouble(1).toFloat(), e.getInt(2) and 3))
                }
            }
        }
        return m
    }

    private fun fromJson(o: JSONObject): GameState {
        val s = GameState()
        s.money = o.optLong("money", 400L)
        s.xp = o.optInt("xp", 0)
        s.level = o.optInt("level", 1)
        s.nextMachineId = o.optInt("nid", 1)
        s.contractNonce = o.optInt("nonce", 1)
        s.currentFactory = o.optInt("factory", 0)
        s.autoSell = o.optBoolean("auto", false)
        s.effectsOn = o.optBoolean("fx", true)
        s.researchPoints = o.optDouble("rp", 0.0).toFloat()
        val sc = o.optJSONArray("staffc")
        val sl = o.optJSONArray("staffl")
        for (i in 0..2) {
            if (sc != null && i < sc.length()) s.staffCount[i] = sc.getInt(i)
            if (sl != null && i < sl.length()) s.staffLevel[i] = maxOf(1, sl.getInt(i))
        }
        val tr = o.optJSONObject("territory")
        if (tr != null) {
            val tk = tr.keys()
            while (tk.hasNext()) {
                val k = tk.next()
                s.territory[k.toInt()] = tr.getInt(k)
            }
        }

        val st = o.optJSONObject("stats")
        if (st != null) {
            s.stats.totalEarned = st.optLong("earned", 0L)
            s.stats.totalSpent = st.optLong("spent", 0L)
            s.stats.partsProduced = st.optInt("parts", 0)
            s.stats.metalProduced = st.optInt("metal", 0)
            s.stats.oreMined = st.optInt("ore", 0)
            s.stats.contractsDone = st.optInt("contracts", 0)
            s.stats.itemsSold = st.optInt("sold", 0)
            s.stats.gadgetsProduced = st.optInt("gadgets", 0)
        }

        val rs = o.optJSONObject("research")
        if (rs != null) {
            val keys = rs.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                s.research[k] = rs.optInt(k, 0)
            }
        }

        val cs = o.optJSONArray("contracts")
        if (cs != null) {
            for (i in 0 until cs.length()) {
                val co = cs.getJSONObject(i)
                s.contracts.add(
                    Contract(
                        co.getInt("id"),
                        ItemType.valueOf(co.getString("item")),
                        co.getInt("amount"),
                        co.getInt("reward"),
                        co.getInt("xp"),
                        co.optString("cust", "Клиент"),
                        co.optDouble("tot", 600.0).toFloat(),
                        co.optDouble("tl", 600.0).toFloat(),
                        co.optInt("del", 0)
                    )
                )
            }
        }

        val ach = o.optJSONArray("ach")
        if (ach != null) for (i in 0 until ach.length()) s.achievements.add(ach.getString(i))

        val uf = o.optJSONArray("unlocked")
        if (uf != null) for (i in 0 until uf.length()) s.unlockedFactories.add(uf.getInt(i))

        s.setGrid(o.optInt("grid", 12))

        val ms = o.optJSONArray("machines")
        if (ms != null) {
            for (i in 0 until ms.length()) {
                val m = machineFromJson(ms.getJSONObject(i))
                if (!s.inBounds(m.x, m.y) || s.machineAt(m.x, m.y) != null) continue
                s.addMachine(m)
            }
        }
        val lo = o.optJSONObject("layouts")
        if (lo != null) {
            val keys = lo.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = lo.getJSONArray(k)
                val list = ArrayList<Machine>()
                for (i in 0 until arr.length()) list.add(machineFromJson(arr.getJSONObject(i)))
                s.layouts[k.toInt()] = list
            }
        }
        return s
    }
}
