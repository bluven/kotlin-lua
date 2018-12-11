package lua

import lua.debug.printBinChunk
import lua.debug.testOp

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        printBinChunk(args[0])
    }

    testOp()
}
