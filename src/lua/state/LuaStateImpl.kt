package lua.state

import lua.api.LuaState
import lua.api.ArithOp
import lua.api.ArithOp.*
import lua.api.CmpOp
import lua.api.CmpOp.*
import lua.api.LuaType
import lua.api.LuaType.*

class LuaStateImpl : LuaState {

    private val stack = LuaStack()

    /* basic stack manipulation */

    override var top: Int
        get() = stack.top
        set(idx) {
            val newTop = stack.absIndex(idx)
            if (newTop < 0) {
                throw RuntimeException("stack underflow!")
            }

            val n = stack.top - newTop
            if (n > 0) {
                for (i in 0 until n) {
                    stack.pop()
                }
            } else if (n < 0) {
                for (i in 0 downTo n + 1) {
                    stack.push(null)
                }
            }
        }

    override fun absIndex(idx: Int): Int {
        return stack.absIndex(idx)
    }

    override fun checkStack(n: Int): Boolean {
        return true // TODO
    }

    override fun pop(n: Int) {
        for (i in 0 until n) {
            stack.pop()
        }
    }

    override fun copy(fromIdx: Int, toIdx: Int) {
        stack.set(toIdx, stack.get(fromIdx))
    }

    override fun pushValue(idx: Int) {
        stack.push(stack.get(idx))
    }

    override fun replace(idx: Int) {
        stack.set(idx, stack.pop())
    }

    override fun insert(idx: Int) {
        rotate(idx, 1)
    }

    override fun remove(idx: Int) {
        rotate(idx, -1)
        pop(1)
    }

    override fun rotate(idx: Int, n: Int) {
        val t = stack.top - 1            /* end of stack segment being rotated */
        val p = stack.absIndex(idx) - 1    /* start of segment */
        val m = if (n >= 0) t - n else p - n - 1 /* end of prefix */

        stack.reverse(p, m)     /* reverse the prefix with length 'n' */
        stack.reverse(m + 1, t) /* reverse the suffix */
        stack.reverse(p, t)     /* reverse the entire segment */
    }

    /* access functions (stack -> Go); */

    override fun typeName(tp: LuaType) = when (tp) {
        LUA_TNONE -> "no value"
        LUA_TNIL -> "nil"
        LUA_TBOOLEAN -> "boolean"
        LUA_TNUMBER -> "number"
        LUA_TSTRING -> "string"
        LUA_TTABLE -> "table"
        LUA_TFUNCTION -> "function"
        LUA_TTHREAD -> "thread"
        else -> "userdata"
    }

    override fun type(idx: Int) = if (stack.isValid(idx)) typeOf(stack.get(idx)) else LUA_TNONE

    override fun isNone(idx: Int) = type(idx) === LUA_TNONE

    override fun isNil(idx: Int) = type(idx) === LUA_TNIL

    override fun isNoneOrNil(idx: Int): Boolean {
        val t = type(idx)
        return t === LUA_TNONE || t === LUA_TNIL
    }

    override fun isBoolean(idx: Int) = type(idx) === LUA_TBOOLEAN

    override fun isInteger(idx: Int) = stack.get(idx) is Long

    override fun isNumber(idx: Int) = toNumberX(idx) != null

    override fun isString(idx: Int): Boolean {
        val t = type(idx)
        return t === LUA_TSTRING || t === LUA_TNUMBER
    }

    override fun isTable(idx: Int) = type(idx) === LUA_TTABLE

    override fun isThread(idx: Int) = type(idx) === LUA_TTHREAD

    override fun isFunction(idx: Int) = type(idx) === LUA_TFUNCTION

    override fun toBoolean(idx: Int) = toBoolean(stack.get(idx))

    override fun toInteger(idx: Int) = toIntegerX(idx) ?: 0

    override fun toIntegerX(idx: Int): Long? {
        val value = stack.get(idx)
        return if (value is Long) value else null
    }

    override fun toNumber(idx: Int) = toNumberX(idx) ?: 0 as Double

    override fun toNumberX(idx: Int): Double? {
        val value = stack.get(idx)

        return when (value) {
            is Double -> value
            is Long -> value.toDouble()
            else -> null
        }
    }

    override fun toString(idx: Int): String? {
        val value = stack.get(idx)

        return when(value) {
            is String -> value
            is Long, is Double -> value.toString()
            else -> null
        }
    }

    /* push functions (Go -> stack); */

    override fun pushNil() {
        stack.push(null)
    }

    override fun pushBoolean(b: Boolean) {
        stack.push(b)
    }

    override fun pushInteger(n: Long) {
        stack.push(n)
    }

    override fun pushNumber(n: Double) {
        stack.push(n)
    }

    override fun pushString(s: String) {
        stack.push(s)
    }

    override fun arith(op: ArithOp) {
        val b = stack.pop()
        val a = if (op != LUA_OPUNM && op != LUA_OPBNOT) {
            stack.pop()
        } else {
            b
        }

        val result = arith(a, b, op)
        if (result != null) {
            stack.push(result)
        } else {
            throw Exception("arithmetic error!")
        }
    }

    override fun compare(idx1: Int, idx2: Int, op: CmpOp): Boolean {
        if (!stack.isValid(idx1) || !stack.isValid(idx2)) {
            return false
        }

        val a = stack.get(idx1)
        val b = stack.get(idx2)

        return when (op) {
            LUA_OPEQ -> eq(a, b)
            LUA_OPLT -> lt(a!!, b!!)
            LUA_OPLE -> le(a!!, b!!)
        }
    }

    override fun len(idx: Int) {
        val value = stack.get(idx)

        if (value is String) {
            pushInteger(value.length.toLong())
        } else {
            throw Exception("length error!")
        }
    }

    override fun concat(n: Int) {
        if (n == 0) {
            stack.push("")
        } else if (n >= 2) {

            repeat(n - 1) {
                if (!isString(-1) || !isString(-2)) {
                    throw Exception("concatenation error!")
                }

                val s2 = toString(-1)
                val s1 = toString(-2)
                pop(2)
                pushString(s1 + s2)
            }
        }

        // n == 1, do nothing
    }

}
