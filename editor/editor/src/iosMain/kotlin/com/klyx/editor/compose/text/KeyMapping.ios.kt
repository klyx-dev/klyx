package com.klyx.editor.compose.text

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key

@OptIn(ExperimentalComposeUiApi::class)
internal actual object MappedKeys {
    actual val A: Key = Key(Key.A.keyCode)
    actual val C: Key = Key(Key.C.keyCode)
    actual val H: Key = Key(Key.H.keyCode)
    actual val V: Key = Key(Key.V.keyCode)
    actual val X: Key = Key(Key.X.keyCode)
    actual val Y: Key = Key(Key.Y.keyCode)
    actual val Z: Key = Key(Key.Z.keyCode)
    actual val Backslash: Key = Key(Key.Backslash.keyCode)
    actual val DirectionLeft: Key = Key(Key.DirectionLeft.keyCode)
    actual val DirectionRight: Key = Key(Key.DirectionRight.keyCode)
    actual val DirectionUp: Key = Key(Key.DirectionUp.keyCode)
    actual val DirectionDown: Key = Key(Key.DirectionDown.keyCode)
    actual val DirectionCenter: Key = Key(Key.DirectionCenter.keyCode)
    actual val PageUp: Key = Key(Key.PageUp.keyCode)
    actual val PageDown: Key = Key(Key.PageDown.keyCode)
    actual val MoveHome: Key = Key(Key.MoveHome.keyCode)
    actual val MoveEnd: Key = Key(Key.MoveEnd.keyCode)
    actual val Insert: Key = Key(Key.Insert.keyCode)
    actual val Enter: Key = Key(Key.Enter.keyCode)
    actual val NumPadEnter: Key = Key(Key.NumPadEnter.keyCode)
    actual val Backspace: Key = Key(Key.Backspace.keyCode)
    actual val Delete: Key = Key(Key.Delete.keyCode)
    actual val Paste: Key = Key(Key.Paste.keyCode)
    actual val Cut: Key = Key(Key.Cut.keyCode)
    actual val Tab: Key = Key(Key.Tab.keyCode)
    actual val Copy: Key = Key(Key.Copy.keyCode)
}

internal actual val MappedKeys.Space: Key
    get() = Key(Key.Spacebar.keyCode)

internal actual val MappedKeys.F: Key
    get() = Key(Key.F.keyCode)

internal actual val MappedKeys.B: Key
    get() = Key(Key.B.keyCode)

internal actual val MappedKeys.P: Key
    get() = Key(Key.P.keyCode)

internal actual val MappedKeys.N: Key
    get() = Key(Key.N.keyCode)

internal actual val MappedKeys.E: Key
    get() = Key(Key.E.keyCode)

internal actual val MappedKeys.D: Key
    get() = Key(Key.D.keyCode)

internal actual val MappedKeys.K: Key
    get() = Key(Key.K.keyCode)

internal actual val MappedKeys.O: Key
    get() = Key(Key.O.keyCode)

internal actual val platformDefaultKeyMapping: KeyMapping
    get() = createMacosDefaultKeyMapping()
