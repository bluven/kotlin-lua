package lua.state

import java.util.*

internal class LuaStack {
    val slots = ArrayList<Any?>()

    val top: Int
        inline get() = slots.size

    fun push(value: Any?) {
        if (slots.size > 10000) {
            throw StackOverflowError("")
        }

        slots.add(value)
    }

    fun pop() = slots.removeAt(slots.lastIndex)

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