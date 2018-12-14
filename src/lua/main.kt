package lua

import lua.debug.testBasicInstructions

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        testBasicInstructions(args[0])
    }
}
