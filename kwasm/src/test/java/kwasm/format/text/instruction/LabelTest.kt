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

package kwasm.format.text.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LabelTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("LabelTest.wat", 1, 1)

    @Test
    fun parsesLabel_evenFromEmptySource() {
        val result = tokenizer.tokenize("", context).parseLabel(0)
        assertThat(result.astNode.stringRepr).isNull()
        assertThat(result.parseLength).isEqualTo(0)
    }

    @Test
    fun parsesLabel_whenNonLabel() {
        val result = tokenizer.tokenize("()", context).parseLabel(0)
        assertThat(result.astNode.stringRepr).isNull()
        assertThat(result.parseLength).isEqualTo(0)
    }

    @Test
    fun parsesLabel_whenLabelExists() {
        val result = tokenizer.tokenize("\$thisIsMyLabel", context).parseLabel(0)
        assertThat(result.astNode.stringRepr).isEqualTo("\$thisIsMyLabel")
        assertThat(result.parseLength).isEqualTo(1)
    }

    @Test
    fun parsesLabel_whenLabelExists_notFirst() {
        val result = tokenizer.tokenize("\$thisOneIsIgnored \$thisOneIsPicked", context)
            .parseLabel(1)
        assertThat(result.astNode.stringRepr).isEqualTo("\$thisOneIsPicked")
        assertThat(result.parseLength).isEqualTo(1)
    }

    @Test
    fun throws_whenInvalidLabelIsFound() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("$", context).parseLabel(0)
        }
        assertThat(exception).hasMessageThat().contains("Invalid identifier")
    }
}
