package com.klyx.core.action

import com.klyx.core.App
import com.klyx.core.Disposable
import com.klyx.core.Owner
import kotlinx.coroutines.Job

/**
 * Declares actions.
 */
class ActionRegistrar(
    val registry: ActionRegistry,
    val defaultOwner: Owner? = null,
) {
    /** Declares the action for [id]. */
    @IgnorableReturnValue
    inline fun <reified A : Action> action(
        id: String,
        title: String = titleFromId(id),
        description: String = "",
        category: String? = null,
        icon: String? = null,
        defaultKeybinding: String? = null,
        availability: ActionAvailability = ActionAvailability(),
        owner: Owner? = defaultOwner,
        noinline factory: (() -> A)? = null,
        noinline invocation: (() -> ActionInvocation<A>)? = null,
        serialize: Boolean = false,
        noinline handle: suspend ActionContext.(A) -> Unit,
    ): Disposable = registry.register(
        type = A::class,
        metadata = ActionMetadata(ActionId(id), title, description, category, icon, defaultKeybinding),
        availability = availability,
        owner = owner,
        factory = factory?.let { build -> { ActionInvocation.Ready(build()) } } ?: invocation,
        serialize = serialize,
    ) { context ->
        val typed = context.action as? A
            ?: throw ActionTypeMismatchException(ActionId(id), A::class, context.action::class)
        context.handle(typed)
    }

    /** Declares a parameterless action from a ready instance. */
    @IgnorableReturnValue
    inline fun <reified A : Action> action(
        instance: A,
        title: String = titleFromId(instance.id.value),
        description: String = "",
        category: String? = null,
        icon: String? = null,
        defaultKeybinding: String? = null,
        availability: ActionAvailability = ActionAvailability(),
        owner: Owner? = defaultOwner,
        serialize: Boolean = false,
        noinline handle: suspend ActionContext.(A) -> Unit,
    ): Disposable = action(
        id = instance.id.value,
        title = title,
        description = description,
        category = category,
        icon = icon,
        defaultKeybinding = defaultKeybinding,
        availability = availability,
        owner = owner,
        factory = { instance },
        serialize = serialize,
        handle = handle,
    )

    /** Declares an action that takes arguments, referred to by [ActionDescriptor]. */
    @IgnorableReturnValue
    inline fun <reified A : Action> action(
        descriptor: ActionDescriptor<A>,
        title: String = titleFromId(descriptor.id.value),
        description: String = "",
        category: String? = null,
        icon: String? = null,
        defaultKeybinding: String? = null,
        availability: ActionAvailability = ActionAvailability(),
        owner: Owner? = defaultOwner,
        noinline factory: (() -> A)? = null,
        noinline invocation: (() -> ActionInvocation<A>)? = null,
        serialize: Boolean = false,
        noinline handle: suspend ActionContext.(A) -> Unit,
    ): Disposable = action(
        id = descriptor.id.value,
        title = title,
        description = description,
        category = category,
        icon = icon,
        defaultKeybinding = defaultKeybinding,
        availability = availability,
        owner = owner,
        factory = factory,
        invocation = invocation,
        serialize = serialize,
        handle = handle,
    )
}

/**
 * A bundle of actions declared together.
 */
fun interface ActionGroup {
    fun ActionRegistrar.register(): Disposable
}

fun registerActionGroup(group: ActionGroup, owner: Owner? = null): Disposable {
    val base = App.global.actionRegistrar
    val registrar = if (owner == null) base else ActionRegistrar(base.registry, owner)
    return with(group) { registrar.register() }
}

/** Registers every group and returns one disposable that unwinds all of them. */
@IgnorableReturnValue
fun registerActionGroups(vararg groups: ActionGroup): Disposable {
    val disposables = groups.map { registerActionGroup(it) }
    return Disposable { disposables.asReversed().forEach { it.dispose() } }
}

/** Runs [action] on the app's [background scope][App.backgroundScope] and returns the [Job], so callers may await it. */
@IgnorableReturnValue
fun action(action: Action): Job = App.global.dispatch(action)

/** Runs whatever [id] resolves to through its registered factory. */
@IgnorableReturnValue
fun action(id: ActionId): Job = App.global.dispatch(id)

/** Builds an action inline and runs it, for actions that take arguments. */
@IgnorableReturnValue
inline fun <reified A : Action> action(noinline build: () -> A): Job = action(build())

/** Declares the action for [id]. */
@IgnorableReturnValue
inline fun <reified A : Action> action(
    id: String,
    title: String = titleFromId(id),
    description: String = "",
    category: String? = null,
    icon: String? = null,
    defaultKeybinding: String? = null,
    availability: ActionAvailability = ActionAvailability(),
    owner: Owner? = null,
    noinline factory: (() -> A)? = null,
    noinline invocation: (() -> ActionInvocation<A>)? = null,
    serialize: Boolean = false,
    noinline handle: suspend ActionContext.(A) -> Unit,
): Disposable = App.global.actionRegistrar.action(
    id = id,
    title = title,
    description = description,
    category = category,
    icon = icon,
    defaultKeybinding = defaultKeybinding,
    availability = availability,
    owner = owner,
    factory = factory,
    invocation = invocation,
    serialize = serialize,
    handle = handle,
)

/** Declares a parameterless action from a ready instance. */
@IgnorableReturnValue
inline fun <reified A : Action> action(
    instance: A,
    title: String = titleFromId(instance.id.value),
    description: String = "",
    category: String? = null,
    icon: String? = null,
    defaultKeybinding: String? = null,
    availability: ActionAvailability = ActionAvailability(),
    owner: Owner? = null,
    serialize: Boolean = false,
    noinline handle: suspend ActionContext.(A) -> Unit,
): Disposable = App.global.actionRegistrar.action(
    instance = instance,
    title = title,
    description = description,
    category = category,
    icon = icon,
    defaultKeybinding = defaultKeybinding,
    availability = availability,
    owner = owner,
    serialize = serialize,
    handle = handle,
)

/** Declares an action that takes arguments, referred to by [ActionDescriptor]. */
@IgnorableReturnValue
inline fun <reified A : Action> action(
    descriptor: ActionDescriptor<A>,
    title: String = titleFromId(descriptor.id.value),
    description: String = "",
    category: String? = null,
    icon: String? = null,
    defaultKeybinding: String? = null,
    availability: ActionAvailability = ActionAvailability(),
    owner: Owner? = null,
    noinline factory: (() -> A)? = null,
    noinline invocation: (() -> ActionInvocation<A>)? = null,
    serialize: Boolean = false,
    noinline handle: suspend ActionContext.(A) -> Unit,
): Disposable = App.global.actionRegistrar.action(
    descriptor = descriptor,
    title = title,
    description = description,
    category = category,
    icon = icon,
    defaultKeybinding = defaultKeybinding,
    availability = availability,
    owner = owner,
    factory = factory,
    invocation = invocation,
    serialize = serialize,
    handle = handle,
)

/** Runs [action] and waits for it, propagating any failure. */
suspend fun invoke(action: Action) = App.global.actions.dispatch(action)

/** Runs whatever [id] resolves to and waits for it. */
suspend fun invoke(id: ActionId) = App.global.actions.dispatch(id)

/** Runs [action], reporting an ordinary failure as a [Result]. Cancellation still propagates. */
suspend fun invokeCatching(action: Action): Result<Unit> = App.global.actions.dispatchCatching(action)

@PublishedApi
internal fun titleFromId(id: String): String =
    id.substringAfterLast('.')
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar { it.uppercase() }
