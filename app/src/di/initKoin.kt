package com.klyx.di

import com.klyx.AppModule
import com.klyx.core.App
import com.klyx.core.init.InitializerManager
import com.klyx.editor.EditorStore
import com.klyx.lsp.LspActions
import com.klyx.runtime.workspace.WorkspaceActionHandlers
import com.klyx.runtime.workspace.WorkspaceSessionManager
import com.klyx.ui.theme.ThemeRegistry
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.plugin.module.dsl.startKoin
import org.koin.core.annotation.KoinApplication as AKoinApplication

@AKoinApplication(modules = [AppModule::class])
class KoinApp

@IgnorableReturnValue
fun initApp(config: KoinAppDeclaration? = null): KoinApplication {
    val koinApplication = startKoin<KoinApp> { includes(config) }
    val koin = koinApplication.koin
    val app = App.init(koinApplication)

    koin.get<WorkspaceActionHandlers>().register()
    koin.get<EditorStore>().registerActions()
    koin.get<WorkspaceSessionManager>().start()
    koin.get<LspActions>().registerActions()

    val initializerManager = InitializerManager(
        initializers = koin.getAll(),
        scope = app.backgroundScope
    )
    app.initializer = initializerManager
    app.backgroundScope.launch {
        ThemeRegistry.initialize()
        initializerManager.start()
    }
    return koinApplication
}
