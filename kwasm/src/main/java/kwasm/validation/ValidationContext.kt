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

import kwasm.ast.Identifier
import kwasm.ast.astNodeListOf
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Type
import kwasm.ast.module.TypeUse
import kwasm.ast.module.WasmFunction
import kwasm.ast.module.WasmModule
import kwasm.ast.type.GlobalType
import kwasm.ast.type.MemoryType
import kwasm.ast.type.ResultType
import kwasm.ast.type.TableType
import kwasm.ast.type.ValueType
import kwasm.ast.util.AstNodeIndex
import kwasm.ast.util.MutableAstNodeIndex

/**
 * Represents the context accrued throughout the validation process.
 *
 * [ValidationContext] objects contain the current [AstNode] being validated, as well as additional
 * context as specified in the WebAssembly Spec.
 *
 * From [the docs](https://webassembly.github.io/spec/core/valid/conventions.html#contexts):
 *
 * Validity of an individual definition is specified relative to a context, which collects relevant
 * information about the surrounding module and the definitions in scope:
 *
 * * Types: the list of types defined in the current module.
 * * Functions: the list of functions declared in the current module, represented by their function
 *   type.
 * * Tables: the list of tables declared in the current module, represented by their table type.
 * * Memories: the list of memories declared in the current module, represented by their memory
 *   type.
 * * Globals: the list of globals declared in the current module, represented by their global type.
 * * Locals: the list of locals declared in the current function (including parameters), represented
 *   by their value type.
 * * Labels: the stack of labels accessible from the current position, represented by their result
 *   type.
 * * Return: (the return type of the current function, represented as an optional result type that is
 *   absent when no return is allowed, as in free-standing expressions.
 *
 * In other words, a context contains a sequence of suitable types for each index space, describing
 * each defined entry in that space. Locals, labels and return type are only used for validating
 * instructions in function bodies, and are left empty elsewhere. The label stack is the only part
 * of the context that changes as validation of an instruction sequence proceeds.
 */
sealed class ValidationContext(
    open val types: AstNodeIndex<Type>,
    open val functions: AstNodeIndex<TypeUse>,
    open val tables: AstNodeIndex<TableType>,
    open val memories: AstNodeIndex<MemoryType>,
    open val globals: AstNodeIndex<GlobalType>
) {
    /** See [ValidationContext]. */
    data class Module(
        override val types: AstNodeIndex<Type>,
        override val functions: AstNodeIndex<TypeUse>,
        override val tables: AstNodeIndex<TableType>,
        override val memories: AstNodeIndex<MemoryType>,
        override val globals: AstNodeIndex<GlobalType>
    ) : ValidationContext(types, functions, tables, memories, globals) {
        fun toFunctionBody(
            locals: AstNodeIndex<ValueType> = AstNodeIndex(),
            labels: AstNodeIndex<ResultType> = AstNodeIndex(),
            returnType: ResultType? = null
        ): FunctionBody = FunctionBody(
            types,
            functions,
            tables,
            memories,
            globals,
            locals,
            labels,
            emptyList(),
            returnType
        )
    }

    /**
     * For validating [Instruction]s within a [WasmFunction] body or [Expression].
     *
     * See [ValidationContext].
     */
    data class FunctionBody(
        override val types: AstNodeIndex<Type>,
        override val functions: AstNodeIndex<TypeUse>,
        override val tables: AstNodeIndex<TableType>,
        override val memories: AstNodeIndex<MemoryType>,
        override val globals: AstNodeIndex<GlobalType>,
        val locals: AstNodeIndex<ValueType>,
        val labels: AstNodeIndex<ResultType>,
        val stack: List<ValueType>,
        val returnType: ResultType?
    ) : ValidationContext(types, functions, tables, memories, globals) {
        /** Returns the [ValueType] at the top of the stack without popping it. */
        fun peekStack(): ValueType? = stack.lastOrNull()

        /** Returns a new [FunctionBody], with the given [valueType] pushed onto the [stack]. */
        fun pushStack(valueType: ValueType): FunctionBody = copy(stack = stack + valueType)

        /**
         * Returns the top [ValueType] from the [stack], and a new [FunctionBody] with that top item
         * removed.
         */
        fun popStack(): Pair<ValueType?, FunctionBody> =
            stack.lastOrNull() to copy(stack = stack.dropLast(1))

        /**
         * Returns the top [count] [ValueType]s from the [stack], and a new [FunctionBody] with
         * those items removed.
         */
        fun popStack(count: Int): Pair<List<ValueType>, FunctionBody> =
            stack.asReversed().take(count).reversed() to copy(stack = stack.dropLast(count))

        /** Prepends a [ResultType] onto the [labels] list with the given identifier. */
        fun prependLabel(identifier: Identifier.Label, label: ResultType) =
            copy(labels = labels.toMutableIndex().prepend(identifier, label))

        /** Prepends a [ResultType] onto the [labels] list. */
        fun prependLabel(label: ResultType) = copy(labels = labels.toMutableIndex().prepend(label))
    }

    companion object {
        /** An empty [ValidationContext.Module] instance. */
        val EMPTY_MODULE = Module(
            types = AstNodeIndex(),
            functions = AstNodeIndex(),
            tables = AstNodeIndex(),
            memories = AstNodeIndex(),
            globals = AstNodeIndex()
        )

        /** An empty [ValidationContext.Function] instance. */
        val EMPTY_FUNCTION_BODY = FunctionBody(
            types = AstNodeIndex(),
            functions = AstNodeIndex(),
            tables = AstNodeIndex(),
            memories = AstNodeIndex(),
            globals = AstNodeIndex(),
            locals = AstNodeIndex(),
            labels = AstNodeIndex(),
            stack = emptyList(),
            returnType = null
        )
    }
}

