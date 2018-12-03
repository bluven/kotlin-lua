package lua.state

import lua.api.LuaType.*

fun typeOf(value: Any?) = when (value) {
    null -> LUA_TNIL
    is Boolean -> LUA_TBOOLEAN
    is Long, is Double -> LUA_TNUMBER
    is String -> LUA_TSTRING
    else -> throw Exception("Don't support ${value::class.simpleName}")
}

fun toBoolean(value: Any?) = when(value) {
    null -> false
    is Boolean -> value
    else -> true
}
