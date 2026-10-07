package com.klyx.core.event

import com.klyx.core.action.ActionId
import okio.Path

/** Anything the app can announce. */
interface AppEvent

/** A project was opened or closed. */
sealed interface ProjectEvent : AppEvent

data class ProjectOpened(val root: Path) : ProjectEvent
data class ProjectClosed(val root: Path) : ProjectEvent

/** An action ran, or failed while running. */
sealed interface ActionEvent : AppEvent {
    val actionId: ActionId
}

data class ActionExecuted(override val actionId: ActionId) : ActionEvent
data class ActionFailed(override val actionId: ActionId, val cause: Throwable) : ActionEvent
