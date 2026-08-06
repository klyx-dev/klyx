package com.klyx.data.runner

import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.runner.FileRunnerContext
import com.klyx.api.ui.Content
import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistration
import com.klyx.api.ui.ScreenRegistry
import com.klyx.presentation.navigation.Navigator
import com.klyx.presentation.navigation.Screen

internal class FileRunnerContextImpl(
    private val terminalRunner: TerminalCommandRunner,
    private val navigator: Navigator,
    private val screenRegistry: ScreenRegistry,
    private val openTab: (WorkspaceTab) -> Unit,
) : FileRunnerContext {

    override suspend fun runInTerminal(
        command: String,
        cwd: String?,
        sessionName: String?,
    ) {
        terminalRunner.run(
            navigateToTerminal = { navigator.navigateTo(Screen.Terminal) },
            command = command,
            cwd = cwd,
            sessionName = sessionName,
        )
    }

    override fun openTerminal() {
        navigator.navigateTo(Screen.Terminal)
    }

    override fun openScreen(screenId: ScreenId) {
        navigator.navigateTo(Screen.Custom(screenId))
    }

    override fun openScreen(screenId: ScreenId, content: Content): ScreenRegistration {
        screenRegistry.setTransient(screenId, content)
        navigator.navigateTo(Screen.Custom(screenId))
        return ScreenRegistration { screenRegistry.unregisterTransient(screenId) }
    }

    override fun openTab(tab: WorkspaceTab) {
        openTab.invoke(tab)
    }
}
