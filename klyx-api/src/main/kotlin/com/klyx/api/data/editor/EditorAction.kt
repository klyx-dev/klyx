package com.klyx.api.data.editor

import com.klyx.api.data.file.KxFile

sealed interface EditorAction

data class Save(val file: KxFile) : EditorAction
data class SaveAs(val oldTabId: String, val newFile: KxFile) : EditorAction
