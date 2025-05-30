/*
 * Copyright 2019 Google LLC
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

package kwasm.format

/**
 * Reflects the current location within a text-based wasm module.
 *
 * Primarily used in conjunction with [ParseException].
 */
data class ParseContext(val fileName: String, val lineNumber: Int = 1, val column: Int = 1) {
    override fun toString(): String = "$fileName:$lineNumber:$column"
}

/**
 * Returns a new [ParseContext] with the [ParseContext.column] value shifted by the given [amount],
 * or `null` if the receiver is null.
 */
fun ParseContext?.shiftColumnBy(amount: Int) = this?.copy(column = column + amount)

/**
 * Returns a new [ParseContext] with the [ParseContext.lineNumber] value shifted by the given
 * [amount] or `null` if the receiver is null.
 */
fun ParseContext?.shiftLineNumberBy(amount: Int) = this?.copy(lineNumber = lineNumber + amount)
