package com.klyx.lsp.types

import com.klyx.lsp.internal.isExactMatch
import com.klyx.lsp.internal.tryDeserialize
import com.klyx.lsp.internal.verify
import com.klyx.lsp.types.OneOfThree.First
import com.klyx.lsp.types.OneOfThree.Second
import com.klyx.lsp.types.OneOfThree.Third
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
 * Converts this value to a [OneOfThree.First] instance.
 *
 * Example:
 * ```kotlin
 * val result: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * ```
 *
 * @receiver The value to wrap as the first variant
 * @return A [OneOfThree.First] containing this value
 */
fun <A> A.asFirst() = First(this)

/**
 * Converts this value to a [OneOfThree.Second] instance.
 *
 * Example:
 * ```kotlin
 * val result: OneOfThree<String, Int, Boolean> = 42.asSecond()
 * ```
 *
 * @receiver The value to wrap as the second variant
 * @return A [OneOfThree.Second] containing this value
 */
fun <B> B.asSecond() = Second(this)

/**
 * Converts this value to a [OneOfThree.Third] instance.
 *
 * Example:
 * ```kotlin
 * val result: OneOfThree<String, Int, Boolean> = true.asThird()
 * ```
 *
 * @receiver The value to wrap as the third variant
 * @return A [OneOfThree.Third] containing this value
 */
fun <C> C.asThird() = Third(this)

/**
 * Checks if this [OneOfThree] is a [OneOfThree.First] instance.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * if (value.isFirst()) {
 *     // Smart cast to First, can access value.value directly
 *     println(value.value) // prints "hello"
 * }
 * ```
 *
 * @receiver The [OneOfThree] instance to check
 * @return `true` if this is a [OneOfThree.First], `false` otherwise
 */
fun <A, B, C> OneOfThree<A, B, C>.isFirst(): Boolean {
    contract {
        returns(true) implies (this@isFirst is First)
    }
    return this is First
}

/**
 * Checks if this [OneOfThree] is a [OneOfThree.Second] instance.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<String, Int, Boolean> = 42.asSecond()
 * if (value.isSecond()) {
 *     // Smart cast to Second, can access value.value directly
 *     println(value.value) // prints 42
 * }
 * ```
 *
 * @receiver The [OneOfThree] instance to check
 * @return `true` if this is a [OneOfThree.Second], `false` otherwise
 */
fun <A, B, C> OneOfThree<A, B, C>.isSecond(): Boolean {
    contract {
        returns(true) implies (this@isSecond is Second)
    }
    return this is Second
}

/**
 * Checks if this [OneOfThree] is a [OneOfThree.Third] instance.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<String, Int, Boolean> = true.asThird()
 * if (value.isThird()) {
 *     // Smart cast to Third, can access value.value directly
 *     println(value.value) // prints true
 * }
 * ```
 *
 * @receiver The [OneOfThree] instance to check
 * @return `true` if this is a [OneOfThree.Third], `false` otherwise
 */
fun <A, B, C> OneOfThree<A, B, C>.isThird(): Boolean {
    contract {
        returns(true) implies (this@isThird is Third)
    }
    return this is Third
}

/**
 * Represents a value that can be one of three types: [A], [B], or [C].
 *
 * This is a discriminated union type (also known as sum type or tagged union) that can hold
 * either a value of type [A] (as [First]), type [B] (as [Second]), or type [C] (as [Third]).
 *
 * Example usage:
 * ```kotlin
 * // Creating instances
 * val stringValue: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * val intValue: OneOfThree<String, Int, Boolean> = 42.asSecond()
 * val boolValue: OneOfThree<String, Int, Boolean> = true.asThird()
 *
 * // Pattern matching
 * when (val value = stringValue) {
 *     is OneOfThree.First -> println("String: ${value.value}")
 *     is OneOfThree.Second -> println("Int: ${value.value}")
 *     is OneOfThree.Third -> println("Boolean: ${value.value}")
 * }
 *
 * // Using type guards
 * if (stringValue.isFirst()) {
 *     println(stringValue.value) // Smart cast to First
 * }
 * ```
 *
 * @param A The type of the first variant
 * @param B The type of the second variant
 * @param C The type of the third variant
 *
 * @see First The first variant of the union
 * @see Second The second variant of the union
 * @see Third The third variant of the union
 */
