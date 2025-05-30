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

package kwasm.validation.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.ast.Identifier
import kwasm.ast.type.GlobalType
import kwasm.ast.type.ValueType
import kwasm.ast.util.MutableAstNodeIndex
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpressionValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun isInvalid_whenConstantRequired_butNonConstantIncluded() = parser.with {
        assertThrows(ValidationException::class.java) {
            "(i32.add (i32.const 0) (i32.const 1))".parseExpression()
                .validateConstant(null, EMPTY_FUNCTION_BODY)
        }.also {
            assertThat(it).hasMessageThat().contains("Constant expressions may only contain")
        }
    }

    @Test
    fun isInvalid_whenConstantRequired_butGlobalGet_referencesMutable() = parser.with {
        val globals = MutableAstNodeIndex<GlobalType>()
        globals[Identifier.Global("\$foo")] = GlobalType(ValueType.I32, mutable = true)
        val context = EMPTY_FUNCTION_BODY.copy(globals = globals)

        assertThrows(ValidationException::class.java) {
            "global.get \$foo".parseExpression().validateConstant(null, context)
        }.also {
            assertThat(it).hasMessageThat().contains("Constant expressions may only contain")
        }
    }

    @Test
    fun isInvalid_whenResultStack_isInvalid_expectingEmpty() = parser.with {
        assertThrows(ValidationException::class.java) {
            "i32.const 0".parseExpression().validateConstant(null, EMPTY_FUNCTION_BODY)
        }
    }

    @Test
    fun isInvalid_whenResultStack_isInvalid_wrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            "i32.const 0".parseExpression().validateConstant(ValueType.F32, EMPTY_FUNCTION_BODY)
        }
    }

    @Test
    fun isInvalid_whenResultStack_isInvalid_moreThanOneLeftInStack() = parser.with {
        assertThrows(ValidationException::class.java) {
            "i32.const 0 i32.const 1".parseExpression().validate(ValueType.I32, EMPTY_FUNCTION_BODY)
        }
    }

    @Test
    fun isValid_whenEmpty_andResultTypeIsNull() = parser.with {
        val result = "".parseExpression().validate(null, EMPTY_FUNCTION_BODY)
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun isValid_whenResultTypeIsNull_andExpressionEndResultIsEmpty() = parser.with {
        val result = "(i32.add (i32.const 1) (i32.const 2)) (drop)".parseExpression()
            .also { println(it) }
            .validate(null, EMPTY_FUNCTION_BODY)
        assertThat(result.stack).isEmpty()
    }

    @Test
    fun isValid_whenResultTypeMatchesRequiredType() = parser.with {
        val result = "(i32.add (i32.const 1) (i32.const 2))".parseExpression()
            .validate(ValueType.I32, EMPTY_FUNCTION_BODY)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    @Test
    fun isValid_whenConstantRequired_globalGet_globalIsConstant() = parser.with {
        val globals = MutableAstNodeIndex<GlobalType>()
        globals[Identifier.Global("\$foo")] = GlobalType(ValueType.I32, mutable = false)
        val context = EMPTY_FUNCTION_BODY.copy(globals = globals)

        val result = "(global.get \$foo)".parseExpression()
            .validateConstant(ValueType.I32, context)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }

    @Test
    fun isValid_whenConstantRequired_usingConstant() = parser.with {
        val result = "i32.const 1".parseExpression()
            .validateConstant(ValueType.I32, EMPTY_FUNCTION_BODY)
        assertThat(result.stack).containsExactly(ValueType.I32)
    }
}
