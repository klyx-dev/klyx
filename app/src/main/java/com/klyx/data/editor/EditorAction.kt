package com.klyx.data.editor

import com.klyx.data.file.KxFile

sealed interface EditorAction

data class Save(val file: KxFile) : EditorAction
data class SaveAs(val oldTabId: String, val newFile: KxFile) : EditorAction
