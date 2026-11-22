@file:OptIn(ExperimentalAtomicApi::class)

package com.klyx.core.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

val LocalActions = staticCompositionLocalOf { App.global.actions }

class ActionDispatcher(parentScope: CoroutineScope) : ActionContext {

    private val scope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
    )

    override val dispatcher get() = this

    private class Entry<A : Action>(
        val type: KClass<A>,
        val serializer: KSerializer<A>,
        val handler: ActionHandler<A>
    )

    private class State(val entries: Map<KClass<*>, Entry<*>>) {
        val json: Json by lazy {
            Json {
                serializersModule = SerializersModule {
                    polymorphic(Action::class) {
                        for (entry in entries.values) {
                            @Suppress("UNCHECKED_CAST")
                            subclass(entry.type as KClass<Action>, entry.serializer as KSerializer<Action>)
                        }
                    }
                }
            }
        }
    }

    private val state = AtomicReference(State(emptyMap()))

    fun isRegistered(type: KClass<*>) = type in state.load().entries

    fun <A : Action> register(
        type: KClass<A>,
        serializer: KSerializer<A>,
        handler: ActionHandler<A>,
    ): Registration {
        val entry = Entry(type, serializer, handler)
        state.update { s ->
            check(type !in s.entries) { "Action ${type.simpleName} is already registered" }
            State(s.entries + (type to entry))
        }
        return Registration {
            state.update { s -> if (s.entries[type] === entry) State(s.entries - type) else s }
        }
    }

    suspend fun execute(action: Action) {
        @Suppress("UNCHECKED_CAST")
        val entry = state.load().entries[action::class] as Entry<Action>?
            ?: throw ActionNotRegisteredException(action::class)

        try {
            with(entry.handler) { handle(action) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw ActionException(action, e)
        }
    }

    @IgnorableReturnValue
    fun dispatch(action: Action): Job = scope.launch { execute(action) }

    fun dispatchAsync(action: Action): Deferred<Unit> = scope.async { execute(action) }

    fun encode(action: Action): String = state.load().json.encodeToString(PolymorphicSerializer(Action::class), action)
    fun decode(raw: String): Action = state.load().json.decodeFromString(PolymorphicSerializer(Action::class), raw)

    fun dispatch(raw: String): Job = dispatch(decode(raw))
}

@PublishedApi
internal inline fun <reified A : Action> defaultAction(): A = Json.decodeFromString(serializer<A>(), "{}")

@IgnorableReturnValue
inline fun <reified A : Action> ActionDispatcher.register(handler: ActionHandler<A>) =
    register(A::class, serializer<A>(), handler)

inline fun <reified A : Action> ActionDispatcher.dispatch(): Job = dispatch(defaultAction<A>())

suspend inline fun <reified A : Action> ActionDispatcher.execute() = execute(defaultAction<A>())

inline fun <reified A : Action> action(): () -> Unit {
    val action = defaultAction<A>()
    return { App.global.actions.dispatch(action) }
}

fun action(action: Action): () -> Unit = { App.global.actions.dispatch(action) }

fun <T> actionWith(transform: (T) -> Action): (T) -> Unit =
    { value -> App.global.actions.dispatch(transform(value)) }

@OptIn(ExperimentalTypeInference::class)
@IgnorableReturnValue
@OverloadResolutionByLambdaReturnType
inline fun <reified A : Action> action(handler: ActionHandler<A>): Registration =
    App.global.actions.register(handler)

@Composable
inline fun <reified A : Action> OnAction(noinline handler: suspend ActionContext.(A) -> Unit) {
    val current by rememberUpdatedState(handler)
    DisposableEffect(Unit) {
        val registration = action<A> { a -> current(this, a) }
        onDispose { registration.unregister() }
    }
}

fun Action.dispatch() = App.global.actions.dispatch(this)

suspend fun Action.execute() = App.global.actions.execute(this)
