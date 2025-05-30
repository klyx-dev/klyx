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

import kwasm.KWasmRuntimeException
import kwasm.api.HostFunction
import kwasm.api.HostFunctionContext
import kwasm.api.functionType
import kwasm.ast.Identifier
import kwasm.ast.instruction.flatten
import kwasm.ast.module.Index
import kwasm.ast.module.WasmFunction
import kwasm.ast.type.FunctionType
import kwasm.ast.type.ValueType
import kwasm.runtime.instruction.executeFlattened
import kwasm.runtime.stack.Activation
import kwasm.runtime.stack.OperandStack
import kwasm.runtime.util.LocalIndex

/**
 * Represents either a [WasmFunction] from a [ModuleInstance], or a [HostFunction] exposed to the
 * [ModuleInstance] via imports.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#function-instances):
 *
 * A function instance is the runtime representation of a function. It effectively is a closure of
 * the original function over the runtime module instance of its originating module. The module
 * instance is used to resolve references to other definitions during execution of the function.
 *
 * ```
 *   funcinst   ::= {type functype, module moduleinst, code func}
 *                  {type functype, hostcode hostfunc}
 *   hostfunc ::= ...
 * ```
 *
 * A host function is a function expressed outside WebAssembly but passed to a module as an import.
 */
sealed class FunctionInstance(open val type: FunctionType) {
    /**
     * Executes the [FunctionInstance].
     */
    internal abstract fun execute(context: ExecutionContext): ExecutionContext

    /**
     * A function from within a [kwasm.ast.module.WasmModule]'s [ModuleInstance].
     *
     * From
     * [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#function-calls):
     *
     * ```
     *   Invocation of function address `a`
     * ```
     *
     * 1. Assert: due to validation, `S.funcs\[a]` exists. (note: already done by this point)
     * 1. Let `f` be the function instance, `S.funcs\[a]`.
     * 1. Let `[t^n_1] -> [t^m_2]` be the function type `f.type`.
     * 1. Assert: due to validation, `m <= 1`.
     * 1. Let `t*` be the list of value types `f.code.locals`.
     * 1. Let `instr* end` be the expression `f.code.body`.
     * 1. Assert: due to validation, `n` values are on the top of the stack.
     * 1. Pop the values `val_n` from the stack.
     * 1. Let `val*_0` be the list of zero values of types `t*`.
     * 1. Let `F` be the frame `{module f.module, locals val_n val*_0}`.
     * 1. Push the activation of `F` with arity `m` to the stack.
     * 1. Execute the instruction `block [t^m_2] instr* end`.
     *
     * ```
     *   Returning from a function
     * ```
     *
     * When the end of a function is reached without a jump (i.e., `return`) or trap aborting it,
     * then the following steps are performed.
     *
     * 1. Let `F` be the current frame.
     * 1. Let `n` be the arity of the activation of `F`.
     * 1. Assert: due to validation, there are `n` values on the top of the stack.
     * 1. Pop the results `val_n` from the stack.
     * 1. Assert: due to validation, the frame `F` is now on the top of the stack.
     * 1. Pop the frame from the stack.
     * 1. Push `val_n` back to the stack.
     * 1. Jump to the instruction after the original call.
     */
    data class Module(
        val moduleInstance: ModuleInstance,
        val code: WasmFunction
    ) : FunctionInstance(
        code.typeUse?.index?.let { moduleInstance.types[it] }
            ?: code.typeUse?.functionType
            ?: FunctionType(emptyList(), emptyList())
    ) {
        val flattenedInstructions by lazy { code.instructions.flatten(0) }

        @Suppress("UNCHECKED_CAST")
        override fun execute(context: ExecutionContext): ExecutionContext {
            val type = type

            if (type.returnValueEnums.size > 1)
                throw KWasmRuntimeException("Function cannot have more than one return value.")

            if (context.stacks.operands.height < type.parameters.size)
                throw KWasmRuntimeException(
                    "Not enough data on the stack to call function with type: $type"
                )
            val arguments = type.parameters.reversed().map {
                val arg = context.stacks.operands.pop()
                when (it.valType) {
                    ValueType.I32 -> arg as? IntValue
                    ValueType.I64 -> arg as? LongValue
                    ValueType.F32 -> arg as? FloatValue
                    ValueType.F64 -> arg as? DoubleValue
                } ?: throw KWasmRuntimeException(
                    "Parameter on stack does not match required parameter type for function " +
                        "with type: $type"
                )
            }.reversed()

            val localIndex = LocalIndex()
            arguments.zip(type.parameters).forEach { (argValue, param) ->
                param.id.takeIf { it.stringRepr != null }?.let { localIndex.add(argValue, it) }
                    ?: localIndex.add(argValue)
            }
            code.locals.forEach { local ->
                local.id?.let {
                    localIndex.add(local.valueType.zeroValue, it)
                } ?: localIndex.add(local.valueType.zeroValue)
            }

            val activation = Activation(
                code.id?.let { Index.ByIdentifier(it) }
                    ?: Index.ByInt(-1) as Index<Identifier.Function>,
                localIndex,
                context.moduleInstance,
                type.returnValueEnums.size
            )

            context.stacks.activations.push(activation)
            val activationStackHeightAtCallTime = context.stacks.activations.height
            val functionContext = context.copy(
                // Use a fresh op stack for the function.
                stacks = context.stacks.copy(operands = OperandStack()),
                instructionIndex = 0,
                flattenedInstructions = flattenedInstructions
            )

            // Execute the function.
            val resultContext = flattenedInstructions.executeFlattened(functionContext)

            if (resultContext.stacks.activations.height == activationStackHeightAtCallTime) {
                // Function exited without a `return` instruction, so we need to pop our own frame.
                context.stacks.activations.pop()
            }

            type.returnValueEnums.map { expectedResult ->
                resultContext.stacks.operands.pop().also {
                    it.checkType(expectedResult.valType)
                }
            }.reversed().forEach(context.stacks.operands::push)

            return context
        }
    }

