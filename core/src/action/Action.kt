package com.klyx.core.action

import com.klyx.core.Owner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

/**
 * Stable identity of an [Action], for example `"workspace.openProject"`.
 */
@JvmInline
value class ActionId(val value: String) {
    init {
        require(value.isNotBlank()) { "ActionId must not be blank" }
        require(value == value.trim()) { "ActionId '$value' must not have surrounding whitespace" }
    }

    override fun toString() = value
}

/**
 * A unit of work with a stable [id]. An action is a plain value: it carries no behaviour, only what the
 * handler needs. A single id may have many instances, one per set of arguments.
 */
interface Action {
    val id: ActionId
}

/**
 * Lets a parameterized action be referred to by id alone, since the type itself has no value to point at.
 * The companion of an action class can implement this.
 */
interface ActionDescriptor<A : Action> {
    val id: ActionId
}

/**
 * Everything a palette, menu or keybinding UI needs to present an action.
 */
data class ActionMetadata(
    val id: ActionId,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val icon: String? = null,
    val defaultKeybinding: String? = null,
)

/**
 * [enabled] gates whether the action runs. [visible] only decides whether a palette offers it, and never
 * stops a direct call.
 */
data class ActionAvailability(val enabled: Boolean = true, val visible: Boolean = true)

/** An action's metadata paired with its current state */
data class ActionListing(
    val metadata: ActionMetadata,
    val enabled: Boolean,
    val visible: Boolean,
)

/** What the registry stores and invokes. Use [ActionRegistrar] rather than building one directly. */
fun interface ActionHandler {
    suspend fun handle(context: ActionContext)
}

/**
 * One registered action. [enabled] and [visible] are live, and [mutex] is present only for actions declared
 * with `serialize = true`, which is how a handler opts into being run one invocation at a time.
 */
class RegisteredAction<A : Action>(
    val type: KClass<A>,
    val metadata: ActionMetadata,
    val owner: Owner?,
    val factory: (() -> ActionInvocation<*>)?,
    val availability: ActionAvailability,
    serialize: Boolean,
    val handler: ActionHandler,
) {
    private val _enabled = MutableStateFlow(availability.enabled)
    private val _visible = MutableStateFlow(availability.visible)

    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    val visible: StateFlow<Boolean> = _visible.asStateFlow()
    val mutex: Mutex? = if (serialize) Mutex() else null

    fun setEnabled(value: Boolean) {
        _enabled.value = value
    }

    fun setVisible(value: Boolean) {
        _visible.value = value
    }

    fun listing() = ActionListing(metadata, _enabled.value, _visible.value)
}

data class InputField(val key: String, val label: String, val placeholder: String = "")

/** What an id needs before it can run: either a ready action, or a form to fill in. */
sealed interface ActionInvocation<out A : Action> {
    data class Ready<A : Action>(val action: A) : ActionInvocation<A>

    data class NeedsInput<A : Action>(
        val fields: List<InputField>,
        val build: (Map<String, String>) -> A,
    ) : ActionInvocation<A>
}

/** Shorthand for the common single-text-field case. */
fun <A : Action> singleInput(prompt: String, build: (String) -> A): ActionInvocation.NeedsInput<A> =
    ActionInvocation.NeedsInput(listOf(InputField("value", prompt))) { values -> build(values.getValue("value")) }

class DuplicateActionException(id: ActionId) :
    IllegalStateException("Action '${id.value}' is already registered")

class UnknownActionException(id: ActionId) :
    IllegalStateException("No action registered for '${id.value}'")

class ActionDisabledException(id: ActionId) :
    IllegalStateException("Action '${id.value}' is disabled")

class ActionTypeMismatchException(id: ActionId, expected: KClass<*>, actual: KClass<*>) :
    IllegalStateException("Action '${id.value}' is a ${actual.simpleName} but its handler expects ${expected.simpleName}")

class ActionNeedsInputException(id: ActionId) :
    IllegalStateException("Action '${id.value}' needs input and cannot be dispatched by id alone")

class ActionOwnerInactiveException(id: ActionId, owner: Owner) :
    IllegalStateException("Action '${id.value}' belongs to unloaded owner '${owner.ownerId}'")

class ActionFactoryMismatchException(id: ActionId, produced: ActionId) :
    IllegalStateException("Factory for '${id.value}' produced '${produced.value}'")
