package com.klyx.util

inline fun <T> List<T>.partitionPoint(predicate: (T) -> Boolean): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (predicate(this[mid])) {
            low = mid + 1
        } else {
            high = mid
        }
    }
    return low
}
