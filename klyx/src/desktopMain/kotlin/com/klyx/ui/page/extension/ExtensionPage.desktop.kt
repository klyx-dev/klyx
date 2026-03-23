package com.klyx.ui.page.extension

import com.klyx.Navigator
import com.klyx.Route
import com.klyx.core.PlatformContext

internal actual fun editOrViewExtension(
    context: PlatformContext,
    navigator: Navigator,
    edit: Boolean,
    filePath: String
) {
    navigator.navigateTo(Route.EditOrViewExtension(filePath, edit = edit))
}
