package lua

import java.nio.file.Files
import java.nio.file.Paths

import lua.api.LuaState
import lua.state.LuaStateImpl

fun main(args: Array<String>) {
    if (args.size > 0) {
        val data = Files.readAllBytes(Paths.get(args[0]))
        val ls = LuaStateImpl()

        ls.register("print", ::print)
        ls.load(data, args[0], "b")
        ls.call(0, 0)
    }
}

private fun print(ls: LuaState): Int {
    val nArgs = ls.top

    for (i in 1..nArgs) {
        if (ls.isBoolean(i)) {
            System.out.print(ls.toBoolean(i))
        } else if (ls.isString(i)) {
            System.out.print(ls.toString(i))
        } else {
            System.out.print(ls.typeName(ls.type(i)))
        }
        if (i < nArgs) {
            print("\t")
        }
    }
    println()
    return 0
}