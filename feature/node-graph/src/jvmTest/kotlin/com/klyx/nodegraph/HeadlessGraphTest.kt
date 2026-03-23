package com.klyx.nodegraph

import com.klyx.nodegraph.extension.builtin.BuiltinExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

class HeadlessGraphTest : FunSpec({

    test("should load graph.kxng and execute.") {
        val resource = this::class.java.getResource("/graph.kxng")
        resource.shouldNotBeNull()

        val bytes = resource.readBytes()

        val testRegistry = NodeRegistry {
            install(BuiltinExtension)
        }

        val vm = headlessGraph(bytes, testRegistry)
        vm.execute(object : GraphExecutionListener {
            override fun onLog(message: String) {
                println(message)
            }
        })
    }
})
