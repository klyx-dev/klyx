@file:OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)

package com.klyx.core

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.flatten
import arrow.core.identity
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.catch
import arrow.core.raise.mapOrAccumulate
import arrow.core.right
import arrow.core.toOption
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.mapError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

typealias AnyResult<T> = Result<T, AnyErr>

class AnyException(
    override val message: String?,
    override val cause: Throwable? = null
) : RuntimeException()

class AnyErr private constructor(
    val causeError: Throwable,
    private val ctx: List<String>
) : Throwable(causeError) {

    fun context(msg: String): AnyErr = AnyErr(causeError, ctx + msg)
    fun render(): String = (ctx + causeError.message).joinToString("\nCaused by: ")

    override fun toString(): String = render()

    companion object {

        fun from(err: AnyErr) = err
        fun from(e: Throwable) = e as? AnyErr ?: AnyErr(e, emptyList())
        fun msg(s: String) = AnyErr(AnyException(s), emptyList())

        fun <E> fromAny(error: E): AnyErr =
            when (error) {
                is AnyErr -> error
                is Throwable -> from(error)
                is String -> msg(error)
                else -> msg(error.toString())
            }
    }
}

@DslMarker
private annotation class AnyHowDsl

@Suppress("ClassName")
@AnyHowDsl
object anyhow {
    fun <A> ok(value: A) = anyResult { value }
    fun err(error: AnyErr) = anyResult { raise(error) }
    fun err(error: Throwable) = anyResult { raise(error) }
    fun err(error: String) = anyResult { raise(error) }
    fun <Error> err(error: Error) = anyResult { raise(error) }
}

@Suppress("FunctionName")
fun <Error> Err(error: Error) = anyhow.err(error)

@Suppress("FunctionName")
fun <V> Ok(value: V) = anyResult { value }

@PublishedApi
@Suppress("FunctionName")
internal fun <Error> AnyErr(error: Error) = Err(AnyErr.fromAny(error))

fun <A> kotlin.Result<A>.toAnyResult(): AnyResult<A> = fold(::Ok, ::AnyErr)
fun <A> Either<AnyErr, A>.toAnyResult(): AnyResult<A> = fold(::Err, ::Ok)
fun <A> AnyResult<A>.toEither(): Either<AnyErr, A> = fold(failure = { it.left() }, success = { it.right() })
fun <A> AnyResult<A>.toKotlinResult() = fold(kotlin.Result.Companion::success, kotlin.Result.Companion::failure)

class AnyResultRaise(private val raise: Raise<AnyErr>) : Raise<AnyErr> by raise {

    @RaiseDSL
    fun <A> AnyResult<A>.bind(): A = fold(::identity) { raise(it) }

    @RaiseDSL
    fun <A> A?.bind(): A {
        contract {
            returns() implies (this@bind != null)
            returnsNotNull() implies (this@bind != null)
        }
        return this ?: raise(AnyErr.msg("null value"))
    }

    @RaiseDSL
    fun <A, Error> A?.bind(error: () -> Error): A {
        contract {
            callsInPlace(error, InvocationKind.AT_MOST_ONCE)
            returns() implies (this@bind != null)
            returnsNotNull() implies (this@bind != null)
        }
        return this ?: raise(error())
    }

    @RaiseDSL
    @JvmName("bindResult")
    fun <A, E> Result<A, E>.bind(): A = fold(::identity) { raise(it) }

    @JvmName("bindAllResult")
    fun <K, V> Map<K, AnyResult<V>>.bindAll(): Map<K, V> = mapValues { (_, v) -> v.bind() }

    @RaiseDSL
    @JvmName("bindAllResult")
    fun <A> Iterable<AnyResult<A>>.bindAll(): List<A> = map { it.bind() }

    @RaiseDSL
    @JvmName("bindAllResult")
    fun <A> NonEmptyList<AnyResult<A>>.bindAll(): NonEmptyList<A> = map { it.bind() }

