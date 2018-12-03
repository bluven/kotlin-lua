package lua.debug

import lua.api.LuaState
import lua.api.LuaType.*
import lua.state.LuaStateImpl

fun testLuaState() {
    val ls = LuaStateImpl()

    ls.pushBoolean(true)
    printStack(ls)
    ls.pushInteger(10)
    printStack(ls)
    ls.pushNil()
    printStack(ls)
    ls.pushString("hello")
    printStack(ls)
    ls.pushValue(-4)
    printStack(ls)
    ls.replace(3)
    printStack(ls)
    ls.top = 6
    printStack(ls)
    ls.remove(-3)
    printStack(ls)
    ls.top = -5
    printStack(ls)
}

private fun printStack(ls: LuaState) {
    for (i in 1..ls.top) {
        val t = ls.type(i)
        when (t) {
            LUA_TBOOLEAN -> System.out.printf("[%b]", ls.toBoolean(i))
            LUA_TNUMBER -> if (ls.isInteger(i)) {
                System.out.printf("[%d]", ls.toInteger(i))
            } else {
                System.out.printf("[%f]", ls.toNumber(i))
            }
            LUA_TSTRING -> System.out.printf("[\"%s\"]", ls.toString(i))
            else // other values
            -> System.out.printf("[%s]", ls.typeName(t))
        }
    }
    println()
}