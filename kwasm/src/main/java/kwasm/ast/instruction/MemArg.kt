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

package kwasm.ast.instruction

import kwasm.ast.AstNode
import kwasm.ast.DeDupeableAstNode
import kotlin.math.floor
import kotlin.math.log

/**
 * Represents an [Argument] to a memory [Instruction].
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/instructions.html#text-memarg):
 *
 * The [offset] and [alignment] immediates to memory instructions are optional. The [offset]
 * defaults to `0` and the [alignment] to the storage size of the respective memory access, which is
 * its natural alignment.
 */
class MemArg(
    offset: Int? = null,
    private val alignment: Int
) : Argument,
    DeDupeableAstNode<MemArg> {
    val offset: UInt = (offset ?: 0).toUInt()
    override val valueAstNode: AstNode
        get() = this

    /**
     * Returns whether or not the value of [alignment] is well-formed at parse time.
     * It must be a power of 2.
     */
    fun isAlignmentWellFormed(): Boolean =
        alignment != 0 && log(alignment.toFloat(), 2.0f).let { floor(it) == it }

    /**
     * Returns whether or not the value of [alignment] is valid for the given [forByteWidth]
     * upper-bound.
     */
    fun isAlignmentValid(forByteWidth: Int? = null): Boolean =
        isAlignmentWellFormed() && (forByteWidth == null || alignment <= forByteWidth)

    /** Returns a canonical instance of [MemArg] matching this one, if it exists. */
    override fun deDupe(): MemArg = when (this) {
        ONE -> ONE
        TWO -> TWO
        FOUR -> FOUR
        EIGHT -> EIGHT
        else -> this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemArg

        if (offset != other.offset) return false
        if (alignment != other.alignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + alignment.hashCode()
        return result
    }

    override fun toString(): String = "MemArg(offset=$offset, alignment=$alignment)"

    companion object {
        internal val ONE = MemArg(0, 1)
        internal val TWO = MemArg(0, 2)
        internal val FOUR = MemArg(0, 4)
        internal val EIGHT = MemArg(0, 8)
    }
}
