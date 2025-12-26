package com.klyx.lsp.types

import com.klyx.lsp.internal.isExactMatch
import com.klyx.lsp.internal.tryDeserialize
import com.klyx.lsp.internal.verify
import com.klyx.lsp.types.OneOf.Left
import com.klyx.lsp.types.OneOf.Right
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

/**
 * Converts this value to a [OneOf.Left] instance.
 *
 * Example:
 * ```kotlin
 * val result: OneOf<String, Int> = "hello".asLeft()
 * ```
 *
 * @receiver The value to wrap as the left variant
 * @return A [OneOf.Left] containing this value
 */
fun <A> A.asLeft() = Left(this)

/**
 * Converts this value to a [OneOf.Right] instance.
 *
 * Example:
 * ```kotlin
 * val result: OneOf<String, Int> = 42.asRight()
 * ```
 *
 * @receiver The value to wrap as the right variant
 * @return A [OneOf.Right] containing this value
 */
fun <A> A.asRight() = Right(this)

/**
 * Checks if this [OneOf] is a [OneOf.Left] instance.
 *
 * Example:
 * ```kotlin
 * val value: OneOf<String, Int> = "hello".asLeft()
 * if (value.isLeft()) {
 *     // Smart cast to Left, can access value.value directly
 *     println(value.value) // prints "hello"
 * }
 * ```
 *
 * @receiver The [OneOf] instance to check
 * @return `true` if this is a [OneOf.Left], `false` otherwise
 */
fun <A, B> OneOf<A, B>.isLeft(): Boolean {
    contract {
        returns(true) implies (this@isLeft is Left)
        returns(false) implies (this@isLeft is Right)
    }
    return this is Left
}

/**
 * Checks if this [OneOf] is a [OneOf.Right] instance.
 *
 * Example:
 * ```kotlin
 * val value: OneOf<String, Int> = 42.asRight()
 * if (value.isRight()) {
 *     // Smart cast to Right, can access value.value directly
 *     println(value.value) // prints 42
 * }
 * ```
 *
 * @receiver The [OneOf] instance to check
 * @return `true` if this is a [OneOf.Right], `false` otherwise
 */
fun <A, B> OneOf<A, B>.isRight(): Boolean {
    contract {
        returns(true) implies (this@isRight is Right)
        returns(false) implies (this@isRight is Left)
    }
    return this is Right
}

fun <A, B> OneOf<A, B>.value() = if (isLeft()) left else right

/**
 * Represents a value that can be one of two types: [A] or [B].
 *
 * This is a discriminated union type (also known as sum type or tagged union) that can hold
 * either a value of type [A] (as [Left]) or a value of type [B] (as [Right]).
 *
 * Example usage:
 * ```kotlin
 * // Creating instances
 * val stringValue: OneOf<String, Int> = "hello".asLeft()
 * val intValue: OneOf<String, Int> = 42.asRight()
 *
 * // Pattern matching
 * when (val value = stringValue) {
 *     is OneOf.Left -> println("String: ${value.value}")
 *     is OneOf.Right -> println("Int: ${value.value}")
 * }
 *
 * // Using type guards
 * if (stringValue.isLeft()) {
 *     println(stringValue.value) // Smart cast to Left
 * }
 * ```
 *
 * @param A The type of the left variant
 * @param B The type of the right variant
 *
 * @see Left The left variant of the union
 * @see Right The right variant of the union
 */
@Serializable(OneOfSerializer::class)
sealed interface OneOf<out A, out B> {

    /**
     * Unsafely accesses the left value.
     *
     * **Warning:** This property throws a [ClassCastException] if this [OneOf] is actually a [Right].
     * Use [isLeft] to check before accessing, or use pattern matching with `when` expression.
     *
     * Example:
     * ```kotlin
     * val value: OneOf<String, Int> = "hello".asLeft()
     * if (value.isLeft()) {
     *     println(value.left) // Safe: prints "hello"
     * }
     *
     * val number: OneOf<String, Int> = 42.asRight()
     * println(number.left) // Unsafe: throws ClassCastException!
     * ```
     *
     * @throws ClassCastException if this is a [Right] instance
     */
    val left get() = (this as Left<A>).value

