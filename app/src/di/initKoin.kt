package com.klyx.di

import com.klyx.AppModule
import com.klyx.core.App
import com.klyx.editor.EditorStore
import com.klyx.runtime.workspace.Workspace
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.plugin.module.dsl.startKoin

@org.koin.core.annotation.KoinApplication(modules = [AppModule::class])
class KoinApp

@IgnorableReturnValue
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    val koinApplication = startKoin<KoinApp> { includes(config) }
    val app = App.init(koinApplication)
    val _ = koinApplication.koin.get<Workspace>().install(app.actionRegistrar)
    val _ = koinApplication.koin.get<EditorStore>().install(app.actionRegistrar)
    app.bindDefaultKeybindings()
    return koinApplication
}
