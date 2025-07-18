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

package kwasm

import kwasm.ast.type.FunctionType
import kwasm.ast.type.ValueType
import kwasm.runtime.Value

/** Represents a function exported by a WebAssembly module. */
interface ExportedFunction {
    /** String-representation of the function's arguments and result type. */
    val signature: String

    /** Number of expected arguments. */
    val argCount: Int

    /**
     * Invokes the exported WebAssembly function.
     *
     * When called, the provided [args] will be compared against the required parameters of the
     * exported WebAssembly function about to be executed. If the arguments don't match the
     * requirements, an [IllegalArgumentException] will be thrown.
     *
     * Returns `null` if the exported function has no result value.
     */
    operator fun invoke(vararg args: Number): Number?

    data class IllegalArgumentException(
        override val message: String
    ) : kotlin.IllegalArgumentException(message) {
        companion object {
            fun create(
                moduleName: String,
                functionName: String,
                functionType: FunctionType,
                args: List<Value<*>>
            ): IllegalArgumentException {
                val signature = functionType.toSignature(moduleName, functionName)
                val argumentsList = args.map { it::class.simpleName }

                return IllegalArgumentException(
                    "Incorrect arguments for exported function $signature: $argumentsList"
                )
            }
        }
    }
}

internal fun FunctionType.toSignature(moduleName: String, functionName: String): String {
    val functionParams = parameters.joinToString(", ") {
        when (it.valType) {
            ValueType.I32 -> "Int"
            ValueType.I64 -> "Long"
            ValueType.F32 -> "Float"
            ValueType.F64 -> "Double"
        }
    }
    val retStr = when (returnValueEnums.firstOrNull()?.valType) {
        ValueType.I32 -> "Int"
        ValueType.I64 -> "Long"
        ValueType.F32 -> "Float"
        ValueType.F64 -> "Double"
        null -> "Unit"
    }
    return "$moduleName.$functionName($functionParams) -> $retStr"
}