    /**
     * Unsafely accesses the right value.
     *
     * **Warning:** This property throws a [ClassCastException] if this [OneOf] is actually a [Left].
     * Use [isRight] to check before accessing, or use pattern matching with `when` expression.
     *
     * Example:
     * ```kotlin
     * val value: OneOf<String, Int> = 42.asRight()
     * if (value.isRight()) {
     *     println(value.right) // Safe: prints 42
     * }
     *
     * val text: OneOf<String, Int> = "hello".asLeft()
     * println(text.right) // Unsafe: throws ClassCastException!
     * ```
     *
     * @throws ClassCastException if this is a [Left] instance
     */
    val right get() = (this as Right<B>).value

    /**
     * The left variant of [OneOf], containing a value of type [A].
     *
     * Example:
     * ```kotlin
     * val left: OneOf<String, Int> = OneOf.Left("hello")
     * // or using extension function
     * val left2: OneOf<String, Int> = "hello".asLeft()
     * ```
     *
     * @param A The type of the contained value
     * @property value The wrapped value
     */
    @JvmInline
    @Serializable
    value class Left<A>(val value: A) : OneOf<A, Nothing> {
        /**
         * Returns a string representation of the contained value.
         *
         * If the value is already a String, returns it directly.
         * Otherwise, calls [toString] on the value.
         */
        override fun toString() = if (value is String) value else value.toString()
    }

    /**
     * The right variant of [OneOf], containing a value of type [B].
     *
     * Example:
     * ```kotlin
     * val right: OneOf<String, Int> = OneOf.Right(42)
     * // or using extension function
     * val right2: OneOf<String, Int> = 42.asRight()
     * ```
     *
     * @param B The type of the contained value
     * @property value The wrapped value
     */
    @JvmInline
    @Serializable
    value class Right<B>(val value: B) : OneOf<Nothing, B> {
        /**
         * Returns a string representation of the contained value.
         *
         * If the value is already a String, returns it directly.
         * Otherwise, calls [toString] on the value.
         */
        override fun toString() = if (value is String) value else value.toString()
    }
}

/**
 * Maps the left value of this [OneOf] using the provided transform function.
 *
 * If this is a [OneOf.Left], applies [transform] to its value and returns a new [OneOf.Left].
 * If this is a [OneOf.Right], returns it unchanged.
 *
 * Example:
 * ```kotlin
 * val value: OneOf<Int, String> = 5.asLeft()
 * val doubled: OneOf<Int, String> = value.mapLeft { it * 2 }
 * // doubled is Left(10)
 * ```
 *
 * @param transform The function to apply to the left value
 * @return A new [OneOf] with the transformed left value, or the original right value
 */
fun <A, B, C> OneOf<A, B>.mapLeft(transform: (A) -> C): OneOf<C, B> =
    when (this) {
        is Left -> transform(value).asLeft()
        is Right -> this
    }

/**
 * Maps the right value of this [OneOf] using the provided transform function.
 *
 * If this is a [OneOf.Right], applies [transform] to its value and returns a new [OneOf.Right].
 * If this is a [OneOf.Left], returns it unchanged.
 *
 * Example:
 * ```kotlin
 * val value: OneOf<String, Int> = 5.asRight()
 * val doubled: OneOf<String, Int> = value.mapRight { it * 2 }
 * // doubled is Right(10)
 * ```
 *
 * @param transform The function to apply to the right value
 * @return A new [OneOf] with the transformed right value, or the original left value
 */
fun <A, B, C> OneOf<A, B>.mapRight(transform: (B) -> C): OneOf<A, C> =
    when (this) {
        is Left -> this
        is Right -> transform(value).asRight()
    }

