package lua.vm

import lua.api.LuaVM
import lua.api.ArithOp
import lua.api.ArithOp.*
import lua.api.CmpOp
import lua.api.CmpOp.*
import lua.api.LuaType.*

/* number of list items to accumulate before a SETLIST instruction */
const val LFIELDS_PER_FLUSH = 50

/* misc */

// R(A) := R(B)
fun move(i: Instruction, vm: LuaVM) {
    vm.copy(i.b + 1, i.a + 1)
}

// pc+=sBx; if (A) close all upvalues >= R(A - 1)
fun jmp(i: Instruction, vm: LuaVM) {
    vm.addPC(i.sBx)
    if (i.a != 0) {
        throw Exception("todo: jmp!")
    }
}

/* load */

// R(A), R(A+1), ..., R(A+B) := nil
fun loadNil(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b

    vm.pushNil()

    repeat(b) {
        vm.copy(-1, a + it)
    }

    vm.pop(1)
}

// R(A) := (bool)B; if (C) pc++
fun loadBool(i: Instruction, vm: LuaVM) {
    vm.pushBoolean(i.b != 0)

    vm.replace(i.a + 1)
    if (i.c != 0) {
        vm.addPC(1)
    }
}


// R(A) := Kst(Bx)
fun loadK(i: Instruction, vm: LuaVM) {
    vm.getConst(i.bx)
    vm.replace(i.a + 1)
}

// R(A) := Kst(extra arg)
fun loadKx(i: Instruction, vm: LuaVM) {
    val ax = Instruction(vm.fetch()).ax

    vm.getConst(ax)
    vm.replace(i.a + 1)
}

/* arith */

fun add(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPADD)
} // +

fun sub(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPSUB)
} // -

fun mul(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPMUL)
} // *

fun mod(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPMOD)
} // %

fun pow(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPPOW)
} // ^

fun div(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPDIV)
} // /

fun idiv(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPIDIV)
} // //

fun band(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPBAND)
} // &

fun bor(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPBOR)
} // |

fun bxor(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPBXOR)
} // ~

fun shl(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPSHL)
} // <<

fun shr(i: Instruction, vm: LuaVM) {
    binaryArith(i, vm, LUA_OPSHR)
} // >>

fun unm(i: Instruction, vm: LuaVM) {
    unaryArith(i, vm, LUA_OPUNM)
} // -

fun bnot(i: Instruction, vm: LuaVM) {
    unaryArith(i, vm, LUA_OPBNOT)
} // ~


// R(A) := RK(B) op RK(C)
private fun binaryArith(i: Instruction, vm: LuaVM, op: ArithOp) {
    vm.getRK(i.b)
    vm.getRK(i.c)
    vm.arith(op)

    vm.replace(i.a + 1)
}

// R(A) := op R(B)
private fun unaryArith(i: Instruction, vm: LuaVM, op: ArithOp) {
    vm.pushValue(i.b + 1)
    vm.arith(op)
    vm.replace(i.a + 1)
}

/* compare */
fun eq(i: Instruction, vm: LuaVM) {
    compare(i, vm, LUA_OPEQ)
} // ==

fun lt(i: Instruction, vm: LuaVM) {
    compare(i, vm, LUA_OPLT)
} // <

fun le(i: Instruction, vm: LuaVM) {
    compare(i, vm, LUA_OPLE)
} // <=

// if ((RK(B) op RK(C)) ~= A) then pc++
private fun compare(i: Instruction, vm: LuaVM, op: CmpOp) {
    vm.getRK(i.b)
    vm.getRK(i.c)
    if (vm.compare(-2, -1, op) != (i.a != 0)) {
        vm.addPC(1)
    }
    vm.pop(2)
}

/* logical */

// R(A) := not R(B)
fun not(i: Instruction, vm: LuaVM) {
    vm.pushBoolean(!vm.toBoolean(i.b + 1))
    vm.replace(i.a + 1)
}

// if not (R(A) <=> C) then pc++
fun test(i: Instruction, vm: LuaVM) {
    if (vm.toBoolean(i.a + 1) != (i.c != 0)) {
        vm.addPC(1)
    }
}

// if (R(B) <=> C) then R(A) := R(B) else pc++
fun testSet(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b + 1
    if (vm.toBoolean(b) == (i.c != 0)) {
        vm.copy(b, a)
    } else {
        vm.addPC(1)
    }
}

/* len & concat */

// R(A) := length of R(B)
fun length(i: Instruction, vm: LuaVM) {
    vm.len(i.b + 1)
    vm.replace(i.a + 1)
}

// R(A) := R(B).. ... ..R(C)
fun concat(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b + 1
    val c = i.c + 1
    val n = c - b + 1

    vm.checkStack(n)
    for (j in b..c) {
        vm.pushValue(j)
    }
    vm.concat(n)
    vm.replace(a)
}

/* for */

// R(A)-=R(A+2); pc+=sBx
fun forPrep(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val sBx = i.sBx

    if (vm.type(a) === LUA_TSTRING) {
        vm.pushNumber(vm.toNumber(a))
        vm.replace(a)
    }
    if (vm.type(a + 1) === LUA_TSTRING) {
        vm.pushNumber(vm.toNumber(a + 1))
        vm.replace(a + 1)
    }
    if (vm.type(a + 2) === LUA_TSTRING) {
        vm.pushNumber(vm.toNumber(a + 2))
        vm.replace(a + 2)
    }

    vm.pushValue(a)
    vm.pushValue(a + 2)
    vm.arith(LUA_OPSUB)
    vm.replace(a)
    vm.addPC(sBx)
}

