package lua.number

fun isInteger(f: Double): Boolean {
    return f == f.toLong().toDouble()
}

fun parseInteger(str: String): Long? {
    return try {
        java.lang.Long.parseLong(str)
    } catch (e: NumberFormatException) {
        null
    }

}

fun parseFloat(str: String): Double? {
    return try {
        java.lang.Double.parseDouble(str)
    } catch (e: NumberFormatException) {
        null
    }
}
