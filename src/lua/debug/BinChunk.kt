package lua.debug

import lua.binchunk.undump
import lua.binchunk.Prototype
import lua.vm.Instruction
import lua.vm.OpArgMask.*
import lua.vm.OpMode.*

import java.nio.file.Files
import java.nio.file.Paths

fun printBinChunk(path: String) {
    val data = Files.readAllBytes(Paths.get(path))
    val proto = undump(data)
    list(proto)
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

    f.code.forEachIndexed { i, code ->

        val line = if (f.lineInfo.isNotEmpty()) f.lineInfo[i].toString() else "-"
        val inst = Instruction(code)

        System.out.printf("\t%d\t[%s]\t%-8s \t", i + 1, line, inst.opCode)
//        print("\t${i+1}\t[$line]\t${inst.opCode}")
        printOperands(inst)
        println()
    }
}

private fun printOperands(i: Instruction) {
    val opCode = i.opCode
    val a = i.a
    when (opCode.opMode) {
        iABC -> {
            System.out.printf("%d", a)
            if (opCode.argBMode !== OpArgN) {
                val b = i.b
                System.out.printf(" %d", if (b > 0xFF) -1 - (b and 0xFF) else b)
            }
            if (opCode.argCMode !== OpArgN) {
                val c = i.c
                System.out.printf(" %d", if (c > 0xFF) -1 - (c and 0xFF) else c)
            }
        }
        iABx -> {
            System.out.printf("%d", a)
            val bx = i.bx
            if (opCode.argBMode === OpArgK) {
                System.out.printf(" %d", -1 - bx)
            } else if (opCode.argBMode === OpArgU) {
                System.out.printf(" %d", bx)
            }
        }
        iAsBx -> {
            System.out.printf("%d %d", a, i.sBx)
        }
        iAx -> {
            System.out.printf("%d", -1 - i.ax)
        }
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
