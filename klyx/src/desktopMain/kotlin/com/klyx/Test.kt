package com.klyx

import kotlin.math.pow

fun main() {
    val principal = 100.0
    val rate = 0.10
    val time = 250.0

    val finalAmount = calculateCompoundGrowth(principal, rate, time)
    println("Final amount after ${time.toInt()} days: ${"%,.2f".format(finalAmount)}")
}

fun calculateCompoundGrowth(principal: Double, rate: Double, time: Double): Double {
    val futureValue = principal * (1 + rate).pow(time)
    return futureValue
}
