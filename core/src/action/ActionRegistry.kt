package com.klyx.core.action

import com.klyx.core.Disposable
import com.klyx.core.Lock
import com.klyx.core.Owner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Holds the registered actions and answers questions about them.
 */
class ActionRegistry {
    private val lock = Lock()
    private val actions = LinkedHashMap<ActionId, RegisteredAction<*>>()
    private val _all = MutableStateFlow<List<ActionListing>>(emptyList())

    /** Live snapshot of every registered action, which is what a command palette enumerates. */
    val all: StateFlow<List<ActionListing>> = _all.asStateFlow()

    /**
     * Registers a handler for [type] and returns a handle that unregisters exactly that entry. Disposing a
     * handle whose action has since been replaced is a no-op.
     *
     * Throws [DuplicateActionException] if [ActionMetadata.id] is taken. Ids are a flat namespace, so
     * prefix your own.
     */
    @IgnorableReturnValue
    fun <A : Action> register(
        type: KClass<A>,
        metadata: ActionMetadata,
        availability: ActionAvailability = ActionAvailability(),
        owner: Owner? = null,
        factory: (() -> ActionInvocation<*>)? = null,
        serialize: Boolean = false,
        handler: ActionHandler,
    ): Disposable {
        val entry = RegisteredAction(type, metadata, owner, factory, availability, serialize, handler)
        return lock.withLock {
            if (actions.containsKey(metadata.id)) throw DuplicateActionException(metadata.id)
            actions[metadata.id] = entry
            publish()
            Disposable {
                lock.withLock {
                    if (actions[metadata.id] === entry) {
                        actions.remove(metadata.id)
                        publish()
                    }
                }
            }
        }
    }

    /** Removes whatever is registered under [id], if anything. */
    fun unregister(id: ActionId) {
        lock.withLock {
            if (actions.remove(id) != null) publish()
        }
    }

    /** Removes every action contributed by [owner]. */
    fun unregisterAll(owner: Owner) {
        lock.withLock {
            actions.entries.removeAll { it.value.owner === owner }
            publish()
        }
    }

    fun lookup(id: ActionId): RegisteredAction<*>? = lock.withLock { actions[id] }

    fun isRegistered(id: ActionId): Boolean = lock.withLock { actions.containsKey(id) }

    fun metadataOf(id: ActionId): ActionMetadata? = lock.withLock { actions[id]?.metadata }

    fun isEnabled(id: ActionId): Boolean = lock.withLock { actions[id]?.enabled?.value ?: false }

    fun isVisible(id: ActionId): Boolean = lock.withLock { actions[id]?.visible?.value ?: false }

    fun setEnabled(id: ActionId, enabled: Boolean) {
        lock.withLock {
            actions[id]?.setEnabled(enabled)
            publish()
        }
    }

    fun setVisible(id: ActionId, visible: Boolean) {
        lock.withLock {
            actions[id]?.setVisible(visible)
            publish()
        }
    }

    /** Current metadata and state of every registered action, in registration order. */
    fun list(): List<ActionListing> = lock.withLock { actions.values.map { it.listing() } }

    fun listVisible(): List<ActionListing> = lock.withLock {
        actions.values.map { it.listing() }.filter { it.visible }
    }

    /** Chords declared by registered actions via [ActionMetadata.defaultKeybinding], chord to action id. */
    fun defaultKeybindings(): Map<String, ActionId> = lock.withLock {
        actions.values.mapNotNull { entry ->
            entry.metadata.defaultKeybinding?.let { it to entry.metadata.id }
        }.toMap()
    }

    private fun publish() {
        _all.value = actions.values.map { it.listing() }
    }
}
