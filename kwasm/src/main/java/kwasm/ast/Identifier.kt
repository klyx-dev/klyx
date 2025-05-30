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

package kwasm.ast

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
sealed class Identifier(
    open val unique: Int?,
    open val stringRepr: String?
) : AstNode {
    data class Type(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class Function(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class Table(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class Memory(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class Global(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class Local(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class Label(
        override val stringRepr: String? = null,
        override val unique: Int? = "$stringRepr".hashCode()
    ) : Identifier(unique, stringRepr) {
        override fun toString() = "$stringRepr"
    }

    data class TypeDef(
        val funcType: FunctionType,
        override val stringRepr: String? = null
    ) : Identifier(null, stringRepr) {
        override fun toString() = "$stringRepr"
    }
}
