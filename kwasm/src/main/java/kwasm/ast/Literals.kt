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

/** Base for all literal values in the AST. */
abstract class Literal<T> : AstNode {
    abstract val value: T

    override fun equals(other: Any?): Boolean = other is Literal<*> && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "${javaClass.simpleName}(value=$value)"
}

/** [Literal] Representation of an unsigned [Int] (as [UInt]). */
@OptIn(ExperimentalUnsignedTypes::class)
sealed class IntegerLiteral<T> : Literal<T>() {
    /** [Literal] Representation of an unsigned [Int] (as [UInt]). */
    class U32(private val orig: UInt) : IntegerLiteral<UInt>() {
        override val value: UInt by lazy {
            // This is a stupid hack around a bug with unsigned types in Kotlin. The issue is that
            // the JVM sees a java.lang.Integer, not an `int`, and there  is no
            // java.lang.Integer.toUInt() method.
            orig.toInt().toUInt()
        }
    }

    /** [Literal] Representation of a signed [Int]. */
    class S32(override val value: Int) : IntegerLiteral<Int>()

    /** [Literal] Representation of an unsigned [Long] (as [ULong]). */
    class U64(private val orig: ULong) : IntegerLiteral<ULong>() {
        override val value: ULong by lazy {
            // This is a stupid hack around a bug with unsigned types in Kotlin. The issue is that
            // the JVM sees a java.lang.Long, not a `long`, and there is no java.lang.Long.toUInt()
            // method.
            orig.toLong().toULong()
        }
    }

    /** [Literal] Representation of a signed [Long]. */
    class S64(override val value: Long) : IntegerLiteral<Long>()
}

/** Base for all [Literal] classes which represent a floating-point value. */
sealed class FloatLiteral<T> : Literal<T>() {
    /** [Literal] Representation of a [Float]. */
    data class SinglePrecision(override val value: Float) : FloatLiteral<Float>()

    /** [Literal] Representation of a [Double]. */
    data class DoublePrecision(override val value: Double) : FloatLiteral<Double>()
}

/** Representation of a [String] as a [Literal]. */
data class StringLiteral(override val value: String) : Literal<String>()
