package com.klyx.core.noderuntime

import com.klyx.core.process.Command
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.Url

class NodeRuntimeTest : FunSpec({
    test("configure npm command map localhost proxy") {
        val cases = mapOf(
            // Map localhost to 127.0.0.1
            "http://localhost:9090/" to "http://127.0.0.1:9090/",
            "https://google.com/" to "https://google.com/",
            "http://username:password@proxy.thing.com:8080/" to "http://username:password@proxy.thing.com:8080/",
            // Test when localhost is contained within a different part of the URL
            "http://username:localhost@localhost:8080/" to "http://username:localhost@127.0.0.1:8080/"
        )

        for ((proxy, mappedProxy) in cases) {
            val dummy = Command.newCommand("")
            val proxy = Url(proxy)
            configureNpmCommand(dummy, null, proxy)
            val pr = dummy
                .args.toMutableList()
                .dropWhile { it != "--proxy" }
                .drop(1)
                .first()
            pr shouldBe mappedProxy
        }
    }
})
