package com.klyx.extension.capabilities

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.ktor.http.Url

class DownloadFileCapabilityTest : FunSpec({
    test("test allows") {
        var capability = DownloadFileCapability(
            host = "*",
            path = listOf("**")
        )
        capability.allows(Url("https://example.com/some/path")).shouldBeTrue()

        capability = DownloadFileCapability(
            host = "github.com",
            path = listOf("**")
        )
        capability.allows(Url("https://github.com/some-owner/some-repo")).shouldBeTrue()
        capability.allows(Url("https://fake-github.com/some-owner/some-repo")).shouldBeFalse()

        capability = DownloadFileCapability(
            host = "github.com",
            path = listOf("specific-owner", "*")
        )
        capability.allows(Url("https://github.com/some-owner/some-repo")).shouldBeFalse()
        capability.allows(Url("https://github.com/specific-owner/some-repo")).shouldBeTrue()

        capability = DownloadFileCapability(
            host = "github.com",
            path = listOf("specific-owner", "*")
        )
        capability.allows(Url("https://github.com/some-owner/some-repo/extra")).shouldBeFalse()
        capability.allows(Url("https://github.com/specific-owner/some-repo/extra")).shouldBeFalse()
    }
})
