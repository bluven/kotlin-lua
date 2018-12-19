package lua.vm

import lua.api.LuaVM

/*
 31       22       13       5    0
  +-------+^------+-^-----+-^-----
  |b=9bits |c=9bits |a=8bits|op=6|
  +-------+^------+-^-----+-^-----
  |    bx=18bits    |a=8bits|op=6|
  +-------+^------+-^-----+-^-----
  |   sbx=18bits    |a=8bits|op=6|
  +-------+^------+-^-----+-^-----
  |    ax=26bits            |op=6|
  +-------+^------+-^-----+-^-----
 31      23      15       7      0
*/

const val MAXARG_Bx = (1 shl 18) - 1   // 262143
const val MAXARG_sBx = MAXARG_Bx shr 1 // 131071

class Instruction(content: Int) {

    val opCode = OpCode.values()[content and 0x3F]

    val a = content shr 6 and 0xFF

    val c = content shr 14 and 0x1FF

    val b = content shr 23 and 0x1FF

    val bx = content.ushr(14)

    val sBx = bx - MAXARG_sBx

    val ax = content.ushr(6)

    val isReturn = opCode === OpCode.RETURN

    fun execute(vm: LuaVM) {
        opCode.action(this, vm)
    }

}