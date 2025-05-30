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

import kwasm.ParseRule
import kwasm.format.text.type.parseGlobalType
import kwasm.validation.ValidationContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GlobalTypeValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun validImmutableGlobalTypes_areValid() = parser.with {
        "i32".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)

        "i64".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)

        "f32".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)

        "f64".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }

    @Test
    fun validMutableGlobalTypes_areValid() = parser.with {
        "(mut i32)".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)

        "(mut i64)".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)

        "(mut f32)".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)

        "(mut f64)".tokenize()
            .parseGlobalType(0).astNode
            .validate(ValidationContext.EMPTY_MODULE)
    }
}