@Serializable(OneOfThreeSerializer::class)
sealed interface OneOfThree<out A, out B, out C> {

    /**
     * Unsafely accesses the first value.
     *
     * **Warning:** This property throws a [ClassCastException] if this [OneOfThree] is actually
     * a [Second] or [Third]. Use [isFirst] to check before accessing, or use pattern matching
     * with `when` expression.
     *
     * Example:
     * ```kotlin
     * val value: OneOfThree<String, Int, Boolean> = "hello".asFirst()
     * if (value.isFirst()) {
     *     println(value.first) // Safe: prints "hello"
     * }
     *
     * val number: OneOfThree<String, Int, Boolean> = 42.asSecond()
     * println(number.first) // Unsafe: throws ClassCastException!
     * ```
     *
     * @throws ClassCastException if this is a [Second] or [Third] instance
     */
    val first get() = (this as First).value

    /**
     * Unsafely accesses the second value.
     *
     * **Warning:** This property throws a [ClassCastException] if this [OneOfThree] is actually
     * a [First] or [Third]. Use [isSecond] to check before accessing, or use pattern matching
     * with `when` expression.
     *
     * Example:
     * ```kotlin
     * val value: OneOfThree<String, Int, Boolean> = 42.asSecond()
     * if (value.isSecond()) {
     *     println(value.second) // Safe: prints 42
     * }
     *
     * val text: OneOfThree<String, Int, Boolean> = "hello".asFirst()
     * println(text.second) // Unsafe: throws ClassCastException!
     * ```
     *
     * @throws ClassCastException if this is a [First] or [Third] instance
     */
    val second get() = (this as Second).value

    /**
     * Unsafely accesses the third value.
     *
     * **Warning:** This property throws a [ClassCastException] if this [OneOfThree] is actually
     * a [First] or [Second]. Use [isThird] to check before accessing, or use pattern matching
     * with `when` expression.
     *
     * Example:
     * ```kotlin
     * val value: OneOfThree<String, Int, Boolean> = true.asThird()
     * if (value.isThird()) {
     *     println(value.third) // Safe: prints true
     * }
     *
     * val text: OneOfThree<String, Int, Boolean> = "hello".asFirst()
     * println(text.third) // Unsafe: throws ClassCastException!
     * ```
     *
     * @throws ClassCastException if this is a [First] or [Second] instance
     */
    val third get() = (this as Third).value

    /**
     * The first variant of [OneOfThree], containing a value of type [A].
     *
     * Example:
     * ```kotlin
     * val first: OneOfThree<String, Int, Boolean> = OneOfThree.First("hello")
     * // or using extension function
     * val first2: OneOfThree<String, Int, Boolean> = "hello".asFirst()
     * ```
     *
     * @param A The type of the contained value
     * @property value The wrapped value
     */
    @JvmInline
    @Serializable
    value class First<A>(val value: A) : OneOfThree<A, Nothing, Nothing> {
        /**
         * Returns a string representation of the contained value.
         *
         * If the value is already a String, returns it directly.
         * Otherwise, calls [toString] on the value.
         */
        override fun toString() = if (value is String) value else value.toString()
    }

    /**
     * The second variant of [OneOfThree], containing a value of type [B].
     *
     * Example:
     * ```kotlin
     * val second: OneOfThree<String, Int, Boolean> = OneOfThree.Second(42)
     * // or using extension function
     * val second2: OneOfThree<String, Int, Boolean> = 42.asSecond()
     * ```
     *
     * @param B The type of the contained value
     * @property value The wrapped value
     */
    @JvmInline
    @Serializable
    value class Second<B>(val value: B) : OneOfThree<Nothing, B, Nothing> {
        /**
         * Returns a string representation of the contained value.
         *
         * If the value is already a String, returns it directly.
         * Otherwise, calls [toString] on the value.
         */
        override fun toString() = if (value is String) value else value.toString()
    }

