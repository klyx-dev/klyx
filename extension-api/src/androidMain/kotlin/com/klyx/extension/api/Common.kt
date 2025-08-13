package com.klyx.extension.api

import com.klyx.wasm.type.WasmString
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.collections.WasmList

typealias EnvVars = WasmList<Tuple2<WasmString, WasmString>>

/**
 * @property start The start of the range (inclusive).
 * @property end The end of the range (exclusive).
 */
data class Range(val start: Int, val end: Int)
