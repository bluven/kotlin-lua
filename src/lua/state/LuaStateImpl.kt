package lua.state

import lua.api.*
import lua.api.ArithOp.*
import lua.api.CmpOp.*
import lua.api.LuaType.*
import lua.binchunk.undump
import lua.vm.Instruction

class LuaStateImpl: LuaVM {

    internal var registry = LuaTable(0, 0)
    private var stack = LuaStack()

    init {
        registry.put(LUA_RIDX_GLOBALS, LuaTable(0, 0))

        val stack = LuaStack()
        stack.state = this

        pushLuaStack(stack)
    }

    private fun pushLuaStack(newTop: LuaStack) {
        newTop.prev = this.stack
        this.stack = newTop
    }

    private fun popLuaStack() {
        val top = this.stack
        this.stack = top.prev!!
        top.prev = null
    }

    /* basic stack manipulation */

    override var top: Int
        get() = stack.top
        set(idx) {
            val newTop = stack.absIndex(idx)
            if (newTop < 0) {
                throw Exception("stack underflow!")
            }

            val n = stack.top - newTop
            if (n > 0) {
                for (i in 0 until n) {
                    stack.pop()
                }
            } else if (n < 0) {
                for (i in 0 downTo n + 1) {
                    stack.push(null)
                }
            }
        }

    override fun absIndex(idx: Int): Int {
        return stack.absIndex(idx)
    }

    override fun checkStack(n: Int): Boolean {
        return true // TODO
    }

    override fun pop(n: Int) {
        for (i in 0 until n) {
            stack.pop()
        }
    }

    override fun copy(fromIdx: Int, toIdx: Int) {
        stack.set(toIdx, stack.get(fromIdx))
    }

    override fun pushValue(idx: Int) {
        stack.push(stack.get(idx))
    }

    override fun replace(idx: Int) {
        stack.set(idx, stack.pop())
    }

    override fun insert(idx: Int) {
        rotate(idx, 1)
    }

    override fun remove(idx: Int) {
        rotate(idx, -1)
        pop(1)
    }

    override fun rotate(idx: Int, n: Int) {
        val t = stack.top - 1            /* end of stack segment being rotated */
        val p = stack.absIndex(idx) - 1    /* start of segment */
        val m = if (n >= 0) t - n else p - n - 1 /* end of prefix */

        stack.reverse(p, m)     /* reverse the prefix with length 'n' */
        stack.reverse(m + 1, t) /* reverse the suffix */
        stack.reverse(p, t)     /* reverse the entire segment */
    }

    /* access functions (stack -> Go); */

    override fun typeName(tp: LuaType) = when (tp) {
        LUA_TNONE -> "no value"
        LUA_TNIL -> "nil"
        LUA_TBOOLEAN -> "boolean"
        LUA_TNUMBER -> "number"
        LUA_TSTRING -> "string"
        LUA_TTABLE -> "table"
        LUA_TFUNCTION -> "function"
        LUA_TTHREAD -> "thread"
        else -> "userdata"
    }

    override fun type(idx: Int) = if (stack.isValid(idx)) typeOf(stack.get(idx)) else LUA_TNONE

    override fun isNone(idx: Int) = type(idx) === LUA_TNONE

    override fun isNil(idx: Int) = type(idx) === LUA_TNIL

    override fun isNoneOrNil(idx: Int): Boolean {
        val t = type(idx)
        return t === LUA_TNONE || t === LUA_TNIL
    }

    override fun isBoolean(idx: Int) = type(idx) === LUA_TBOOLEAN

    override fun isInteger(idx: Int) = stack.get(idx) is Long

    override fun isNumber(idx: Int) = toNumberX(idx) != null

    override fun isString(idx: Int): Boolean {
        val t = type(idx)
        return t === LUA_TSTRING || t === LUA_TNUMBER
    }

    override fun isTable(idx: Int) = type(idx) === LUA_TTABLE

    override fun isThread(idx: Int) = type(idx) === LUA_TTHREAD

    override fun isFunction(idx: Int) = type(idx) === LUA_TFUNCTION

