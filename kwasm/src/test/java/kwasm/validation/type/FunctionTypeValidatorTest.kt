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

package kwasm.validation.type

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.format.text.type.parseFunctionType
import kwasm.validation.ValidationContext
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionTypeValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun empty_isValid() = parser.with {
        "(func)".tokenize()
            .parseFunctionType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun zeroResults_isValid() = parser.with {
        "(func (param i32))".tokenize()
            .parseFunctionType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun oneResult_isValid() = parser.with {
        "(func (param i32) (result i32))".tokenize()
            .parseFunctionType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun moreThanOneResult_isInvalid() = parser.with {
        val e = assertThrows(ValidationException::class.java) {
            "(func (param i32) (result i32) (result i64))".tokenize()
                .parseFunctionType(0).astNode
                .validate(ValidationContext.EMPTY_MODULE)
        }
        assertThat(e).hasMessageThat()
            .contains("Function types may not specify more than one result")
    }
}
