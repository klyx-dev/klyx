package com.klyx.ui.page.extension

import android.content.Intent
import com.klyx.Navigator
import com.klyx.activities.EditExtensionActivity
import com.klyx.core.PlatformContext

internal actual fun editOrViewExtension(
    context: PlatformContext,
    navigator: Navigator,
    edit: Boolean,
    filePath: String
) {
    context.startActivity(
        Intent(context, EditExtensionActivity::class.java).apply {
            putExtra("edit", edit)
            putExtra("filePath", filePath)
        }
    )
}
