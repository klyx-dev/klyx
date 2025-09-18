package com.klyx.editor.input

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi

@ExperimentalCodeEditorApi
internal actual suspend fun PlatformTextInputSessionScope.createInputRequest(
    state: CodeEditorState
): PlatformTextInputMethodRequest {
    return object : PlatformTextInputMethodRequest {
        @ExperimentalComposeUiApi
        override val editText: (block: TextEditingScope.() -> Unit) -> Unit
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val focusedRectInRoot: () -> Rect?
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val imeOptions: ImeOptions
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val onEditCommand: (List<EditCommand>) -> Unit
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val onImeAction: ((ImeAction) -> Unit)?
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val state: TextEditorState
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val textClippingRectInRoot: () -> Rect?
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val textFieldRectInRoot: () -> Rect?
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val textLayoutResult: () -> TextLayoutResult?
            get() = TODO("Not yet implemented")

        @ExperimentalComposeUiApi
        override val value: () -> TextFieldValue
            get() = TODO("Not yet implemented")

    }
}
