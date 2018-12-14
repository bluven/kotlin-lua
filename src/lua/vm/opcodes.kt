package lua.vm

import lua.api.LuaVM
import lua.vm.OpMode.*
import lua.vm.OpArgMask.*

enum class OpMode {
    iABC , // [  B:9  ][  C:9  ][ A:8  ][OP:6]
    iABx , // [      Bx:18     ][ A:8  ][OP:6]
    iAsBx, // [     sBx:18     ][ A:8  ][OP:6]
    iAx    // [           Ax:26        ][OP:6]
}

enum class OpArgMask {
    OpArgN, // argument is not used
    OpArgU, // argument is used
    OpArgR, // argument is a register or a jump offset
    OpArgK // argument is a constant or register/constant
}

typealias OpAction = (Instruction, LuaVM) -> Unit

enum class OpCode(
    val testFlag: Int,
    val setAFlag: Int,
    val argBMode: OpArgMask,
    val argCMode: OpArgMask,
    val opMode: OpMode,
    val action: OpAction? = null) {

    /*       T  A    B       C     mode */
    MOVE    (0, 1, OpArgR, OpArgN, iABC, ::move), // R(A) := R(B)
    LOADK   (0, 1, OpArgK, OpArgN, iABx, ::loadK), // R(A) := Kst(Bx)
    LOADKX  (0, 1, OpArgN, OpArgN, iABx, ::loadKx), // R(A) := Kst(extra arg)
    LOADBOOL(0, 1, OpArgU, OpArgU, iABC, ::loadBool), // R(A) := (bool)B; if (C) pc++
    LOADNIL (0, 1, OpArgU, OpArgN, iABC, ::loadNil), // R(A), R(A+1), ..., R(A+B) := nil
    GETUPVAL(0, 1, OpArgU, OpArgN, iABC), // R(A) := UpValue[B]
    GETTABUP(0, 1, OpArgU, OpArgK, iABC), // R(A) := UpValue[B][RK(C)]
    GETTABLE(0, 1, OpArgR, OpArgK, iABC ), // R(A) := R(B)[RK(C)]
    SETTABUP(0, 0, OpArgK, OpArgK, iABC ), // UpValue[A][RK(B)] := RK(C)
    SETUPVAL(0, 0, OpArgU, OpArgN, iABC ), // UpValue[B] := R(A)
    SETTABLE(0, 0, OpArgK, OpArgK, iABC ), // R(A)[RK(B)] := RK(C)
    NEWTABLE(0, 1, OpArgU, OpArgU, iABC ), // R(A) := {} (size = B,C)
    SELF    (0, 1, OpArgR, OpArgK, iABC ), // R(A+1) := R(B); R(A) := R(B)[RK(C)]
    ADD     (0, 1, OpArgK, OpArgK, iABC, ::add), // R(A) := RK(B) + RK(C)
    SUB     (0, 1, OpArgK, OpArgK, iABC, ::sub), // R(A) := RK(B) - RK(C)
    MUL     (0, 1, OpArgK, OpArgK, iABC, ::mul), // R(A) := RK(B) * RK(C)
    MOD     (0, 1, OpArgK, OpArgK, iABC, ::mod), // R(A) := RK(B) % RK(C)
    POW     (0, 1, OpArgK, OpArgK, iABC, ::pow ), // R(A) := RK(B) ^ RK(C)
    DIV     (0, 1, OpArgK, OpArgK, iABC, ::div ), // R(A) := RK(B) / RK(C)
    IDIV    (0, 1, OpArgK, OpArgK, iABC, ::idiv ), // R(A) := RK(B) // RK(C)
    BAND    (0, 1, OpArgK, OpArgK, iABC, ::band ), // R(A) := RK(B) & RK(C)
    BOR     (0, 1, OpArgK, OpArgK, iABC, ::bor ), // R(A) := RK(B) | RK(C)
    BXOR    (0, 1, OpArgK, OpArgK, iABC, ::bxor ), // R(A) := RK(B) ~ RK(C)
    SHL     (0, 1, OpArgK, OpArgK, iABC, ::shl ), // R(A) := RK(B) << RK(C)
    SHR     (0, 1, OpArgK, OpArgK, iABC, ::shr ), // R(A) := RK(B) >> RK(C)
    UNM     (0, 1, OpArgR, OpArgN, iABC, ::unm ), // R(A) := -R(B)
    BNOT    (0, 1, OpArgR, OpArgN, iABC, ::bnot ), // R(A) := ~R(B)
    NOT     (0, 1, OpArgR, OpArgN, iABC, ::not ), // R(A) := not R(B)
    LEN     (0, 1, OpArgR, OpArgN, iABC, ::length ), // R(A) := length of R(B)
    CONCAT  (0, 1, OpArgR, OpArgR, iABC, ::concat), // R(A) := R(B).. ... ..R(C)
    JMP     (0, 0, OpArgR, OpArgN, iAsBx, ::jmp), // pc+=sBx; if (A) close all upvalues >= R(A - 1)
    EQ      (1, 0, OpArgK, OpArgK, iABC, ::eq), // if ((RK(B) == RK(C)) ~= A) then pc++
    LT      (1, 0, OpArgK, OpArgK, iABC, ::lt), // if ((RK(B) <  RK(C)) ~= A) then pc++
    LE      (1, 0, OpArgK, OpArgK, iABC, ::le), // if ((RK(B) <= RK(C)) ~= A) then pc++
    TEST    (1, 0, OpArgN, OpArgU, iABC, ::test ), // if not (R(A) <=> C) then pc++
    TESTSET (1, 1, OpArgR, OpArgU, iABC, ::testSet ), // if (R(B) <=> C) then R(A) := R(B) else pc++
    CALL    (0, 1, OpArgU, OpArgU, iABC), // R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
    TAILCALL(0, 1, OpArgU, OpArgU, iABC ), // return R(A)(R(A+1), ... ,R(A+B-1))
    RETURN  (0, 0, OpArgU, OpArgN, iABC ), // return R(A), ... ,R(A+B-2)
    FORLOOP (0, 1, OpArgR, OpArgN, iAsBx, ::forLoop), // R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }
    FORPREP (0, 1, OpArgR, OpArgN, iAsBx, ::forPrep), // R(A)-=R(A+2); pc+=sBx
    TFORCALL(0, 0, OpArgN, OpArgU, iABC ), // R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2));
    TFORLOOP(0, 1, OpArgR, OpArgN, iAsBx), // if R(A+1) ~= nil then { R(A)=R(A+1); pc += sBx }
    SETLIST (0, 0, OpArgU, OpArgU, iABC ), // R(A)[(C-1)*FPF+i] := R(A+i), 1 <= i <= B
    CLOSURE (0, 1, OpArgU, OpArgN, iABx ), // R(A) := closure(KPROTO[Bx])
    VARARG  (0, 1, OpArgU, OpArgN, iABC ), // R(A), R(A+1), ..., R(A+B-2) = vararg
    EXTRAARG(0, 0, OpArgU, OpArgU, iAx  ), // extra (larger) argument for previous opcode
    ;
}