    /**
     * The third variant of [OneOfThree], containing a value of type [C].
     *
     * Example:
     * ```kotlin
     * val third: OneOfThree<String, Int, Boolean> = OneOfThree.Third(true)
     * // or using extension function
     * val third2: OneOfThree<String, Int, Boolean> = true.asThird()
     * ```
     *
     * @param C The type of the contained value
     * @property value The wrapped value
     */
    @JvmInline
    @Serializable
    value class Third<C>(val value: C) : OneOfThree<Nothing, Nothing, C> {
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
 * Maps the first value of this [OneOfThree] using the provided transform function.
 *
 * If this is a [OneOfThree.First], applies [transform] to its value and returns a new [OneOfThree.First].
 * If this is a [OneOfThree.Second] or [OneOfThree.Third], returns it unchanged.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<Int, String, Boolean> = 5.asFirst()
 * val doubled: OneOfThree<Int, String, Boolean> = value.mapFirst { it * 2 }
 * // doubled is First(10)
 * ```
 *
 * @param transform The function to apply to the first value
 * @return A new [OneOfThree] with the transformed first value, or the original second/third value
 */
fun <A, B, C, D> OneOfThree<A, B, C>.mapFirst(transform: (A) -> D): OneOfThree<D, B, C> =
    when (this) {
        is First -> transform(value).asFirst()
        is Second -> this
        is Third -> this
    }

/**
 * Maps the second value of this [OneOfThree] using the provided transform function.
 *
 * If this is a [OneOfThree.Second], applies [transform] to its value and returns a new [OneOfThree.Second].
 * If this is a [OneOfThree.First] or [OneOfThree.Third], returns it unchanged.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<String, Int, Boolean> = 5.asSecond()
 * val doubled: OneOfThree<String, Int, Boolean> = value.mapSecond { it * 2 }
 * // doubled is Second(10)
 * ```
 *
 * @param transform The function to apply to the second value
 * @return A new [OneOfThree] with the transformed second value, or the original first/third value
 */
fun <A, B, C, D> OneOfThree<A, B, C>.mapSecond(transform: (B) -> D): OneOfThree<A, D, C> =
    when (this) {
        is First -> this
        is Second -> transform(value).asSecond()
        is Third -> this
    }

/**
 * Maps the third value of this [OneOfThree] using the provided transform function.
 *
 * If this is a [OneOfThree.Third], applies [transform] to its value and returns a new [OneOfThree.Third].
 * If this is a [OneOfThree.First] or [OneOfThree.Second], returns it unchanged.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<String, Int, Boolean> = true.asThird()
 * val negated: OneOfThree<String, Int, Boolean> = value.mapThird { !it }
 * // negated is Third(false)
 * ```
 *
 * @param transform The function to apply to the third value
 * @return A new [OneOfThree] with the transformed third value, or the original first/second value
 */
fun <A, B, C, D> OneOfThree<A, B, C>.mapThird(transform: (C) -> D): OneOfThree<A, B, D> =
    when (this) {
        is First -> this
        is Second -> this
        is Third -> transform(value).asThird()
    }

/**
 * Folds this [OneOfThree] into a single value by applying one of three functions.
 *
 * If this is a [OneOfThree.First], applies [firstFn] to its value.
 * If this is a [OneOfThree.Second], applies [secondFn] to its value.
 * If this is a [OneOfThree.Third], applies [thirdFn] to its value.
 *
 * Example:
 * ```kotlin
 * val value: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * val result: String = value.fold(
 *     firstFn = { "String: $it" },
 *     secondFn = { "Int: $it" },
 *     thirdFn = { "Boolean: $it" }
 * )
 * // result is "String: hello"
 * ```
 *
 * @param firstFn The function to apply to a first value
 * @param secondFn The function to apply to a second value
 * @param thirdFn The function to apply to a third value
 * @return The result of applying the appropriate function
 */
fun <A, B, C, D> OneOfThree<A, B, C>.fold(
    firstFn: (A) -> D,
    secondFn: (B) -> D,
    thirdFn: (C) -> D
): D =
    when (this) {
        is First -> firstFn(value)
        is Second -> secondFn(value)
        is Third -> thirdFn(value)
    }

/**
 * Returns the first value if present, otherwise returns the provided default value.
 *
 * Example:
 * ```kotlin
 * val first: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * val second: OneOfThree<String, Int, Boolean> = 42.asSecond()
 *
 * println(first.firstOr("default"))   // prints "hello"
 * println(second.firstOr("default"))  // prints "default"
 * ```
 *
 * @param default The value to return if this is a [OneOfThree.Second] or [OneOfThree.Third]
 * @return The first value or the default
 */
fun <A, B, C> OneOfThree<A, B, C>.firstOr(default: A): A =
    when (this) {
        is First -> value
        is Second -> default
        is Third -> default
    }

/**
 * Returns the second value if present, otherwise returns the provided default value.
 *
 * Example:
 * ```kotlin
 * val first: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * val second: OneOfThree<String, Int, Boolean> = 42.asSecond()
 *
 * println(first.secondOr(0))   // prints 0
 * println(second.secondOr(0))  // prints 42
 * ```
 *
 * @param default The value to return if this is a [OneOfThree.First] or [OneOfThree.Third]
 * @return The second value or the default
 */
fun <A, B, C> OneOfThree<A, B, C>.secondOr(default: B): B =
    when (this) {
        is First -> default
        is Second -> value
        is Third -> default
    }

/**
 * Returns the third value if present, otherwise returns the provided default value.
 *
 * Example:
 * ```kotlin
 * val first: OneOfThree<String, Int, Boolean> = "hello".asFirst()
 * val third: OneOfThree<String, Int, Boolean> = true.asThird()
 *
 * println(first.thirdOr(false))   // prints false
 * println(third.thirdOr(false))   // prints true
 * ```
 *
 * @param default The value to return if this is a [OneOfThree.First] or [OneOfThree.Second]
 * @return The third value or the default
 */
fun <A, B, C> OneOfThree<A, B, C>.thirdOr(default: C): C =
    when (this) {
        is First -> default
        is Second -> default
        is Third -> value
    }

internal class OneOfThreeSerializer<A, B, C>(
    private val firstSerializer: KSerializer<A>,
    private val secondSerializer: KSerializer<B>,
    private val thirdSerializer: KSerializer<C>
) : KSerializer<OneOfThree<A, B, C>> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("com.klyx.lsp.types.OneOfThree", PolymorphicKind.SEALED) {
            element("first", firstSerializer.descriptor)
            element("second", secondSerializer.descriptor)
            element("third", thirdSerializer.descriptor)
        }

    override fun serialize(encoder: Encoder, value: OneOfThree<A, B, C>) {
        verify(encoder)
        when (value) {
            is First -> firstSerializer.serialize(encoder, value.value)
            is Second -> secondSerializer.serialize(encoder, value.value)
            is Third -> thirdSerializer.serialize(encoder, value.value)
        }
    }

    override fun deserialize(decoder: Decoder): OneOfThree<A, B, C> {
        val decoder = verify(decoder)
        val element = decoder.decodeJsonElement()

        val firstResult = tryDeserialize(decoder.json, firstSerializer, element)
        val secondResult = tryDeserialize(decoder.json, secondSerializer, element)
        val thirdResult = tryDeserialize(decoder.json, thirdSerializer, element)

        // Count how many succeeded
        val successCount = listOfNotNull(firstResult, secondResult, thirdResult).size

        return when (successCount) {
            0 -> throw SerializationException("Could not deserialize OneOfThree: element does not match any of the three types")

            1 -> {
                when {
                    firstResult != null -> First(firstResult)
                    secondResult != null -> Second(secondResult)
                    thirdResult != null -> Third(thirdResult)
                    else -> error("Unreachable: successCount is 1 but no result found")
                }
            }

            else -> {
                when {
                    firstResult != null && isExactMatch(element, firstSerializer.descriptor) ->
                        First(firstResult)

                    secondResult != null && isExactMatch(element, secondSerializer.descriptor) ->
                        Second(secondResult)

                    thirdResult != null && isExactMatch(element, thirdSerializer.descriptor) ->
                        Third(thirdResult)
                    // If no exact match, prefer in order: First > Second > Third
                    firstResult != null -> First(firstResult)
                    secondResult != null -> Second(secondResult)
                    thirdResult != null -> Third(thirdResult)
                    else -> error("Unreachable: successCount > 1 but no result found")
                }
            }
        }
    }
}
