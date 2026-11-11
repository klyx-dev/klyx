package com.klyx.di

import com.klyx.AppModule
import com.klyx.core.App
import com.klyx.editor.EditorStore
import com.klyx.lsp.LspActions
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.theme.ThemeRegistry
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.plugin.module.dsl.startKoin

@org.koin.core.annotation.KoinApplication(modules = [AppModule::class])
class KoinApp

@IgnorableReturnValue
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    val koinApplication = startKoin<KoinApp> { includes(config) }
    val koin = koinApplication.koin
    val app = App.init(koinApplication)
    koin.get<Workspace>().registerActions()
    koin.get<EditorStore>().registerActions()
    koin.get<LspActions>().registerActions()
    app.backgroundScope.launch { ThemeRegistry.init() }
    return koinApplication
}