    override fun isKFunction(idx: Int): Boolean {
        val value = stack.get(idx)
        return value is Closure && value.kFunc != null
    }

    override fun toBoolean(idx: Int) = toBoolean(stack.get(idx))

    override fun toInteger(idx: Int) = toIntegerX(idx) ?: 0

    override fun toIntegerX(idx: Int): Long? {
        val value = stack.get(idx)
        return if (value is Long) value else null
    }

    override fun toNumber(idx: Int) = toNumberX(idx) ?: 0.toDouble()

    override fun toNumberX(idx: Int): Double? {
        val value = stack.get(idx)

        return when (value) {
            is Double -> value
            is Long -> value.toDouble()
            else -> null
        }
    }

    override fun toString(idx: Int): String? {
        val value = stack.get(idx)

        return when(value) {
            is String -> value
            is Long, is Double -> value.toString()
            else -> null
        }
    }

    override fun toKFunction(idx: Int): KFunction? {
        val value = stack.get(idx)

        return if (value is Closure) value.kFunc else null
    }

    /* push functions (Go -> stack); */

    override fun pushNil() {
        stack.push(null)
    }

    override fun pushBoolean(b: Boolean) {
        stack.push(b)
    }

    override fun pushInteger(n: Long) {
        stack.push(n)
    }

    override fun pushNumber(n: Double) {
        stack.push(n)
    }

    override fun pushString(s: String) {
        stack.push(s)
    }

    override fun pushKFunction(f: KFunction) {
        stack.push(Closure(kFunc = f))
    }

    override fun pushGlobalTable() {
        stack.push(registry[LUA_RIDX_GLOBALS])
    }

    override fun arith(op: ArithOp) {
        val b = stack.pop()
        val a = if (op != LUA_OPUNM && op != LUA_OPBNOT) {
            stack.pop()
        } else {
            b
        }

        val result = arith(a, b, op)
        if (result != null) {
            stack.push(result)
        } else {
            throw Exception("arithmetic error!")
        }
    }

    override fun compare(idx1: Int, idx2: Int, op: CmpOp): Boolean {
        if (!stack.isValid(idx1) || !stack.isValid(idx2)) {
            return false
        }

        val a = stack.get(idx1)
        val b = stack.get(idx2)

        return when (op) {
            LUA_OPEQ -> eq(a, b)
            LUA_OPLT -> lt(a!!, b!!)
            LUA_OPLE -> le(a!!, b!!)
        }
    }

    override fun newTable() {
        createTable(0, 0)
    }

    override fun createTable(nArr: Int, nRec: Int) {
        stack.push(LuaTable(nArr, nRec))
    }

    override fun getTable(idx: Int): LuaType {
        val t = stack.get(idx)
        val k = stack.pop()
        return getTable(t, k)
    }

    override fun getField(idx: Int, k: String): LuaType {
        val t = stack.get(idx)
        return getTable(t, k)
    }

    override fun getI(idx: Int, i: Long): LuaType {
        val t = stack.get(idx)
        return getTable(t, i)
    }

    private fun getTable(t: Any?, k: Any?): LuaType {
        if (t is LuaTable) {
            val v = t[k]
            stack.push(v)
            return typeOf(v)
        }
        throw Exception("not a table!") // todo
    }

    override fun getGlobal(name: String) = getTable(registry.get(LUA_RIDX_GLOBALS), name)

    override fun setGlobal(name: String) {
        setTable(registry[LUA_RIDX_GLOBALS], name, stack.pop())
    }

    override fun setTable(idx: Int) {
        val t = stack.get(idx)
        val v = stack.pop()
        val k = stack.pop()
        setTable(t, k, v)
    }

    override fun setField(idx: Int, k: String) {
        val t = stack.get(idx)
        val v = stack.pop()
        setTable(t, k, v)
    }

    override fun setI(idx: Int, i: Long) {
        val t = stack.get(idx)
        val v = stack.pop()
        setTable(t, i, v)
    }

    private fun setTable(t: Any?, k: Any?, v: Any?) {
        if (t is LuaTable) {
            t.put(k, v)
            return
        }
        throw Exception("not a table!")
    }

