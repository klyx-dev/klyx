package com.klyx.core.keybinding

import com.klyx.core.Disposable
import com.klyx.core.Lock
import com.klyx.core.Owner
import com.klyx.core.action.ActionDispatcher
import com.klyx.core.action.ActionId
import com.klyx.core.action.ActionMetadata

/**
 * Maps a key chord such as `"Ctrl+S"` to an [ActionId], and nothing more. It never implements behaviour: a
 * chord is data pointing at an action, and the dispatcher does the rest.
 *
 * Several chords may point at one action, but one chord may only point at one action. Rebinding replaces
 * the previous mapping.
 */
class KeybindingRegistry {
    private val lock = Lock()
    private data class Binding(val actionId: ActionId, val owner: Owner?)

    private val bindings = LinkedHashMap<String, Binding>()

    /** Binds [chord] to [actionId] and returns a handle that unbinds just that chord. */
    @IgnorableReturnValue
    fun bind(chord: String, actionId: ActionId, owner: Owner? = null): Disposable = lock.withLock {
        val normalized = normalize(chord)
        bindings[normalized] = Binding(actionId, owner)
        Disposable { lock.withLock { bindings.remove(normalized) } }
    }

    /** Binds [metadata]'s [ActionMetadata.defaultKeybinding] if it declares one, otherwise does nothing. */
    @IgnorableReturnValue
    fun bindDefault(metadata: ActionMetadata, owner: Owner? = null): Disposable? =
        metadata.defaultKeybinding?.let { bind(it, metadata.id, owner) }

    /** Binds every entry of [bindings] and returns the handles that undo them, in order. */
    @IgnorableReturnValue
    fun bindAll(bindings: Map<String, ActionId>, owner: Owner? = null): List<Disposable> =
        bindings.map { entry -> bind(entry.key, entry.value, owner) }

    fun unbind(chord: String): ActionId? = lock.withLock { bindings.remove(normalize(chord))?.actionId }

    fun resolve(chord: String): ActionId? = lock.withLock { bindings[normalize(chord)]?.actionId }

    /** Removes every chord bound by [owner]. */
    fun unbindAll(owner: Owner) {
        lock.withLock { bindings.entries.removeAll { it.value.owner === owner } }
    }

    fun chords(): Map<String, ActionId> = lock.withLock { bindings.mapValues { it.value.actionId } }

    /**
     * Resolves [chord] and runs what it points at, reporting whether anything was bound. Throws whatever
     * the action throws, so an unbound chord is a no-op and a bound but broken action is an error.
     */
    suspend fun handle(chord: String, dispatcher: ActionDispatcher): Boolean {
        val actionId = resolve(chord) ?: return false
        dispatcher.dispatch(actionId)
        return true
    }

    /**
     * Sorts and lowercases chord parts so `"Shift+Ctrl+Z"` and `"ctrl+shift+z"` are the same chord. A
     * trailing `+` is taken as the plus key, so `"Ctrl++"`.
     */
    private fun normalize(chord: String): String {
        val parts = chord.split('+')
        val keys = if (parts.size > 1 && parts.last().isEmpty()) parts.dropLast(1) + "+" else parts
        return keys.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.sorted().joinToString("+")
    }
}
