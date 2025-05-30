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

package kwasm.runtime

import kwasm.KWasmRuntimeException
import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/conventions.html#contexts):
 *
 * Where `I` is a [IdentifierContext]:
 *
 * ```
 *   I  ::=   { types       (id?)*,
 *              funcs       (id?)*,
 *              tables      (id?)*,
 *              mems        (id?)*,
 *              globals     (id?)*,
 *              locals      (id?)*,
 *              labels      (id?)*,
 *              typedefs    functype*   }
 * ```
 */
data class IdentifierContext(
    val types: MutableMap<Int?, Identifier.Type> = mutableMapOf(),
    val functions: MutableMap<Int?, Identifier.Function> = mutableMapOf(),
    val tables: MutableMap<Int?, Identifier.Table> = mutableMapOf(),
    val mems: MutableMap<Int?, Identifier.Memory> = mutableMapOf(),
    val globals: MutableMap<Int?, Identifier.Global> = mutableMapOf(),
    val locals: MutableMap<Int?, Identifier.Local> = mutableMapOf(),
    val labels: MutableMap<Int?, Identifier.Local> = mutableMapOf(),
    val typedefs: MutableMap<FunctionType, Identifier.TypeDef> = mutableMapOf()
) {
    /** Gets an [Identifier] by its registered [id], or `null` if none is registered yet. */
    inline fun <reified T : Identifier> get(id: Int?): T? = when (T::class) {
        Identifier.Type::class -> types[id] as T?
        Identifier.Function::class -> functions[id] as T?
        Identifier.Table::class -> tables[id] as T?
        Identifier.Memory::class -> mems[id] as T?
        Identifier.Global::class -> globals[id] as T?
        Identifier.Local::class -> locals[id] as T?
        Identifier.Label::class -> labels[id] as T?
        else -> throw KWasmRuntimeException(
            "Record lookup for ${T::class.java} by uniqueId is not supported"
        )
    }

    /** Gets an [Identifier.TypeDef] by its [funcType], or `null` if none is registered yet. */
    fun get(funcType: FunctionType): Identifier.TypeDef? = typedefs[funcType]

    /** Registers an [Identifier] with the [IdentifierContext]. */
    fun <T : Identifier> register(identifier: Identifier): T = compute(identifier)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Identifier> compute(identifier: Identifier): T {
        val lookup = when (identifier) {
            is Identifier.Type -> types
            is Identifier.Function -> functions
            is Identifier.Table -> tables
            is Identifier.Memory -> mems
            is Identifier.Global -> globals
            is Identifier.Local -> locals
            is Identifier.Label -> labels
            is Identifier.TypeDef -> {
                typedefs.computeIfAbsent(identifier.funcType) { identifier }
                return typedefs[identifier.funcType] as T
            }
        }

        if (identifier.unique !in lookup) {
            (lookup as MutableMap<Int?, Identifier>)[identifier.unique] = identifier
        }

        return lookup[identifier.unique] as T
    }
}
