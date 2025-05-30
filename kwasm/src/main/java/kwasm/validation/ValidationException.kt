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

package kwasm.validation

import kwasm.format.ParseContext

/**
 * Exception thrown during the validation process when an invalid WasmProgram is being analyzed.
 */
data class ValidationException(
    val errorMsg: String,
    val parseContext: ParseContext? = null,
    val rootCause: Throwable? = null
) : Exception("(${parseContext ?: "Unknown Context"}) $errorMsg", rootCause)

/**
 * Validates a given [condition], and throws a [ValidationException] if the [condition] is `false`.
 */
fun validate(condition: Boolean, parseContext: ParseContext? = null, message: String) {
    validate(condition, parseContext) { message }
}

/**
 * Validates a given [condition], and throws a [ValidationException] if the [condition] is `false`.
 */
inline fun validate(
    condition: Boolean,
    parseContext: ParseContext? = null,
    crossinline block: () -> String
) {
    if (!condition) throw ValidationException(block(), parseContext)
}

/** Validates that the given [value] is non-null and returns a non-nullable reference to it. */
fun <T> validateNotNull(value: T?, parseContext: ParseContext? = null, message: String): T =
    validateNotNull(value, parseContext) { message }

/** Validates that the given [value] is non-null and returns a non-nullable reference to it. */
inline fun <T> validateNotNull(
    value: T?,
    parseContext: ParseContext? = null,
    crossinline block: () -> String
): T {
    if (value != null) return value
    throw ValidationException(block(), parseContext)
}

/** Upcasts any [Throwables] thrown by the [block] to [ValidationException]s. */
inline fun <T : Any?> upcastThrown(
    context: ParseContext? = null,
    crossinline block: () -> T
): T = try { block() } catch (e: Throwable) {
    throw ValidationException(e.message ?: e::class.toString(), context)
}
