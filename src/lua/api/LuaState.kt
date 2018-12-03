package lua.api

interface LuaState {
    /* basic stack manipulation */
    var top: Int

    fun absIndex(idx: Int): Int
    fun checkStack(n: Int): Boolean
    fun pop(n: Int)
    fun copy(fromIdx: Int, toIdx: Int)
    fun pushValue(idx: Int)
    fun replace(idx: Int)
    fun insert(idx: Int)
    fun remove(idx: Int)
    fun rotate(idx: Int, n: Int)

    /* access functions (stack -> Go); */
    fun typeName(tp: LuaType): String
    fun type(idx: Int): LuaType
    fun isNone(idx: Int): Boolean
    fun isNil(idx: Int): Boolean
    fun isNoneOrNil(idx: Int): Boolean
    fun isBoolean(idx: Int): Boolean
    fun isInteger(idx: Int): Boolean
    fun isNumber(idx: Int): Boolean
    fun isString(idx: Int): Boolean
    fun isTable(idx: Int): Boolean
    fun isThread(idx: Int): Boolean
    fun isFunction(idx: Int): Boolean
    fun toBoolean(idx: Int): Boolean
    fun toInteger(idx: Int): Long
    fun toIntegerX(idx: Int): Long?
    fun toNumber(idx: Int): Double
    fun toNumberX(idx: Int): Double?
    fun toString(idx: Int): String?

    /* push functions (Go -> stack); */
    fun pushNil()
    fun pushBoolean(b: Boolean)
    fun pushInteger(n: Long)
    fun pushNumber(n: Double)
    fun pushString(s: String)
}

