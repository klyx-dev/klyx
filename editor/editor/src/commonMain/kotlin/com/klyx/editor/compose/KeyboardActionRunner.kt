package com.klyx.editor.compose

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeAction.Companion.Default
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.ImeAction.Companion.Go
import androidx.compose.ui.text.input.ImeAction.Companion.Next
import androidx.compose.ui.text.input.ImeAction.Companion.None
import androidx.compose.ui.text.input.ImeAction.Companion.Previous
import androidx.compose.ui.text.input.ImeAction.Companion.Search
import androidx.compose.ui.text.input.ImeAction.Companion.Send

/** This class can be used to run keyboard actions when the user triggers an IME action. */
internal class KeyboardActionRunner(private val keyboardController: SoftwareKeyboardController?) :
    KeyboardActionScope {

    /** The developer specified [KeyboardActions]. */
    lateinit var keyboardActions: KeyboardActions

    /** A reference to the [FocusManager] composition local. */
    lateinit var focusManager: FocusManager

    /**
     * Run the keyboard action corresponding to the specified imeAction. If a keyboard action is not
     * specified, use the default implementation provided by [defaultKeyboardAction].
     *
     * @return Whether an action was actually performed.
     */
    fun runAction(imeAction: ImeAction): Boolean {
        val keyboardAction =
            when (imeAction) {
                Done -> keyboardActions.onDone
                Go -> keyboardActions.onGo
                Next -> keyboardActions.onNext
                Previous -> keyboardActions.onPrevious
                Search -> keyboardActions.onSearch
                Send -> keyboardActions.onSend
                Default,
                None -> null

                else -> error("invalid ImeAction")
            }
        if (keyboardAction != null) {
            keyboardAction()
            return true
        } else return defaultKeyboardActionWithResult(imeAction)
    }

    /**
     * Performs the default keyboard action for the given [imeAction], if any.
     *
     * @return whether an action was actually performed.
     */
    private fun defaultKeyboardActionWithResult(imeAction: ImeAction): Boolean {
        return when (imeAction) {
            Next -> {
                focusManager.moveFocus(FocusDirection.Next)
                true
            }

            Previous -> {
                focusManager.moveFocus(FocusDirection.Previous)
                true
            }

            Done -> {
                if (keyboardController != null) {
                    keyboardController.hide()
                    true
                } else false
            }

            else -> false
        }
    }

    /** Default implementations for [KeyboardActions]. */
    override fun defaultKeyboardAction(imeAction: ImeAction) {
        defaultKeyboardActionWithResult(imeAction)
    }
}
