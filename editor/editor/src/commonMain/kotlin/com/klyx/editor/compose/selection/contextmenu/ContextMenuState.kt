package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import com.klyx.editor.compose.checkPrecondition

private const val UNSPECIFIED_OFFSET_ERROR_MESSAGE =
    "ContextMenuState.Status should never be open with an unspecified offset. " +
            "Use ContextMenuState.Status.Closed instead."

/** Holds state related to the context menu. */
internal class ContextMenuState internal constructor(initialStatus: Status = Status.Closed) {
    var status by mutableStateOf(initialStatus)

    override fun toString(): String = "ContextMenuState(status=$status)"

    override fun hashCode(): Int = status.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ContextMenuState) return false
        return other.status == this.status
    }

    /** The status of the context menu. Can be [Open] or [Closed]. */
    sealed class Status {
        /** An open context menu [Status]. */
        class Open(
            /** The offset to open the menu at. It must be specified. */
            val offset: Offset
        ) : Status() {
            init {
                checkPrecondition(offset.isSpecified) { UNSPECIFIED_OFFSET_ERROR_MESSAGE }
            }

            override fun toString(): String = "Open(offset=$offset)"

            override fun hashCode(): Int = offset.hashCode()

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Open) return false
                return this.offset == other.offset
            }
        }

        /** A closed context menu [Status]. */
        object Closed : Status() {
            override fun toString(): String = "Closed"
        }
    }
}

/** Convenience method to set the state's status to [ContextMenuState.Status.Closed]. */
internal fun ContextMenuState.close() {
    status = ContextMenuState.Status.Closed
}
