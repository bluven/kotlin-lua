package lua.state

import lua.api.LuaType.*
import lua.number.*

fun typeOf(value: Any?) = when (value) {
    null -> LUA_TNIL
    is Boolean -> LUA_TBOOLEAN
    is Long, is Double -> LUA_TNUMBER
    is String -> LUA_TSTRING
    is LuaTable -> LUA_TTABLE
    is Closure -> LUA_TFUNCTION
    else -> throw Exception("Don't support ${value::class.simpleName}")
}

fun toBoolean(value: Any?) = when(value) {
    is Boolean -> value
    null -> false
    else -> true
}

// http://www.lua.org/manual/5.3/manual.html#3.4.3
fun toFloat(value: Any?) = when (value) {
    is Double -> value
    is Long -> value.toDouble()
    is String -> parseFloat(value)
    else -> null
}

// http://www.lua.org/manual/5.3/manual.html#3.4.3
fun toInteger(value: Any?) = when (value) {
    is Long -> value
    is Double -> if (isInteger(value)) value.toLong() else null
    is String -> toInteger(value)
    else -> null
}

private fun toInteger(s: String): Long? {
    val i = parseInteger(s)
    if (i != null) {
        return i
    }
    val f = parseFloat(s)
    return if (f != null && isInteger(f))  f!!.toLong()  else null
}