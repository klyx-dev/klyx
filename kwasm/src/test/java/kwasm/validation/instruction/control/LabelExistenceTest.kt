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

package kwasm.validation.instruction.control

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.type.ResultType
import kwasm.format.text.instruction.parseLabel
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class LabelExistenceTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifLabelDoesntExist_byIdentifier() = parser.with {
        val label = "$0".tokenize().parseLabel(0).astNode
        val context = EMPTY_FUNCTION_BODY

        assertThrows(ValidationException::class.java) {
            context.validateLabelExists(Index.ByIdentifier(label))
        }.also {
            assertThat(it).hasMessageThat().contains("Label with index: $0 not found")
        }
    }

    @Test
    fun invalid_ifLabelDoesntExist_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY

        assertThrows(ValidationException::class.java) {
            context.validateLabelExists(Index.ByInt(0) as Index<Identifier.Label>)
        }.also {
            assertThat(it).hasMessageThat().contains("Label with index: 0 not found")
        }
    }

    @Test
    fun valid_ifLabelExists_byIdentifier() = parser.with {
        val label = "$0".tokenize().parseLabel(0).astNode
        val context = EMPTY_FUNCTION_BODY.prependLabel(label, ResultType(null))

        context.validateLabelExists(Index.ByIdentifier(label))
    }

    @Test
    fun valid_ifLabelExists_byInt() = parser.with {
        val context = EMPTY_FUNCTION_BODY.prependLabel(ResultType(null))
        context.validateLabelExists(Index.ByInt(0) as Index<Identifier.Label>)
    }
}
