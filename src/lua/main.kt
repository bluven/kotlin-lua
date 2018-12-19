package lua

import java.nio.file.Files
import java.nio.file.Paths

import lua.state.LuaStateImpl

fun main(args: Array<String>) {
    if (args.size > 0) {
        val data = Files.readAllBytes(Paths.get(args[0]))
        val ls = LuaStateImpl()
        ls.load(data, args[0], "b")
        ls.call(0, 0)
    }
}
