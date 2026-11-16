package com.klyx.core.action

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.molecule.RecompositionMode
import app.cash.molecule.SnapshotNotifier
import app.cash.molecule.moleculeFlow
import com.klyx.core.App
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class OnActionTest : FunSpec({
    coroutineTestScope = true

    lateinit var app: App

    beforeTest {
        @OptIn(ExperimentalCoroutinesApi::class)
        Dispatchers.setMain(StandardTestDispatcher())
        app = App.init(startKoin { })
    }

    afterTest {
        app.close()
        stopKoin()
        @OptIn(ExperimentalCoroutinesApi::class)
        Dispatchers.resetMain()
    }

    fun TestScope.settle() {
        Snapshot.sendApplyNotifications()
        testCoroutineScheduler.runCurrent()
    }

    test("OnAction registers while composed and unregisters when it leaves") {
        var show by mutableStateOf(true)
        val job = moleculeFlow(RecompositionMode.Immediate, snapshotNotifier = SnapshotNotifier.External) {
            if (show) OnAction<Ping> { }
        }.launchIn(backgroundScope)
        try {
            settle()
            app.actions.isRegistered(Ping::class).shouldBeTrue()

            show = false
            settle()
            app.actions.isRegistered(Ping::class).shouldBeFalse()
        } finally {
            job.cancelAndJoin()
        }
    }
})