    @RaiseDSL
    inline fun <A> recover(
        block: AnyResultRaise.() -> A,
        recover: (AnyErr) -> A,
    ): A {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
            callsInPlace(recover, InvocationKind.AT_MOST_ONCE)
        }
        return anyResult(block).fold(
            success = { it },
            failure = { recover(it) },
        )
    }

    /**
     * Raises a _logical failure_ of type [AnyErr].
     * This function behaves like a _return statement_,
     * immediately short-circuiting and terminating the computation.
     *
     * __Alternatives:__ Common ways to raise errors include: [ensure], [ensureNotNull], and [bind].
     * Consider using them to make your code more concise and expressive.
     *
     * __Handling raised errors:__ Refer to [recover] and [mapOrAccumulate].
     *
     * @param error an error of type [String] that will short-circuit the computation.
     * Behaves similarly to _return_ or _throw_.
     */
    @RaiseDSL
    fun raise(error: String): Nothing = raise(AnyErr.msg(error))

    /**
     * Raises a _logical failure_ of type [AnyErr].
     * This function behaves like a _return statement_,
     * immediately short-circuiting and terminating the computation.
     *
     * __Alternatives:__ Common ways to raise errors include: [ensure], [ensureNotNull], and [bind].
     * Consider using them to make your code more concise and expressive.
     *
     * __Handling raised errors:__ Refer to [recover] and [mapOrAccumulate].
     *
     * @param error an error of type [Throwable] that will short-circuit the computation.
     * Behaves similarly to _return_ or _throw_.
     */
    @RaiseDSL
    fun raise(error: Throwable): Nothing = raise(AnyErr.from(error))

    /**
     * Raises a _logical failure_ of type [AnyErr].
     * This function behaves like a _return statement_,
     * immediately short-circuiting and terminating the computation.
     *
     * __Alternatives:__ Common ways to raise errors include: [ensure], [ensureNotNull], and [bind].
     * Consider using them to make your code more concise and expressive.
     *
     * __Handling raised errors:__ Refer to [recover] and [mapOrAccumulate].
     *
     * @param error an error of type [Error] that will short-circuit the computation.
     * Behaves similarly to _return_ or _throw_.
     */
    @RaiseDSL
    @JvmName("raiseAnyErr")
    fun <Error> raise(error: Error): Nothing = raise(AnyErr.fromAny(error))

    /**
     * Ensures that the [condition] is met;
     * otherwise, [Raise.raise]s a logical failure of type [AnyErr].
     *
     * In summary, this is a type-safe alternative to [require], using the [Raise] API.
     */
    @RaiseDSL
    inline fun <Error> ensure(
        condition: Boolean,
        error: () -> Error
    ) {
        contract {
            callsInPlace(error, InvocationKind.AT_MOST_ONCE)
            returns() implies condition
        }
        if (!condition) raise(error())
    }

    /**
     * Ensures that the [value] is not null;
     * otherwise, [Raise.raise]s a logical failure of type [AnyErr].
     *
     * In summary, this is a type-safe alternative to [requireNotNull], using the [Raise] API.
     */
    @RaiseDSL
    inline fun <T : Any, Error> ensureNotNull(
        value: T?,
        error: () -> Error
    ): T {
        contract {
            callsInPlace(error, InvocationKind.AT_MOST_ONCE)
            returns() implies (value != null)
            returnsNotNull() implies (value != null)
        }
        return value ?: raise(error())
    }

    inline fun <T> withContext(
        msg: String,
        block: AnyResultRaise.() -> T
    ): T {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return anyhow(block).fold(
            success = { it },
            failure = { error -> raise(error.context(msg)) }
        )
    }

    /**
     * Starts a new nested [AnyResult] context.
     * Useful for grouping operations where failures should be caught or transformed differently
     * within a larger [AnyResult] block.
     */
    inline fun <A> anyhow(block: AnyResultRaise.() -> A): AnyResult<A> {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return anyResult(block)
    }

    /**
     * Maps the success value using [transform].
     * If the result is an error, it passes through unchanged.
     */
    @RaiseDSL
    inline fun <A, B> map(block: AnyResultRaise.() -> A, transform: (A) -> B): B {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
            callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
        }
        return transform(block())
    }

    /**
     * Executes [block] and if it succeeds, executes [next] with the result.
     * Short-circuits on any error.
     */
    @RaiseDSL
    inline fun <A, B> andThen(
        block: AnyResultRaise.() -> A,
        next: AnyResultRaise.(A) -> B
    ): B {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
            callsInPlace(next, InvocationKind.AT_MOST_ONCE)
        }
        val value = block()
        return next(value)
    }

    /**
     * Bind an Option, raising an error if None.
     */
    @RaiseDSL
    fun <A> Option<A>.bind(message: () -> String): A {
        contract {
            callsInPlace(message, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            is Some -> value
            is None -> bail(message())
        }
    }

    /**
     * Accumulates errors from multiple operations instead of short-circuiting.
     * Returns all results if all succeed, or all errors if any fail.
     */
    @RaiseDSL
    inline fun <A> accumulating(
        block: AnyResultRaise.() -> Iterable<AnyResult<A>>
    ): List<A> {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        val results = block()
        val errors = mutableListOf<AnyErr>()
        val successes = mutableListOf<A>()

        for (result in results) {
            result.fold(
                success = { successes.add(it) },
                failure = { errors.add(it) }
            )
        }

        if (errors.isNotEmpty()) {
            val combined = errors.joinToString("\n") { it.render() }
            bail("Multiple errors occurred:\n$combined")
        }

        return successes
    }
}

inline fun <A> anyResult(block: AnyResultRaise.() -> A): AnyResult<A> {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    return arrow.core.raise.fold({ block(AnyResultRaise(this)) }, ::AnyErr, ::Err, ::Ok)
}

inline fun <A> anyhow(block: AnyResultRaise.() -> A) = anyResult(block)

@RaiseDSL
fun <E> AnyResultRaise.anyhow(error: E): Nothing = raise(error)
fun <E> anyhow(error: E) = anyResult { raise(error) }

/**
 * Raises a _logical failure_ with the given string message.
 * This function behaves like a _return statement_, immediately short-circuiting and terminating the computation.
 *
 * This is a convenience alias for `raise(message)`.
 *
 * @param message The error message string.
 * @return [Nothing] This function never returns; it always raises an error.
 */
