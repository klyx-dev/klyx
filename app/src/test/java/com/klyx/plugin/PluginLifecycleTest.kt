package com.klyx.plugin

import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.BaseKlyxPlugin
import com.klyx.api.plugin.PluginContext
import com.klyx.api.plugin.currentPluginContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.klyx.api.ui.ScreenEntry
import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.ToolbarRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PluginLifecycleTest : FunSpec({

    fun fakePluginContext(
        screens: ScreenRegistry = FakeScreenRegistry(),
        toolbar: ToolbarRegistry = FakeToolbarRegistry()
    ) = object : PluginContext {
        override val app get() = error("not available")
        override val fileSystem get() = error("not available")
        override val terminalBinder get() = error("not available")
        override val terminalManager get() = error("not available")
        override val paths get() = error("not available")
        override val navigator get() = error("not available")
        override val settings get() = error("not available")
        override val font get() = error("not available")
        override val tabs get() = error("not available")
        override val screens get() = screens
        override val toolbar get() = toolbar
    }

    test("plugin onLoad and onUnload are called") {
        val plugin = TestPlugin()
        plugin.context = fakePluginContext()
        plugin.onLoad(fakePluginContext())

        plugin.loaded shouldBe true
        plugin.unloaded shouldBe false

        plugin.onUnload()
        plugin.unloaded shouldBe true
    }

    test("plugin registers screens during onLoad") {
        val screens = FakeScreenRegistry()
        val plugin = ScreenRegPlugin()

        plugin.context = fakePluginContext(screens = screens)
        plugin.onLoad(fakePluginContext(screens = screens))

        screens.registeredScreens.map { it.id } shouldBe listOf(ScreenId("test.settings"))
    }

    test("plugin registers toolbar actions during onLoad") {
        val toolbar = FakeToolbarRegistry()
        val plugin = ToolbarRegPlugin()

        plugin.context = fakePluginContext(toolbar = toolbar)
        plugin.onLoad(fakePluginContext(toolbar = toolbar))

        toolbar.registeredActions.map { it.id } shouldBe listOf("test.action")
    }

    test("plugin unregisters during onUnload") {
        val screens = FakeScreenRegistry()
        val toolbar = FakeToolbarRegistry()
        val plugin = FullPlugin()
        val ctx = fakePluginContext(screens = screens, toolbar = toolbar)

        plugin.context = ctx
        plugin.onLoad(ctx)
        screens.registeredScreens.map { it.id } shouldBe listOf(ScreenId("full.settings"))
        toolbar.registeredActions.map { it.id } shouldBe listOf("full.action")

        plugin.onUnload()
        screens.registeredScreens shouldBe emptyList()
        toolbar.registeredActions shouldBe emptyList()
    }

    test("currentPluginContext throws when no context is set") {
        shouldThrow<IllegalStateException> {
            currentPluginContext()
        }
    }

    test("currentPluginContext returns the active context") {
        val ctx = fakePluginContext()
        com.klyx.api.plugin.PluginContextHolder.set(ctx)
        currentPluginContext() shouldBe ctx
        com.klyx.api.plugin.PluginContextHolder.clear()
    }

    test("lifecycle starts at INITIALIZED") {
        val plugin = TestPlugin()
        plugin.lifecycle.currentState shouldBe Lifecycle.State.INITIALIZED
    }
})

private class TestPlugin : KlyxPlugin {
    override val lifecycle = LifecycleRegistry(this)
    override var context: PluginContext = UninitializedContext()
    var loaded = false
    var unloaded = false

    override fun onLoad(context: PluginContext) {
        loaded = true
    }

    override fun onUnload() {
        unloaded = true
    }
}

private class ScreenRegPlugin : KlyxPlugin {
    override val lifecycle = LifecycleRegistry(this)
    override var context: PluginContext = UninitializedContext()

    override fun onLoad(context: PluginContext) {
        context.screens.register(ScreenEntry(id = ScreenId("test.settings")) { })
    }

    override fun onUnload() {}
}

private class ToolbarRegPlugin : KlyxPlugin {
    override val lifecycle = LifecycleRegistry(this)
    override var context: PluginContext = UninitializedContext()

    override fun onLoad(context: PluginContext) {
        context.toolbar.register(ToolbarAction(id = "test.action", label = "Test") { })
    }

    override fun onUnload() {}
}

private class FullPlugin : BaseKlyxPlugin() {

    override fun onPluginLoad(context: PluginContext) {
        context.screens.register(ScreenEntry(id = ScreenId("full.settings")) { })
        context.toolbar.register(ToolbarAction(id = "full.action", label = "Full") { })
    }

    override fun onPluginUnload() {
        currentPluginContext().screens.unregister(ScreenId("full.settings"))
        currentPluginContext().toolbar.unregister("full.action")
    }
}

private class UninitializedContext : PluginContext {
    override val app get() = error("PluginContext not initialized")
    override val fileSystem get() = error("not available")
    override val terminalBinder get() = error("not available")
    override val terminalManager get() = error("not available")
    override val paths get() = error("not available")
    override val navigator get() = error("not available")
    override val settings get() = error("not available")
    override val font get() = error("not available")
    override val tabs get() = error("not available")
    override val screens get() = error("not available")
    override val toolbar get() = error("not available")
}

private class FakeScreenRegistry : ScreenRegistry {
    private val _screens = mutableListOf<ScreenEntry>()
    override val registeredScreens: List<ScreenEntry> get() = _screens
    override fun register(screen: ScreenEntry) {
        _screens.add(screen)
    }

    override fun unregister(id: ScreenId) {
        _screens.removeAll { it.id == id }
    }
}

private class FakeToolbarRegistry : ToolbarRegistry {
    private val _actions = mutableListOf<ToolbarAction>()
    override val registeredActions: List<ToolbarAction> get() = _actions
    override fun register(action: ToolbarAction) {
        _actions.add(action)
    }

    override fun unregister(id: String) {
        _actions.removeAll { it.id == id }
    }
}