    /** A function supplied by the host. */
    data class Host(
        override val type: FunctionType,
        val hostFunction: HostFunction<*>
    ) : FunctionInstance(type) {
        override fun execute(context: ExecutionContext): ExecutionContext {
            // Collect the parameters to feed to the host function.
            if (context.stacks.operands.height < type.parameters.size)
                throw KWasmRuntimeException(
                    "Not enough data on the stack to call function with type: $type"
                )
            val arguments = type.parameters.reversed().map {
                val arg = context.stacks.operands.pop()
                when (it.valType) {
                    ValueType.I32 -> arg as? IntValue
                    ValueType.I64 -> arg as? LongValue
                    ValueType.F32 -> arg as? FloatValue
                    ValueType.F64 -> arg as? DoubleValue
                } ?: throw KWasmRuntimeException(
                    "Parameter on stack does not match required parameter type for function " +
                        "with type: $type"
                )
            }.reversed()

            // Get the available memory at our current call location, if we have one.
            val currentMemoryAddress = context.stacks.activations.peek()
                ?.module?.memoryAddresses?.getOrNull(0)
            val memoryForFunction = currentMemoryAddress?.let { context.store.memories[it.value] }

            // Call the function.
            val result = hostFunction.invoke(
                arguments,
                object : HostFunctionContext {
                    override val memory: Memory? = memoryForFunction
                }
            )

            if (result != EmptyValue) {
                // If there was a result from the function, push it onto the stack.
                context.stacks.operands.push(result)
            }
            return context
        }
    }

    companion object {
        /**
         * From [the docs](https://webassembly.github.io/spec/core/exec/modules.html#alloc-func):
         *
         * 1. Let `func` be the function to allocate and `moduleinst` its module instance.
         * 1. Let `a` be the first free function address in `S`.
         * 1. Let `functype` be the function type `moduleinst.types\[func.type]`.
         * 1. Let `funcinst` be the function instance
         *    `{type functype, module moduleinst, code func}`.
         * 1. Append `funcinst` to the `funcs` of `S`.
         * 1. Return `a`.
         */
        fun Store.allocate(
            moduleInstance: ModuleInstance,
            wasmFunction: WasmFunction
        ): Store.Allocation<Address.Function> =
            allocateFunction(Module(moduleInstance, wasmFunction))

        /**
         * From [the
         * docs](https://webassembly.github.io/spec/core/exec/modules.html#host-functions):
         *
         * 1. Let `hostfunc` be the host function to allocate and `functype` its function type.
         * 1. Let `a` be the first free function address in `S`.
         * 1. Let `funcinst` be the function instance `{type functype, hostcode hostfunc}`.
         * 1. Append `funcinst` to the `funcs` of `S`.
         * 1. Return `a`.
         */
        fun Store.allocate(hostFunction: HostFunction<*>): Store.Allocation<Address.Function> =
            allocateFunction(Host(hostFunction.functionType, hostFunction))
    }
}

/** Converts a [HostFunction] into a Function Instance. */
internal fun HostFunction<*>.toFunctionInstance(): FunctionInstance.Host =
    FunctionInstance.Host(functionType, this)
