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

package kwasm.format.text.whitespace

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseContext
import kwasm.format.ParseException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CommentTest {
    @Test
    fun stripComments_stripsComments_fromEmpty() {
        val result = Comment.stripComments("", CONTEXT)
        assertThat(result.tokens).isEmpty()
    }

    @Test
    fun stripComments_withSingleLineComment() {
        val result = Comment.stripComments(";; this is a line comment", CONTEXT)
        assertThat(result.tokens).isEmpty()
    }

    @Test
    fun stripComments_withBlockComment() {
        val result = Comment.stripComments(
            """
            (;
              this is a multiline
                block comment
               (; with an inner block comment ;)
            ;)
            """.trimIndent()
        )
        assertThat(result.tokens).isEmpty()
    }

    @Test
    fun stripComments_withMultipleComments_returnsTokensBetweenThem() {
        val commentBlob =
            """
            ;; first line comment
            ;; second line comment
            ;; third line comment
            (;  a block comment, on a single line ;)
            (;
               a multiline block comment
            ;)
            """.trimIndent()
        val comments = commentBlob.split("\n")
        var result = Comment.stripComments(commentBlob, CONTEXT)
        assertThat(result.tokens.size).isEqualTo(4)
        result.tokens.forEachIndexed { index, token ->
            assertThat(token.sequence).isEqualTo("")
            assertThat(token.context?.lineNumber).isEqualTo(index + 1)
            assertThat(token.context?.column).isEqualTo(comments[index].length + 1)
        }

        result = Comment.stripComments(
            """
            (; a block comment ;) with some text before (; another block comment ;)
            """.trimIndent(),
            CONTEXT
        )
        assertThat(result.tokens.size).isEqualTo(1)
        assertThat(result.tokens[0].sequence).isEqualTo(" with some text before ")
        assertThat(result.tokens[0].context?.lineNumber).isEqualTo(1)
        assertThat(result.tokens[0].context?.column).isEqualTo(22)
    }

    @Test
    fun stripComments_fails_ifClosingSequence_forBlockComment_notFound() {
        assertThrows(ParseException::class.java) {
            Comment.stripComments("(; this isn't closed")
        }

        assertThrows(ParseException::class.java) {
            Comment.stripComments(
                """
                (; this isn't closed
                
                
                even with stuff down here
                """.trimIndent()
            )
        }

        assertThrows(ParseException::class.java) {
            Comment.stripComments(
                """
                (; this isn't closed
                
                       (; especially when nested ones aren't closed
                """.trimIndent()
            )
        }

        assertThrows(ParseException::class.java) {
            Comment.stripComments(
                """
                (; this isn't closed
                
                       (; even if this one is ;) 
                """.trimIndent()
            )
        }
    }

    @Test
    fun stripComments_withNestedBlockComments_worksAsIntended() {
        val source =
            """
            This is my first line.
            (;
                Here's a comment
                (; with an inner comment ;)
                
                and another (;
                    right here
                ;)    
            ;) Here's another line
            """.trimIndent()
        val result = Comment.stripComments(source, CONTEXT)
        assertThat(result.tokens).hasSize(2)
        val firstToken = result.tokens[0]
        assertThat(firstToken.sequence).isEqualTo("This is my first line.")
        assertThat(firstToken.context?.lineNumber).isEqualTo(1)
        assertThat(firstToken.context?.column).isEqualTo(1)

        val secondToken = result.tokens[1]
        assertThat(secondToken.sequence).isEqualTo(" Here's another line")
        assertThat(secondToken.context?.lineNumber).isEqualTo(9)
        assertThat(secondToken.context?.column).isEqualTo(3)
    }

    companion object {
        private val CONTEXT = ParseContext("MyFile.wast", 1, 1)
    }
}
