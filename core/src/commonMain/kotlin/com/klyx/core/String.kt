package com.klyx.core

import kotlin.math.pow
import kotlin.math.round

fun String.format(vararg args: Any?): String {
    val regex = Regex("%(\\.\\d+)?[sdif]")
    var argIndex = 0

    return regex.replace(this) { match ->
        if (argIndex >= args.size) return@replace match.value // not enough arguments

        val spec = match.value
        val value = args[argIndex++]

        when {
            spec.endsWith("s") -> value.toString()
            spec.endsWith("d") || spec.endsWith("i") -> (value as? Number)?.toInt()?.toString() ?: "NaN"

            spec.endsWith("f") -> {
                val precision = spec.substringAfter('.', "1").dropLast(1).toIntOrNull() ?: 1
                val num = (value as? Number)?.toDouble() ?: return@replace "NaN"
                formatFloat(num, precision)
            }

            else -> match.value
        }
    }
}

private fun formatFloat(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    return rounded.toString()
}

fun Double.toFixed(decimals: Int): Double {
    return "%.${decimals}f".format(this).toDouble()
}

fun Float.toFixed(decimals: Int): Float {
    return "%.${decimals}f".format(this).toFloat()
}
