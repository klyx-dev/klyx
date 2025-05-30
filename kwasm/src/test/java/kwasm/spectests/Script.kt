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

package kwasm.spectests

import kwasm.KWasmProgram
import kwasm.format.ParseContext
import kwasm.format.text.Tokenizer
import kwasm.format.text.token.Token
import kwasm.spectests.command.parseAndRunCommand
import kwasm.spectests.execution.ScriptContext
import java.io.Reader

fun runScript(input: Reader, parseContext: ParseContext, programBuilder: KWasmProgram.Builder) {
    runScript(Tokenizer().tokenize(input, parseContext), programBuilder)
}

fun runScript(tokens: List<Token>, programBuilder: KWasmProgram.Builder) {
    var position = 0
    val scriptContext = ScriptContext(programBuilder = programBuilder)
    while (position < tokens.size) {
        val result = tokens.parseAndRunCommand(position, scriptContext)
        position += result.parseLength
    }
}
