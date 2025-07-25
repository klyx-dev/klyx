package com.klyx.wasm

fun LongArray.takePair(offset: Int = 0): Pair<Long, Long> {
    return get(offset) to get(offset + 1)
}

fun LongArray.takeTriple(offset: Int = 0): Triple<Long, Long, Long> {
    return Triple(get(offset), get(offset + 1), get(offset + 2))
}
