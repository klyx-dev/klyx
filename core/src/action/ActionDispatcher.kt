package com.klyx.core.action

import com.klyx.core.event.ActionEvent
import com.klyx.core.event.ActionExecuted
import com.klyx.core.event.ActionFailed
import com.klyx.core.event.EventEmitter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import org.koin.core.Koin

/**
 * The one place every part of the app runs actions through: keybindings, the command palette, menus.
 * It looks the id up, checks the action is runnable, then hands the handler an [ActionContext].
 * What the handler does is entirely its own business.
 *
 * A handler that throws cannot corrupt anything the dispatcher owns, because the dispatcher owns no state.
 * Its only job on failure is to emit [ActionFailed] and let the exception out.
 *
 * Two invocations of the same action may overlap unless that action was registered with `serialize = true`,
 * which is why document-scoped actions should ask for it.
 */
class ActionDispatcher(
    private val registry: ActionRegistry,
    private val koin: Koin,
) {
    private val emitter = EventEmitter<ActionEvent>()
    val events = emitter.events

    /**
     * Runs [action]. Throws [UnknownActionException] if nothing is registered for its id,
     * [ActionDisabledException] if it is registered but off, and [ActionTypeMismatchException] if the
     * handler was declared for a different action type. Propagates whatever the handler throws, including
     * [CancellationException] untouched, after emitting [ActionFailed].
     *
     * [RegisteredAction.visible] is not consulted: hidden means a palette should not offer it, not that
     * code cannot call it.
     */
    @IgnorableReturnValue
    suspend fun dispatch(action: Action) {
        val entry = registry.lookup(action.id) ?: throw UnknownActionException(action.id)
        if (!entry.enabled.value) throw ActionDisabledException(action.id)
        entry.owner?.takeIf { !it.isActive }?.let { throw ActionOwnerInactiveException(action.id, it) }

        val run: suspend () -> Unit = { runHandler(entry, action) }
        entry.mutex?.let { mutex -> mutex.withLock { run() } } ?: run()
    }

    /**
     * Runs whatever [id] resolves to, using the factory recorded at registration. Throws
     * [ActionNeedsInputException] if that action cannot be built without arguments, so a caller that can
     * prompt the user. Use [dispatchOrNull] to inspect first.
     */
    suspend fun dispatch(id: ActionId) {
        val entry = registry.lookup(id) ?: throw UnknownActionException(id)
        val invocation = entry.factory?.invoke() ?: throw ActionNeedsInputException(id)
        when (invocation) {
            is ActionInvocation.NeedsInput -> throw ActionNeedsInputException(id)
            is ActionInvocation.Ready -> {
                if (invocation.action.id != id) throw ActionFactoryMismatchException(id, invocation.action.id)
                dispatch(invocation.action)
            }
        }
    }

    /**
     * The invocation [id] would produce, or null if nothing is registered. Never runs anything.
     */
    fun dispatchOrNull(id: ActionId): ActionInvocation<*>? = registry.lookup(id)?.factory?.invoke()

    /** Like [dispatch], but reports an ordinary failure as a [Result]. Cancellation is never captured. */
    suspend fun dispatchCatching(action: Action): Result<Unit> = try {
        dispatch(action)
        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    fun isEnabled(id: ActionId): Boolean = registry.isEnabled(id)

    private suspend fun runHandler(entry: RegisteredAction<*>, action: Action) {
        val context = DefaultActionContext(action, koin)
        try {
            entry.handler.handle(context)
            emitter.emit(ActionExecuted(action.id))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            emitter.emit(ActionFailed(action.id, failure))
            throw failure
        }
    }
}
