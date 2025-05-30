/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm.runtime

import kwasm.ast.type.GlobalType
import kwasm.ast.type.ValueType

/**
 * From [the docs]():
 *
 * A global instance is the runtime representation of a global variable. It holds an individual
 * value and a flag indicating whether it is mutable.
 *
 * ```
 *   globalinst ::= {value val, mut mut}
 * ```
 *
 * The value of mutable globals can be mutated through variable instructions or by external means
 * provided by the embedder.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
sealed class Global<T : Number> {
    abstract var value: T
    abstract val mutable: Boolean
    abstract fun update(value: Number)

    data class Int(
        override var value: kotlin.Int,
        override val mutable: Boolean
    ) : Global<kotlin.Int>() {
        var unsigned: UInt
            get() = value.toUInt()
            set(value) {
                this.value = value.toInt()
            }

        override fun update(value: Number) {
            this.value = value.toInt()
        }
    }

    data class Long(
        override var value: kotlin.Long,
        override val mutable: Boolean
    ) : Global<kotlin.Long>() {
        var unsigned: ULong
            get() = value.toULong()
            set(value) {
                this.value = value.toLong()
            }

        override fun update(value: Number) {
            this.value = value.toLong()
        }
    }

    data class Float(
        override var value: kotlin.Float,
        override val mutable: Boolean
    ) : Global<kotlin.Float>() {
        override fun update(value: Number) {
            this.value = value.toFloat()
        }
    }

    data class Double(
        override var value: kotlin.Double,
        override val mutable: Boolean
    ) : Global<kotlin.Double>() {
        override fun update(value: Number) {
            this.value = value.toDouble()
        }
    }
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/modules.html#alloc-global):
 *
 * 1. Let `globaltype` be the global type to allocate and `val` the value to initialize
 *    the global with.
 * 1. Let `mut t` be the structure of global type `globaltype`.
 * 1. Let `a` be the first free global address in `S`.
 * 1. Let `globalinst` be the global instance `{value val, mut mut}`.
 * 1. Append `globalinst` to the `globals` of `S`.
 * 1. Return `a`.
 */
fun <T : Number> Store.allocate(
    globalType: GlobalType,
    value: T
): Store.Allocation<Address.Global> {
    val globalInst = when (globalType.valueType) {
        ValueType.I32 -> {
            val intValue = requireNotNull(value as? Int) { "Expected I32 value" }
            Global.Int(intValue, globalType.mutable)
        }
        ValueType.I64 -> {
            val longValue = requireNotNull(value as? Long) { "Expected I64 value" }
            Global.Long(longValue, globalType.mutable)
        }
        ValueType.F32 -> {
            val floatValue = requireNotNull(value as? Float) { "Expected F32 value" }
            Global.Float(floatValue, globalType.mutable)
        }
        ValueType.F64 -> {
            val doubleValue =
                requireNotNull(value as? Double) { "Expected F64 value" }
            Global.Double(doubleValue, globalType.mutable)
        }
    }

    return allocateGlobal(globalInst)
}
