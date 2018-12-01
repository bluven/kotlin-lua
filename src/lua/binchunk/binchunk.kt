package lua.binchunk

data class Upvalue(val inStack: Byte, val idx: Byte)

data class LocVar(val varName: String, val startPC: Int, val endPC: Int)

data class Prototype (
    val source: String,
    val lineDefined: Int,
    val lastLineDefined: Int,
    val numParams: Byte,
    val isVararg: Byte,
    val maxStackSize: Byte,
    val code: List<Int>,
    val constants: List<Any?>,
    val upvalues: List<Upvalue>,
    val protos: List<Prototype>,
    val lineInfo: List<Int>,
    val localVars: List<LocVar>,
    val upvalueNames: List<String>
)

