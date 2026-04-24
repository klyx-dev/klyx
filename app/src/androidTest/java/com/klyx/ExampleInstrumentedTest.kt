package com.klyx

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.runner.junit4.KotestTestRunner
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(KotestTestRunner::class)
class ExampleInstrumentedTest : FreeSpec() {

    @get:Rule
    val composeTestRule = createComposeRule()

    init {
        "useAppContext" {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            appContext.packageName shouldBe "com.klyx"
        }
    }
}
