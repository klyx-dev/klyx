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

package kwasm.validation.module

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.validation.ValidationException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DataSegmentValidatorTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun invalid_ifMemoryDoesntExist() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (data (offset (i32.const 0)) "test")
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat().contains("No memory found with index 0 in module")
        }
    }

    @Test
    fun invalid_ifMemoryWithGivenIndex_doesntExist() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (memory ${"$"}foo 0 1)
                    (data ${"$"}bar (offset (i32.const 0)) "test")
                )
            """.trimIndent().parseModule().validate()
        }.also {
            assertThat(it).hasMessageThat().contains("No memory found with index \$bar in module")
        }
    }

    @Test
    fun invalid_ifOffsetHasWrongType() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (memory 0 1337)
                    (data (offset (i64.const 0)) "test")
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun invalid_ifOffsetNonConstant() = parser.with {
        assertThrows(ValidationException::class.java) {
            """
                (module
                    (memory 0 1337)
                    (data (offset (i32.add (i32.const 0) (i32.const 1))) "test")
                )
            """.trimIndent().parseModule().validate()
        }
    }

    @Test
    fun valid() = parser.with {
        """
            (module
                (memory 0 1337)
                (data (offset (i32.const 0)) "test")
            )
        """.trimIndent().parseModule().validate()
    }
}