@RaiseDSL
fun AnyResultRaise.bail(message: String): Nothing = raise(message)

@RaiseDSL
fun AnyResultRaise.bail(t: Throwable): Nothing = raise(AnyErr.from(t))

@RaiseDSL
fun AnyResultRaise.bail(err: AnyErr): Nothing = raise(err)

@RaiseDSL
inline fun <T> AnyResultRaise.catching(block: () -> T): T = catch(block) { raise(it) }

fun <T> AnyResult<T>.context(msg: String): AnyResult<T> = mapError { it.context(msg) }

fun <T> AnyResult<T>.ok() = fold(::Some) { None }

@JvmName("flattenOption")
fun <T> AnyResult<Option<T>>.flatten() = fold(::Some) { None }.flatten()

fun <T> AnyResult<T?>.flatten(): Option<T> = fold({ it.toOption() }) { None }

/**
 * Maps the success value to a new type.
 */
inline fun <T, R> AnyResult<T>.map(transform: (T) -> R): AnyResult<R> =
    fold(success = { Ok(transform(it)) }, failure = { Err(it) })

/**
 * FlatMaps the success value to a new AnyResult.
 */
inline fun <T, R> AnyResult<T>.flatMap(transform: (T) -> AnyResult<R>): AnyResult<R> =
    fold(success = transform, failure = { Err(it) })

/**
 * Alias for flatMap.
 */
inline fun <T, R> AnyResult<T>.andThen(transform: (T) -> AnyResult<R>): AnyResult<R> = flatMap(transform)

/**
 * Returns the success value or null if error.
 */
fun <T> AnyResult<T>.getOrNull(): T? = fold(success = { it }, failure = { null })

/**
 * Returns this result if success, otherwise returns [fallback].
 */
inline fun <T> AnyResult<T>.orElse(fallback: () -> AnyResult<T>): AnyResult<T> =
    fold(success = { this }, failure = { fallback() })

/**
 * Unwraps the result, throwing an exception if error.
 */
fun <T> AnyResult<T>.unwrap(): T = fold(
    success = { it },
    failure = { throw it }
)

/**
 * Unwraps the result, throwing an exception with custom message if error.
 */
fun <T> AnyResult<T>.expect(message: String): T = fold(
    success = { it },
    failure = { throw AnyException(message, it) }
)

/**
 * Converts AnyResult to Option, losing error information.
 */
fun <T> AnyResult<T>.toOption(): Option<T> = fold(success = { Some(it) }, failure = { None })

/**
 * Transposes AnyResult<Option<T>> to Option<AnyResult<T>>.
 */
fun <T> AnyResult<Option<T>>.transpose(): Option<AnyResult<T>> = fold(
    success = { option ->
        when (option) {
            is Some -> Some(Ok(option.value))
            is None -> None
        }
    },
    failure = { Some(Err(it)) }
)

/**
 * Zips two results together, short-circuiting on first error.
 */
fun <A, B> AnyResult<A>.zip(other: AnyResult<B>): AnyResult<Pair<A, B>> =
    flatMap { a -> other.map { b -> a to b } }

/**
 * Zips two results together with a combining function.
 */
inline fun <A, B, R> AnyResult<A>.zip(
    other: AnyResult<B>,
    transform: (A, B) -> R
): AnyResult<R> = flatMap { a -> other.map { b -> transform(a, b) } }

/**
 * Flattens a nested AnyResult.
 */
fun <T> AnyResult<AnyResult<T>>.flatten(): AnyResult<T> = flatMap { it }

/**
 * Converts a collection of AnyResults to AnyResult of collection.
 * Short-circuits on first error.
 */
fun <T> Iterable<AnyResult<T>>.sequence(): AnyResult<List<T>> = anyResult {
    map { it.bind() }
}

/**
 * Maps and sequences in one operation.
 */
inline fun <T, R> Iterable<T>.traverse(transform: (T) -> AnyResult<R>): AnyResult<List<R>> =
    map(transform).sequence()

/**
 * Filters successes, collecting only Ok values.
 */
fun <T> Iterable<AnyResult<T>>.filterSuccess(): List<T> =
    mapNotNull { it.getOrNull() }

/**
 * Partitions results into successes and failures.
 */
fun <T> Iterable<AnyResult<T>>.partition(): Pair<List<T>, List<AnyErr>> {
    val successes = mutableListOf<T>()
    val failures = mutableListOf<AnyErr>()
    forEach { result ->
        result.fold(
            success = { successes.add(it) },
            failure = { failures.add(it) }
        )
    }
    return successes to failures
}

/**
 * Convert nullable to AnyResult.
 */
fun <T : Any> T?.toAnyResult(error: () -> String): AnyResult<T> =
    this?.let { Ok(it) } ?: anyhow(error())

/**
 * Runs a block that returns AnyResult, catching any exceptions as AnyErr.
 */
inline fun <T> runCatchingAny(block: () -> T): AnyResult<T> = anyResult {
    catching(block)
}