// R(A)+=R(A+2);
// if R(A) <?= R(A+1) then {
//   pc+=sBx; R(A+3)=R(A)
// }
fun forLoop(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val sBx = i.sBx

    // R(A)+=R(A+2);
    vm.pushValue(a + 2)
    vm.pushValue(a)
    vm.arith(LUA_OPADD)
    vm.replace(a)

    val isPositiveStep = vm.toNumber(a + 2) >= 0
    if (isPositiveStep && vm.compare(a, a + 1, LUA_OPLE)
        ||
        !isPositiveStep && vm.compare(a + 1, a, LUA_OPLE)) {
        // pc+=sBx; R(A+3)=R(A)
        vm.addPC(sBx)
        vm.copy(a, a + 3)
    }
}

/* table */

private fun int2fb(x: Int): Int {
    var x = x
    var e = 0 /* exponent */
    if (x < 8) {
        return x
    }
    while (x >= 8 shl 4) { /* coarse steps */
        x = x + 0xf shr 4 /* x = ceil(x / 16) */
        e += 4
    }
    while (x >= 8 shl 1) { /* fine steps */
        x = x + 1 shr 1 /* x = ceil(x / 2) */
        e++
    }
    return e + 1 shl 3 or x - 8
}

/* converts back */
private fun fb2int(x: Int): Int {
    return if (x < 8) {
        x
    } else {
        (x and 7) + 8 shl (x shr 3) - 1
    }
}

// R(A) := {} (size = B,C)
fun newTable(i: Instruction, vm: LuaVM) {
    vm.createTable(fb2int(i.b), fb2int(i.c))
    vm.replace(i.a + 1)
}

// R(A) := R(B)[RK(C)]
fun getTable(i: Instruction, vm: LuaVM) {
    vm.getRK(i.c)
    vm.getTable(i.b + 1)
    vm.replace(i.a + 1)
}

// R(A)[RK(B)] := RK(C)
fun setTable(i: Instruction, vm: LuaVM) {
    vm.getRK(i.b)
    vm.getRK(i.c)
    vm.setTable(i.a + 1)
}

// R(A)[(C-1)*FPF+i] := R(A+i), 1 <= i <= B
fun setList(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    var b = i.b
    var c = i.c
    c = if (c > 0) c - 1 else Instruction(vm.fetch()).ax

    val bIsZero = b == 0
    if (bIsZero) {
        b = vm.toInteger(-1).toInt()  - a - 1
        vm.pop(1)
    }

    vm.checkStack(1)
    var idx = c * LFIELDS_PER_FLUSH
    for (j in 1..b) {
        idx++
        vm.pushValue(a + j)
        vm.setI(a, idx.toLong())
    }

    if (bIsZero) {
        for (j in vm.registerCount() + 1..vm.top) {
            idx++
            vm.pushValue(j)
            vm.setI(a, idx.toLong())
        }

        // clear stack
        vm.top = vm.registerCount()
    }
}

// R(A+1) := R(B); R(A) := R(B)[RK(C)]
fun _self(i: Instruction, vm: LuaVM) {
    val a = i.a
    val b = i.b + 1
    val c = i.c

    vm.copy(b, a + 1)
    vm.getRK(c)
    vm.getTable(b)
    vm.replace(a)
}

// R(A) := closure(KPROTO[Bx])
fun closure(i: Instruction, vm: LuaVM) {
    vm.loadProto(i.bx)
    vm.replace(i.a + 1)
}

// R(A), R(A+1), ..., R(A+B-2) = vararg
fun vararg(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b

    if (b != 1) { // b==0 or b>1
        vm.loadVararg(b - 1)
        popResults(a, b, vm)
    }
}

// return R(A)(R(A+1), ... ,R(A+B-1))
fun tailCall(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b
    // todo: optimize tail call!
    val c = 0
    val nArgs = pushFuncAndArgs(a, b, vm)
    vm.call(nArgs, c - 1)
    popResults(a, c, vm)
}

// R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
fun call(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b
    val c = i.c
    val nArgs = pushFuncAndArgs(a, b, vm)
    vm.call(nArgs, c - 1)
    popResults(a, c, vm)
}

// return R(A), ... ,R(A+B-2)
fun _return(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val b = i.b

    when {
        b == 1 -> {
            // no return values
        }
        b > 1 -> {
            // b-1 return values
            vm.checkStack(b - 1)
            for (j in a..a + b - 2) {
                vm.pushValue(j)
            }
        }
        else -> fixStack(a, vm)
    }
}

private fun pushFuncAndArgs(a: Int, b: Int, vm: LuaVM): Int {
    if (b >= 1) {
        vm.checkStack(b)
        for (i in a until a + b) {
            vm.pushValue(i)
        }
        return b - 1
    } else {
        fixStack(a, vm)
        return vm.top - vm.registerCount() - 1
    }
}

private fun fixStack(a: Int, vm: LuaVM) {
    val x = vm.toInteger(-1).toInt()
    vm.pop(1)

    vm.checkStack(x - a)
    for (i in a until x) {
        vm.pushValue(i)
    }
    vm.rotate(vm.registerCount() + 1, x - a)
}

private fun popResults(a: Int, c: Int, vm: LuaVM) {
    if (c == 1) {
        // no results
    } else if (c > 1) {
        for (i in a + c - 2 downTo a) {
            vm.replace(i)
        }
    } else {
        // leave results on stack
        vm.checkStack(1)
        vm.pushInteger(a.toLong())
    }
}


/* upvalues */

// R(A) := UpValue[B][RK(C)]
fun getTabUp(i: Instruction, vm: LuaVM) {
    val a = i.a + 1
    val c = i.c

    vm.pushGlobalTable()
    vm.getRK(c)
    vm.getTable(-2)
    vm.replace(a)
    vm.pop(1)
}