/** Given a [WasmModule], creates a new [ValidationContext]. */
fun ValidationContext(module: WasmModule): ValidationContext.Module {
    val types = MutableAstNodeIndex<Type>()
    val functions = MutableAstNodeIndex<TypeUse>()
    val tables = MutableAstNodeIndex<TableType>()
    val memories = MutableAstNodeIndex<MemoryType>()
    val globals = MutableAstNodeIndex<GlobalType>()

    // TODO: remove upcastThrown calls when ParseContext is available on AstNodes.
    module.types.forEach {
        upcastThrown { types[it.id] = it }
    }
    module.imports.forEach { import ->
        when (val descriptor = import.descriptor) {
            is ImportDescriptor.Function -> {
                val typeUse = descriptor.typeUse
                types.addTypeUse(typeUse)

                upcastThrown { functions[descriptor.id] = typeUse }
            }
            is ImportDescriptor.Table -> {
                validate(tables[0] == null) {
                    "Cannot import a table when one is already defined by the module"
                }
                upcastThrown { tables[descriptor.id] = descriptor.tableType }
            }
            is ImportDescriptor.Memory -> {
                validate(memories[0] == null) {
                    "Cannot import a memory when one is already defined by the module"
                }
                upcastThrown { memories[descriptor.id] = descriptor.memoryType }
            }
            is ImportDescriptor.Global -> upcastThrown {
                globals[descriptor.id] = descriptor.globalType
            }
        }
    }
    module.functions.forEach {
        val typeUse = it.typeUse ?: TypeUse(
            null,
            astNodeListOf(),
            astNodeListOf()
        )
        types.addTypeUse(typeUse)

        upcastThrown { functions[it.id] = typeUse }
    }
    module.tables.forEach {
        upcastThrown { tables[it.id] = it.tableType }
    }
    module.memories.forEach {
        upcastThrown { memories[it.id] = it.memoryType }
    }
    module.globals.forEach {
        upcastThrown { globals[it.id] = it.globalType }
    }

    return ValidationContext.Module(types, functions, tables, memories, globals)
}

private fun MutableAstNodeIndex<Type>.addTypeUse(typeUse: TypeUse) {
    if (typeUse.index == null) {
        // Implicit type use, add it to types.
        this[null] = typeUse.toType()
    } else {
        // If the typeUse has an index, validate that there is a defined type for that index
        validate(this[typeUse.index] != null, parseContext = null) {
            "Function uses type ${typeUse.index}, but it's not declared by the module"
        }
    }
}
