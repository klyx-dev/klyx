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

package kwasm.format.text.whitespace

/**
 * Format whitespace elements are spaces, tabs, newlines, and carriage returns.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/lexical.html#text-format):
 *
 * ```
 *   format ::= ' ' | U+09 | U+0A | U+0D
 * ```
 */
object Format {
    val PATTERN_STRING = "[ \t\n\r]+"
    val PATTERN = object : ThreadLocal<Regex>() {
        override fun initialValue(): Regex = PATTERN_STRING.toRegex()
    }
}
