package lua.state

import lua.number.isInteger
import java.util.*


internal class LuaTable(nArr: Int, nRec: Int) {

    private var arr: MutableList<Any?>? = null
    private var map: MutableMap<Any?, Any>? = null

    init {
        if (nArr > 0) {
            arr = ArrayList(nArr)
        }

        if (nRec > 0) {
            map = HashMap(nRec)
        }
    }

    fun length() = if (arr == null) 0 else arr!!.size

    operator fun get(key: Any?): Any? {
        val key = floatToInteger(key)

        if (arr != null && key is Long) {
            val idx = key.toInt()
            if (idx >= 1 && idx <= arr!!.size) {
                return arr!![idx - 1]
            }
        }

        return if (map != null) map!![key] else null
    }

    fun put(key: Any?, value: Any?) {
        var key: Any? = key ?: throw Exception("table index is nil!")
        if (key is Double && key.isNaN()) {
            throw Exception("table index is NaN!")
        }

        key = floatToInteger(key)
        if (key is Long) {
            val idx = key.toInt()
            if (idx >= 1) {
                if (arr == null) {
                    arr = ArrayList()
                }

                val arrLen = arr!!.size
                if (idx <= arrLen) {
                    arr!!.set(idx - 1, value)
                    if (idx == arrLen && value == null) {
                        shrinkArray()
                    }
                    return
                }
                if (idx == arrLen + 1) {
                    if (map != null) {
                        map!!.remove(key)
                    }
                    if (value != null) {
                        arr!!.add(value)
                        expandArray()
                    }
                    return
                }
            }
        }

        if (value != null) {
            if (map == null) {
                map = HashMap()
            }
            map!![key] = value
        } else {
            if (map != null) {
                map!!.remove(key)
            }
        }
    }

    private fun floatToInteger(key: Any?): Any? {
        if (key is Double) {
            if (isInteger(key)) {
                return key.toLong()
            }
        }
        return key
    }

    private fun shrinkArray() {
        for (i in arr!!.indices.reversed()) {
            if (arr!![i] == null) {
                arr!!.removeAt(i)
            }
        }
    }

    private fun expandArray() {
        if (map != null) {
            var idx = arr!!.size + 1
            while (true) {
                val value = map!!.remove(idx.toLong())
                if (value != null) {
                    arr!!.add(value)
                } else {
                    break
                }
                idx++
            }
        }
    }

}