    override fun register(name: String, f: KFunction) {
        pushKFunction(f)
        setGlobal(name)
    }
    /* 'load' and 'call' functions */

    override fun load(chunk: ByteArray, chunkName: String, mode: String): ThreadStatus {
        stack.push(Closure(undump(chunk)))
        return ThreadStatus.LUA_OK
    }

    override fun call(nArgs: Int, nResults: Int) {
        val value = stack.get(-(nArgs + 1))
        if (value is Closure) {
            if(value.proto != null) {
                callLuaClosure(nArgs, nResults, value)
            } else {
                callKotlinClosure(nArgs, nResults, value)
            }
        } else {
            throw Exception("not function!")
        }
    }


    private fun callKotlinClosure(nArgs: Int, nResults: Int, c: Closure) {
        val newStack = LuaStack()
        newStack.state = this
        newStack.closure = c

        // pass args, pop func
        if (nArgs > 0) {
            newStack.pushN(stack.popN(nArgs), nArgs)
        }
        stack.pop()

        // run closure
        pushLuaStack(newStack)
        val r = c.kFunc!!(this)
        popLuaStack()

        // return results
        if (nResults != 0) {
            val results = newStack.popN(r)
            //stack.check(results.size())
            stack.pushN(results, nResults)
        }
    }

    private fun callLuaClosure(nArgs: Int, nResults: Int, c: Closure) {
        val proto = c.proto!!

        val nRegs = proto.maxStackSize
        val nParams = proto.numParams
        val isVararg = proto.isVararg.toInt() == 1

        // create new lua stack
        val newStack = LuaStack(/*nRegs + 20*/)
        newStack.closure = c

        // pass args, pop func
        val funcAndArgs = stack.popN(nArgs + 1)
        newStack.pushN(funcAndArgs.subList(1, funcAndArgs.size), nParams.toInt())
        if (nArgs > nParams && isVararg) {
            newStack.varargs = funcAndArgs.subList(nParams + 1, funcAndArgs.size)
        }

        // run closure
        pushLuaStack(newStack)
        top = nRegs.toInt()
        runLuaClosure()
        popLuaStack()

        // return results
        if (nResults != 0) {
            val results = newStack.popN(newStack.top - nRegs)
            //stack.check(results.size())
            stack.pushN(results, nResults)
        }
    }

    private fun runLuaClosure() {
        while (true) {
            val i = Instruction(fetch())

            i.execute(this)

            if (i.isReturn) {
                break
            }
        }
    }

    override fun len(idx: Int) {
        val value = stack.get(idx)

        when (value) {
            is String -> pushInteger(value.length.toLong())
            is LuaTable -> pushInteger(value.length().toLong())
            else -> throw Exception("length error!")
        }
    }

    override fun concat(n: Int) {
        if (n == 0) {
            stack.push("")
        } else if (n >= 2) {

            repeat(n - 1) {
                if (!isString(-1) || !isString(-2)) {
                    throw Exception("concatenation error!")
                }

                val s2 = toString(-1)
                val s1 = toString(-2)
                pop(2)
                pushString(s1 + s2)
            }
        }

        // n == 1, do nothing
    }

    override fun addPC(n: Int) {
        stack.pc += n
    }

    override fun fetch(): Int {
        val i = stack.closure!!.proto!!.code[stack.pc]
        stack.pc += 1

        return i
    }

    override fun getConst(idx: Int) {
        stack.push(stack.closure!!.proto!!.constants[idx])
    }

    override fun getRK(rk: Int) {
        if (rk > 0xFF) { // constant
            getConst(rk and 0xFF)
        } else { // register
            pushValue(rk + 1)
        }
    }

    override fun registerCount(): Int {
        return stack.closure!!.proto!!.maxStackSize.toInt()
    }

    override fun loadVararg(n: Int) {
        val varargs = if (stack.varargs != null) stack.varargs!! else emptyList()
        val n = if (n < 0) varargs.size else n

        stack.pushN(varargs, n)
    }

    override fun loadProto(idx: Int) {
        val proto = stack.closure!!.proto!!.protos[idx]
        stack.push(Closure(proto))
    }
}
