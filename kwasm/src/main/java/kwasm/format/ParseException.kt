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
 * An exception which is thrown by the text-based wasm interpreter when a parsing error occurs
 * during evaluation.
 */
data class ParseException(
    private val errorMsg: String,
    val parseContext: ParseContext? = null,
    val origin: Throwable? = null
) : Exception("(${parseContext ?: "Unknown Context"}) $errorMsg", origin)

/** Throws a [ParseException] if the [condition] is not met. */
inline fun parseCheck(
    context: ParseContext?,
    condition: Boolean,
    crossinline message: () -> String
) {
    if (!condition) throw ParseException(message(), context)
}

/** Throws a [ParseException] if the [condition] is not met. */
fun parseCheck(context: ParseContext?, condition: Boolean, message: String? = null) {
    parseCheck(context, condition) { message ?: "Parse Error" }
}

/** Returns a non-nullable [T] value if it's not null, otherwise: throws a [ParseException]. */
inline fun <T> parseCheckNotNull(
    context: ParseContext?,
    value: T?,
    crossinline message: () -> String
): T {
    if (value == null) throw ParseException(message(), context)
    return value
}

/** Returns a non-nullable [T] value if it's not null, otherwise: throws a [ParseException]. */
fun <T> parseCheckNotNull(context: ParseContext?, value: T?, message: String? = null): T {
    return parseCheckNotNull(context, value) { message ?: "Parse Error" }
}
