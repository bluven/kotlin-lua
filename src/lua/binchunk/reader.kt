package lua.binchunk

import java.nio.ByteBuffer
import java.nio.ByteOrder

private val LUA_SIGNATURE = byteArrayOf(0x1b, 'L'.toByte(), 'u'.toByte(), 'a'.toByte())
private const val LUAC_VERSION = 0x53
private const val LUAC_FORMAT = 0
private val LUAC_DATA = byteArrayOf(0x19, 0x93.toByte(), '\r'.toByte(), '\n'.toByte(), 0x1a, '\n'.toByte())
private const val CINT_SIZE = 4
private const val CSIZET_SIZE = 8
private const val INSTRUCTION_SIZE = 4
private const val LUA_INTEGER_SIZE = 8
private const val LUA_NUMBER_SIZE = 8
private const val LUAC_INT = 0x5678
private const val LUAC_NUM = 370.5

private const val TAG_NIL = 0x00
private const val TAG_BOOLEAN = 0x01
private const val TAG_NUMBER = 0x03
private const val TAG_INTEGER = 0x13
private const val TAG_SHORT_STR = 0x04
private const val TAG_LONG_STR = 0x14

private class Reader(val buf: ByteBuffer) {

     fun checkHead() {
        when {
            !LUA_SIGNATURE.contentEquals(readBytes(4)) -> throw RuntimeException("not a precompiled chunk!")
            buf.get().toInt() != LUAC_VERSION -> throw RuntimeException("version mismatch!")
            buf.get().toInt() != LUAC_FORMAT -> throw RuntimeException("format mismatch!")
            !LUAC_DATA.contentEquals(readBytes(6)) -> throw RuntimeException("corrupted!")
            buf.get().toInt() != CINT_SIZE -> throw RuntimeException("int size mismatch!")
            buf.get().toInt() != CSIZET_SIZE -> throw RuntimeException("size_t size mismatch!")
            buf.get().toInt() != INSTRUCTION_SIZE -> throw RuntimeException("instruction size mismatch!")
            buf.get().toInt() != LUA_INTEGER_SIZE -> throw RuntimeException("lua_Integer size mismatch!")
            buf.get().toInt() != LUA_NUMBER_SIZE -> throw RuntimeException("lua_Number size mismatch!")
            buf.long != LUAC_INT.toLong() -> throw RuntimeException("endianness mismatch!")
            buf.double != LUAC_NUM -> throw RuntimeException("float format mismatch!")
        }
    }

    fun readProto(parentSource: String = ""): Prototype {
        var source = readString()
        if (source.isEmpty()) {
            source = parentSource
        }

        return Prototype(
            source=source,
            lineDefined = buf.getInt(),
            lastLineDefined = buf.getInt(),
            numParams = buf.get(),
            isVararg = buf.get(),
            maxStackSize = buf.get(),
            code = readCode(),
            constants = readConstants(),
            upvalues = readUpvalues(),
            protos = readProtos(),
            lineInfo = readLineInfo(),
            localVars = readLocVars(),
            upvalueNames = readUpvalueNames()
        )
    }

    fun readCode(): List<Int> {
        val length = buf.getInt()
        val code = ArrayList<Int>(length)

        repeat(length) {
            code.add(buf.getInt())
        }

        return code
    }

    fun readConstants(): List<Any?> {
        val length = buf.getInt()
        val constants = ArrayList<Any?>(length)

        repeat(length) {
            constants.add(readConstant())
        }

        return constants
    }

    fun readConstant(): Any? {
        return when (buf.get().toInt()) {
            TAG_NIL -> null
            TAG_BOOLEAN -> buf.get().toInt() != 0
            TAG_INTEGER -> buf.long
            TAG_NUMBER -> buf.double
            TAG_SHORT_STR, TAG_LONG_STR -> readString()
            else -> throw RuntimeException("corrupted!")
        }
    }

    fun readString(): String {
        var size = buf.get().toInt()

        if(size == 0) {
           return  ""
        }

        if (size == 0xFF) {
           size = buf.getLong().toInt()
        }

        return String(readBytes(size - 1))
    }

    fun readUpvalues(): List<Upvalue> {
        val length = buf.getInt()
        val upvalues = ArrayList<Upvalue>(length)

        repeat(length) {
           upvalues.add(Upvalue(
               inStack=buf.get(),
               idx=buf.get())
           )
        }

        return upvalues
    }

    fun readProtos(parentSource: String = ""): List<Prototype> {
        val length = buf.getInt()
        val protos = ArrayList<Prototype>(length)

        repeat(length) {
            protos.add(readProto(parentSource))
        }

        return protos
    }

    fun readLineInfo(): List<Int> {
        val size = buf.getInt()
        val lineInfo = ArrayList<Int>()

        repeat(size) {
            lineInfo.add(buf.getInt())
        }

        return lineInfo
    }

    fun readLocVars(): List<LocVar> {
        val size = buf.getInt()
        val locVars = ArrayList<LocVar>(size)

        repeat(size) {
            locVars.add(
                LocVar(
                varName = readString(),
                startPC = buf.getInt(),
                endPC = buf.getInt()
            ))
        }

        return locVars
    }

    fun readUpvalueNames(): List<String> {
        val size = buf.getInt()
        val upvalueNames = ArrayList<String>(size)

        repeat(size) {
           upvalueNames.add(readString())
        }

        return upvalueNames
    }

    fun readByte() = buf.get()

    fun readBytes(n: Int): ByteArray {
        val a = ByteArray(n)
        buf.get(a)
        return a
    }
}

fun undump(data: ByteArray): Prototype {
    val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    val reader = Reader(buf)

    reader.checkHead()
    reader.readByte() // size_upvalues
    return reader.readProto()
}


