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

package kwasm.ast.module

import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType

/**
 * Represents a reference to a [Type] definition.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/modules.html#text-typeuse):
 *
 * It may optionally be augmented by explicit inlined parameter and result declarations. That allows
 * binding symbolic identifiers to name the local indices of parameters. If inline declarations are
 * given, then their types must match the referenced function type.
 *
 * ```
 *   typeuse(I) ::=
 *          ‘(’ ‘type’ x:typeidx(I) ‘)’
 *              => x,I′ (if I.typedefs[x] = [t^n_1] -> [t*^2] ∧ I′ = {locals (ϵ)^n})
 *          ‘(’ ‘type’ x:typeidx(I) ‘)’ (t_1:param)* (t_2:result)*
 *              => x,I′ (if I.typedefs[x] = [t*_1] -> [t*_2] ∧ I′ = {locals id(param)*} well-formed)
 * ```
 *
 * **Note:**
 * Both productions overlap for the case that the function type is `[] -> []`. However, in that
 * case, they also produce the same results, so that the choice is immaterial. The well-formedness
 * condition on `I′` ensures that the parameters do not contain duplicate identifier.
 *
 * A `typeuse` may also be replaced entirely by inline parameter and result declarations. In that
 * case, a type index is automatically inserted:
 *
 * ```
 * (t_1:param)* (t_2:result)* ≡ ‘(’ ‘type' x ‘)’ param* result*
 * ```
 *
 * where `x` is the smallest existing type index whose definition in the current module is the
 * function type `[t*_1] -> [t*_2]`. If no such index exists, then a new type definition of the
 * form `‘(’ ‘type’ ‘(’ ‘func’ param* result ‘)’ ‘)’` is inserted at the end of the module.
 *
 * Abbreviations are expanded in the order they appear, such that previously inserted type
 * definitions are reused by consecutive expansions.
 */
data class TypeUse(
    val index: Index<Identifier.Type>?,
    val params: List<Param>,
    val results: List<Result>
) : AstNode {
    /** A [FunctionType] matching the [TypeUse]. */
    val functionType: FunctionType = FunctionType(params, results)

    /** Converts the [TypeUse] into a [Type]. */
    fun toType(): Type =
        Type((index as? Index.ByIdentifier<Identifier.Type>)?.indexVal, functionType)

    /** Converts the [TypeUse] into a [ResultType]. */
    fun toResultType(): ResultType = ResultType(results.takeIf { it.isNotEmpty() }?.last(), index)
}
