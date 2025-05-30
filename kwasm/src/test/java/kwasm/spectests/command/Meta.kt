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

package kwasm.spectests.command

import kwasm.ast.Identifier
import kwasm.format.text.ParseResult
import kwasm.format.text.token.Token
import kwasm.spectests.execution.ScriptContext

/**
 * Defines meta-commands for the WebAssembly scripting language.
 */
sealed class Meta : Command<Unit> {
    /** Identifies a subscript. */
    data class Script(val scriptName: Identifier.Label?, val script: List<Command<*>>) : Meta() {
        override fun execute(context: ScriptContext) {
            TODO("Not yet implemented")
        }
    }

    /** Reads a module or a script from a file. */
    data class Input(val scriptName: Identifier.Label?, val fileName: String) : Meta() {
        override fun execute(context: ScriptContext) {
            TODO("Not yet implemented")
        }
    }

    /** Outputs a module to a file (or stdout if file is not specified). */
    data class Output(val moduleName: Identifier.Label?, val fileName: String?) : Meta() {
        override fun execute(context: ScriptContext) {
            TODO("Not yet implemented")
        }
    }
}

fun List<Token>.parseMeta(fromIndex: Int): ParseResult<out Meta>? {
    return null
}
