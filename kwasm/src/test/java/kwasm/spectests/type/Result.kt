/*
 * Copyright 2021 Google LLC
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

package kwasm.spectests.type

import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.contextAt
import kwasm.format.text.isClosedParen
import kwasm.format.text.isKeyword
import kwasm.format.text.isOpenParen
import kwasm.format.text.parseLiteral
import kwasm.format.text.token.Token
import kwasm.runtime.Value
import kwasm.runtime.toValue

/**
 * Defines an expected result from an [Action] in a script.
 *
 * See [the docs](https://github.com/WebAssembly/spec/tree/master/interpreter#scripts)
 *
 * (<valueType>.const <value>)
 */
fun List<Token>.parseScriptResult(fromIndex: Int): ParseResult<Value<*>>? {
    var currentIndex = fromIndex
    if (!isOpenParen(currentIndex)) return null
    currentIndex++

    val result = if (isKeyword(currentIndex, "i32.const")) {
        parseLiteral(currentIndex + 1, Int::class).astNode.value.toValue()
    } else if (isKeyword(currentIndex, "i64.const")) {
        parseLiteral(currentIndex + 1, Long::class).astNode.value.toValue()
    } else if (isKeyword(currentIndex, "f32.const")) {
        parseLiteral(currentIndex + 1, Float::class).astNode.value.toValue()
    } else if (isKeyword(currentIndex, "f64.const")) {
        parseLiteral(currentIndex + 1, Double::class).astNode.value.toValue()
    } else return null

    currentIndex += 2

    if (!isClosedParen(currentIndex)) {
        throw ParseException("Expected close paren", contextAt(currentIndex))
    }
    currentIndex++

    return ParseResult(result, currentIndex - fromIndex)
}
