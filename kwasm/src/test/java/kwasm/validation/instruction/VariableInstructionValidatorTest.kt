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
import kwasm.ast.type.ValueType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VariableInstructionValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun localGet_isInvalid_whenLocalNotDefined_noLocals() = parser.with {
        val instruction = "local.get 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains("No local with index: 0 is defined")
    }

    @Test
    fun localGet_isInvalid_whenLocalNotDefined_indexNotFound_byInt() = parser.with {
        val instruction = "local.get 1".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("No local with index: 1 is defined")
    }

    @Test
    fun localGet_isInvalid_whenLocalNotDefined_indexNotFound_byIdentifier() = parser.with {
        val instruction = "local.get \$myLocal".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("No local with index: \$myLocal is defined")
    }

    @Test
    fun localGet_isValid_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY.withLocals("(local i32) (local i64)")
        assertThat("local.get 0".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I32)
        assertThat("local.get 1".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I64)
    }

    @Test
    fun localGet_isValid_byIdentifier() = parser.with {
        val context = EMPTY_FUNCTION_BODY.withLocals("(local \$foo i32) (local \$bar i64)")

        assertThat("local.get \$foo".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I32)
        assertThat("local.get \$bar".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I64)
    }

    @Test
    fun localSet_isInvalid_whenLocalNotDefined_noLocals() = parser.with {
        val instruction = "local.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains("No local with index: 0 is defined")
    }

    @Test
    fun localSet_isInvalid_whenLocalNotDefined_indexNotFound_byInt() = parser.with {
        val instruction = "local.set 1".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("No local with index: 1 is defined")
    }

    @Test
    fun localSet_isInvalid_whenLocalNotDefined_indexNotFound_byIdentifier() = parser.with {
        val instruction = "local.set \$myLocal".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("No local with index: \$myLocal is defined")
    }

    @Test
    fun localSet_isInvalid_whenStack_isEmpty() = parser.with {
        val instruction = "local.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("local.set expects the stack to be non-empty")
    }

    @Test
    fun localSet_isInvalid_whenStack_containsWrongType() = parser.with {
        val instruction = "local.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                EMPTY_FUNCTION_BODY.withLocals("(local i32)")
                    .pushStack(ValueType.I64)
            )
        }
        assertThat(e).hasMessageThat().contains("Local 0 has type i32, but top of stack is i64")
    }

    @Test
    fun localSet_isValid_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withLocals("(local i32 i64)")
        assertThat(
            "local.set 0".parseInstruction().validate(context.pushStack(ValueType.I32)).stack
        ).isEmpty()
        assertThat(
            "local.set 1".parseInstruction().validate(context.pushStack(ValueType.I64)).stack
        ).isEmpty()
    }

    @Test
    fun localSet_isValid_byIdentifier() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withLocals("(local \$foo i32) (local \$bar i64)")

        assertThat(
            "local.set \$foo".parseInstruction().validate(context.pushStack(ValueType.I32)).stack
        ).isEmpty()
        assertThat(
            "local.set \$bar".parseInstruction().validate(context.pushStack(ValueType.I64)).stack
        ).isEmpty()
    }

    @Test
    fun localTee_isInvalid_whenLocalNotDefined_noLocals() = parser.with {
        val instruction = "local.tee 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains("No local with index: 0 is defined")
    }

    @Test
    fun localTee_isInvalid_whenLocalNotDefined_indexNotFound_byInt() = parser.with {
        val instruction = "local.tee 1".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("No local with index: 1 is defined")
    }

    @Test
    fun localTee_isInvalid_whenLocalNotDefined_indexNotFound_byIdentifier() = parser.with {
        val instruction = "local.tee \$myLocal".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("No local with index: \$myLocal is defined")
    }

    @Test
    fun localTee_isInvalid_whenStack_isEmpty() = parser.with {
        val instruction = "local.tee 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withLocals("(local i32)"))
        }
        assertThat(e).hasMessageThat().contains("local.tee expects the stack to be non-empty")
    }

    @Test
    fun localTee_isInvalid_whenStack_containsWrongType() = parser.with {
        val instruction = "local.tee 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                EMPTY_FUNCTION_BODY.withLocals("(local i32)")
                    .pushStack(ValueType.I64)
            )
        }
        assertThat(e).hasMessageThat().contains("Local 0 has type i32, but top of stack is i64")
    }

    @Test
    fun localTee_isValid_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withLocals("(local i32 i64)")
        assertThat(
            "local.tee 0".parseInstruction().validate(context.pushStack(ValueType.I32)).stack
        ).containsExactly(ValueType.I32)
        assertThat(
            "local.tee 1".parseInstruction().validate(context.pushStack(ValueType.I64)).stack
        ).containsExactly(ValueType.I64)
    }

    @Test
    fun localTee_isValid_byIdentifier() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withLocals("(local \$foo i32) (local \$bar i64)")

        assertThat(
            "local.tee \$foo".parseInstruction().validate(context.pushStack(ValueType.I32)).stack
        ).containsExactly(ValueType.I32)
        assertThat(
            "local.tee \$bar".parseInstruction().validate(context.pushStack(ValueType.I64)).stack
        ).containsExactly(ValueType.I64)
    }

    @Test
    fun globalGet_isInvalid_whenGlobalNotDefined_noGlobals() = parser.with {
        val instruction = "global.get 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains("No global with index: 0 is defined")
    }

    @Test
    fun globalGet_isInvalid_whenGlobalNotDefined_indexNotFound_byInt() = parser.with {
        val instruction = "global.get 1".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withGlobal("(global i32)"))
        }
        assertThat(e).hasMessageThat().contains("No global with index: 1 is defined")
    }

    @Test
    fun globalGet_isInvalid_whenGlobalNotDefined_indexNotFound_byIdentifier() = parser.with {
        val instruction = "global.get \$myGlobal".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withGlobal("(global i32)"))
        }
        assertThat(e).hasMessageThat().contains("No global with index: \$myGlobal is defined")
    }

    @Test
    fun globalGet_isValid_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withGlobal("(global i32)")
            .withGlobal("(global (mut i64))")
        assertThat("global.get 0".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I32)
        assertThat("global.get 1".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I64)
    }

    @Test
    fun globalGet_isValid_byIdentifier() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withGlobal("(global \$foo i32)")
            .withGlobal("(global \$bar (mut i64))")

        assertThat("global.get \$foo".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I32)
        assertThat("global.get \$bar".parseInstruction().validate(context).stack)
            .containsExactly(ValueType.I64)
    }

    @Test
    fun globalSet_isInvalid_whenGlobalNotDefined_noGlobals() = parser.with {
        val instruction = "global.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY)
        }
        assertThat(e).hasMessageThat().contains("No global with index: 0 is defined")
    }

    @Test
    fun globalSet_isInvalid_whenGlobalNotDefined_indexNotFound_byInt() = parser.with {
        val instruction = "global.set 1".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withGlobal("(global (mut i32))"))
        }
        assertThat(e).hasMessageThat().contains("No global with index: 1 is defined")
    }

    @Test
    fun globalSet_isInvalid_whenGlobalNotDefined_indexNotFound_byIdentifier() = parser.with {
        val instruction = "global.set \$myGlobal".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withGlobal("(global (mut i32))"))
        }
        assertThat(e).hasMessageThat().contains("No global with index: \$myGlobal is defined")
    }

    @Test
    fun globalSet_isInvalid_ifGlobal_isImmutable() = parser.with {
        val instruction = "global.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withGlobal("(global i32)"))
        }
        assertThat(e).hasMessageThat()
            .contains("global.set requires global to be mutable, but 0 is not")
    }

    @Test
    fun globalSet_isInvalid_whenStack_isEmpty() = parser.with {
        val instruction = "global.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(EMPTY_FUNCTION_BODY.withGlobal("(global (mut i32))"))
        }
        assertThat(e).hasMessageThat().contains("global.set expects the stack to be non-empty")
    }

    @Test
    fun globalSet_isInvalid_whenStack_containsWrongType() = parser.with {
        val instruction = "global.set 0".parseInstruction()
        val e = assertThrows(ValidationException::class.java) {
            instruction.validate(
                EMPTY_FUNCTION_BODY.withGlobal("(global (mut i32))")
                    .pushStack(ValueType.I64)
            )
        }
        assertThat(e).hasMessageThat().contains("Global 0 has type i32, but top of stack is i64")
    }

    @Test
    fun globalSet_isValid_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withGlobal("(global (mut i32))")
            .withGlobal("(global (mut i64))")
        assertThat(
            "global.set 0".parseInstruction().validate(context.pushStack(ValueType.I32)).stack
        ).isEmpty()
        assertThat(
            "global.set 1".parseInstruction().validate(context.pushStack(ValueType.I64)).stack
        ).isEmpty()
    }

    @Test
    fun globalSet_isValid_byIdentifier() = parser.with {
        val context = EMPTY_FUNCTION_BODY
            .withGlobal("(global \$foo (mut i32))")
            .withGlobal("(global \$bar (mut i64))")

        assertThat(
            "global.set \$foo".parseInstruction().validate(context.pushStack(ValueType.I32)).stack
        ).isEmpty()
        assertThat(
            "global.set \$bar".parseInstruction().validate(context.pushStack(ValueType.I64)).stack
        ).isEmpty()
    }

    private fun ValidationContext.FunctionBody.withLocals(
        localSource: String
    ): ValidationContext.FunctionBody {
        val localNodes = with(parser) { localSource.parseLocals() }
        val contextLocals = locals.toMutableIndex()
        localNodes.forEach {
            contextLocals[it.id] = it.valueType
        }
        return copy(locals = contextLocals)
    }

    private fun ValidationContext.FunctionBody.withGlobal(
        globalSource: String
    ): ValidationContext.FunctionBody {
        val globalNode = with(parser) { globalSource.parseGlobal() }
        val contextGlobals = globals.toMutableIndex()
        contextGlobals[globalNode.id] = globalNode.globalType
        return copy(globals = contextGlobals)
    }
}
