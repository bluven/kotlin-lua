package lua

import lua.debug.testTable

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        testTable(args[0])
    }
}
