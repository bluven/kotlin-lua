package lua.state

import lua.binchunk.Prototype
import java.util.*

internal class Closure(val proto: Prototype)


internal class LuaStack {
    val slots = ArrayList<Any?>()

    val top: Int
        inline get() = slots.size

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

    fun absIndex(idx: Int) = if (idx >= 0) idx else idx + top + 1

    fun isValid(idx: Int) = absIndex(idx) in 1..top

    fun get(idx: Int): Any? {
        val absIdx = absIndex(idx)

        return if (isValid(idx)) {
            slots[absIdx - 1]
        } else {
            null
        }
    }

    fun set(idx: Int, value: Any?) {
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