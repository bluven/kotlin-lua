package lua.debug

import lua.api.LuaState
import lua.api.LuaType.*
import lua.api.ArithOp.*
import lua.api.CmpOp.*
import lua.state.LuaStateImpl

fun testOp() {
    val ls = LuaStateImpl()
    ls.pushInteger(1)
    ls.pushString("2.0")
    ls.pushString("3.0")
    ls.pushNumber(4.0)
    printStack(ls)

    ls.arith(LUA_OPADD)
    printStack(ls)
    ls.arith(LUA_OPBNOT)
    printStack(ls)
    ls.len(2)
    printStack(ls)
    ls.concat(3)
    printStack(ls)
    ls.pushBoolean(ls.compare(1, 2, LUA_OPEQ))
    printStack(ls)
}

private fun printStack(ls: LuaState) {
    val top = ls.top

    repeat(top) {
        val i = it + 1
        val t = ls.type(i)

        when (t) {
            LUA_TBOOLEAN -> System.out.printf("[%b]", ls.toBoolean(i))

            LUA_TNUMBER -> {
                if (ls.isInteger(i)) {
                    print("[${ls.toInteger(i)}]")
                } else {
                    print("[${ls.toNumber(i)}]")
                }
            }
            LUA_TSTRING -> print("[${ls.toString(i)}]")

            else -> print("[${ls.typeName(t)}]")
        }
    }

    println()
}
