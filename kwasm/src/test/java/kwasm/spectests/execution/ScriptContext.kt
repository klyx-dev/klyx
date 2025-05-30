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

package kwasm.spectests.execution

import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import kwasm.ast.Identifier
import kwasm.ast.instruction.Expression
import kwasm.ast.module.WasmModule
import kwasm.ast.util.MutableAstNodeIndex
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.Value
import kwasm.runtime.instruction.execute
import kwasm.runtime.toValue

class ScriptContext(
    val modules: MutableAstNodeIndex<WasmModule> = MutableAstNodeIndex(),
    var programBuilder: KWasmProgram.Builder =
        KWasmProgram.Builder(ByteBufferMemoryProvider(4000000))
) {
    lateinit var program: KWasmProgram

    fun build() {
        program = programBuilder.build()
    }

    fun invoke(
        moduleIdentifier: Identifier.Label?,
        fnName: String,
        args: List<Expression>
    ): List<Value<*>> {
        val context = args.fold(EmptyExecutionContext()) { acc, expr ->
            expr.instructions.fold(acc) { accInner, instruction -> instruction.execute(accInner) }
        }
        val moduleName = if (moduleIdentifier != null) {
            programBuilder.modulesInOrder.find { it.second.identifier == moduleIdentifier }?.first
                ?: ""
        } else {
            // If no identifier was used, use the most-recently declared module.
            programBuilder.modulesInOrder.last().first
        }
        val fn = program.getFunction(moduleName, fnName)
        val argValues = context.stacks.operands.values.takeLast(fn.argCount)
            .map { it.value }
            .reversed()
        val answer = fn.invoke(*argValues.toTypedArray())
        return answer?.let { listOf(it.toValue()) } ?: emptyList()
    }

    fun getGlobal(moduleIdentifier: Identifier.Label?, globalName: String): Value<*> {
        val moduleName = if (moduleIdentifier != null) {
            programBuilder.modulesInOrder.find { it.second.identifier == moduleIdentifier }?.first
                ?: ""
        } else {
            // If no identifier was used, use the most-recently declared module.
            programBuilder.modulesInOrder.last().first
        }
        return program.getGlobal(moduleName, globalName).value.toValue()
    }
}