/**
 * Folds this [OneOf] into a single value by applying one of two functions.
 *
 * If this is a [OneOf.Left], applies [leftFn] to its value.
 * If this is a [OneOf.Right], applies [rightFn] to its value.
 *
 * Example:
 * ```kotlin
 * val value: OneOf<String, Int> = "hello".asLeft()
 * val length: Int = value.fold(
 *     leftFn = { it.length },
 *     rightFn = { it }
 * )
 * // length is 5
 * ```
 *
 * @param leftFn The function to apply to a left value
 * @param rightFn The function to apply to a right value
 * @return The result of applying the appropriate function
 */
inline fun <A, B, C> OneOf<A, B>.fold(leftFn: (A) -> C, rightFn: (B) -> C): C =
    when (this) {
        is Left -> leftFn(value)
        is Right -> rightFn(value)
    }

/**
 * Swaps the left and right types of this [OneOf].
 *
 * Converts [OneOf.Left] to [OneOf.Right] and vice versa.
 *
 * Example:
 * ```kotlin
 * val left: OneOf<String, Int> = "hello".asLeft()
 * val swapped: OneOf<Int, String> = left.swap()
 * // swapped is Right("hello")
 * ```
 *
 * @return A new [OneOf] with swapped variants
 */
fun <A, B> OneOf<A, B>.swap(): OneOf<B, A> =
    when (this) {
        is Left -> value.asRight()
        is Right -> value.asLeft()
    }

/**
 * Returns the left value if present, otherwise returns the provided default value.
 *
 * Example:
 * ```kotlin
 * val left: OneOf<String, Int> = "hello".asLeft()
 * val right: OneOf<String, Int> = 42.asRight()
 *
 * println(left.leftOr { "default" })   // prints "hello"
 * println(right.leftOr { "default" })  // prints "default"
 * ```
 *
 * @param default The value to return if this is a [OneOf.Right]
 * @return The left value or the default
 */
fun <A, B> OneOf<A, B>.leftOr(default: () -> A): A =
    when (this) {
        is Left -> value
        is Right -> default()
    }

/**
 * Returns the right value if present, otherwise returns the provided default value.
 *
 * Example:
 * ```kotlin
 * val left: OneOf<String, Int> = "hello".asLeft()
 * val right: OneOf<String, Int> = 42.asRight()
 *
 * println(left.rightOr { 0 })   // prints 0
 * println(right.rightOr { 0 })  // prints 42
 * ```
 *
 * @param default The value to return if this is a [OneOf.Left]
 * @return The right value or the default
 */
fun <A, B> OneOf<A, B>.rightOr(default: () -> B): B =
    when (this) {
        is Left -> default()
        is Right -> value
    }

internal class OneOfSerializer<A, B>(
    private val leftSerializer: KSerializer<A>,
    private val rightSerializer: KSerializer<B>
) : KSerializer<OneOf<A, B>> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("com.klyx.lsp.types.OneOf", PolymorphicKind.SEALED) {
            element("left", leftSerializer.descriptor)
            element("right", rightSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: OneOf<A, B>) {
        verify(encoder)
        when (value) {
            is Left -> leftSerializer.serialize(encoder, value.value)
            is Right -> rightSerializer.serialize(encoder, value.value)
        }
    }

    override fun deserialize(decoder: Decoder): OneOf<A, B> {
        val decoder = verify(decoder)
        val element = decoder.decodeJsonElement()

        val leftResult = tryDeserialize(decoder.json, leftSerializer, element)
        val rightResult = tryDeserialize(decoder.json, rightSerializer, element)

        return when {
            leftResult != null && rightResult != null -> {
                if (isExactMatch(element, leftSerializer.descriptor)) {
                    Left(leftResult)
                } else {
                    Right(rightResult)
                }
            }

            leftResult != null -> Left(leftResult)
            rightResult != null -> Right(rightResult)
            else -> throw SerializationException(
                "Could not deserialize OneOf: element does not match either Left or Right type"
            )
        }
    }
}
