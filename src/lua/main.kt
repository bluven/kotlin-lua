package lua

import lua.binchunk.undump
import lua.binchunk.Prototype

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {

    if (args.isNotEmpty()) {
        val data = Files.readAllBytes(Paths.get(args[0]))
        val proto = undump(data)
        list(proto)
    }
}

private fun list(proto: Prototype) {
    printHeader(proto)
    printCode(proto)
    printDetail(proto)

    proto.protos.forEach(::list)
}

private fun printHeader(f: Prototype) {
    val funcType = if (f.lineDefined > 0) "function" else "main"
    val varargFlag = if (f.isVararg > 0) "+" else ""

    System.out.printf(
        "\n%s <%s:%d,%d> (%d instructions)\n",
        funcType, f.source, f.lineDefined, f.lastLineDefined,
        f.code.size
    )

    System.out.printf(
        "%d%s params, %d slots, %d upvalues, ",
        f.numParams, varargFlag, f.maxStackSize, f.upvalueNames.size
    )

    System.out.printf(
        "%d locals, %d constants, %d functions\n",
        f.localVars.size, f.constants.size, f.protos.size
    )
}

private fun printCode(f: Prototype) {
    for (i in f.code.indices) {
        val line = if (f.lineInfo.isNotEmpty()) f.lineInfo[i].toString() else "-"
        System.out.printf("\t%d\t[%s]\t0x%08X\n", i + 1, line, f.code[i])
    }
}

private fun printDetail(f: Prototype) {
    System.out.printf("constants (%d):\n", f.constants.size)
    var i = 1
    for (k in f.constants) {
        System.out.printf("\t%d\t%s\n", i++, constantToString(k))
    }

    i = 0
    System.out.printf("locals (%d):\n", f.localVars.size)
    for (locVar in f.localVars) {
        System.out.printf(
            "\t%d\t%s\t%d\t%d\n", i++,
            locVar.varName, locVar.startPC + 1, locVar.endPC + 1
        )
    }

    i = 0
    System.out.printf("upvalues (%d):\n", f.upvalues.size)
    for (upval in f.upvalues) {
        val name = if (f.upvalueNames.isNotEmpty()) f.upvalueNames[i] else "-"
        System.out.printf(
            "\t%d\t%s\t%d\t%d\n", i++,
            name, upval.inStack, upval.idx
        )
    }
}

private fun constantToString(k: Any?): String {
    return if (k == null) {
        "nil"
    } else if (k is String) {
        "\"" + k + "\""
    } else {
        k.toString()
    }
}
