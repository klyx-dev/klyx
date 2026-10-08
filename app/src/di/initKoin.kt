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
    val koin = koinApplication.koin
    val app = App.init(koinApplication)
    context(app.actionRegistrar) {
        val _ = koin.get<Workspace>().registerActions()
        val _ = koin.get<EditorStore>().registerActions()
    }
    app.bindDefaultKeybindings()
    return koinApplication
}
