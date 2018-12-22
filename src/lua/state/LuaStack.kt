package lua.state

import lua.api.KFunction
import lua.api.LUA_REGISTRYINDEX
import lua.api.LuaState
import lua.binchunk.Prototype
import java.util.*

internal data class Closure(
    val proto: Prototype? = null,
    val kFunc: KFunction? = null
)


internal class LuaStack {
    val slots = ArrayList<Any?>()
    val top: Int
        inline get() = slots.size

    var state: LuaStateImpl? = null
    var closure: Closure? = null
    var varargs: List<Any?>? = null
    var pc: Int = 0
    var prev: LuaStack? = null

    fun push(value: Any?) {
        if (slots.size > 10000) {
            throw StackOverflowError("")
        }

        slots.add(value)
    }

    fun pop() = slots.removeAt(slots.lastIndex)

    fun pushN(vals: List<Any?>, n: Int) {
        val nVals = vals.size
        val n = if (n < 0) nVals else n

        repeat(n) {
            push(if (it < nVals) vals[it] else null)
        }
    }

    fun popN(n: Int): List<Any?> {
        val vals = ArrayList<Any?>(n)

        repeat(n) {
            vals.add(pop())
        }

        return vals.reversed()
    }

    fun absIndex(idx: Int) = when {
        idx <= LUA_REGISTRYINDEX -> idx
        idx >= 0 -> idx
        else -> idx + top + 1
    }

    fun isValid(idx: Int) = idx == LUA_REGISTRYINDEX || absIndex(idx) in 1..top

    fun get(idx: Int): Any? {

        if (idx == LUA_REGISTRYINDEX) {
            return state!!.registry
        }

        val absIdx = absIndex(idx)

        return if (isValid(idx)) {
            slots[absIdx - 1]
        } else {
            null
        }
    }

    fun set(idx: Int, value: Any?) {
        if (idx == LUA_REGISTRYINDEX) {
            state!!.registry = value as LuaTable
        }

        val absIdx = absIndex(idx)

        if (isValid(idx)) {
            slots[absIdx - 1] = value
        } else {
            throw Exception("invalid index!")
        }
    }

    fun reverse(from: Int, to: Int) {
        slots.subList(from, to + 1).reverse()
    }
}
