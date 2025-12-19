package com.klyx.extension.capabilities

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class NpmInstallPackageCapabilityTest : FunSpec() {
    init {
        test("test allows") {
            var capability = NpmInstallPackageCapability("*")
            capability.allows("package").shouldBeTrue()

            capability = NpmInstallPackageCapability("react")
            capability.allows("react").shouldBeTrue()

            capability = NpmInstallPackageCapability("react")
            capability.allows("malicious-package").shouldBeFalse()
        }
    }
}
