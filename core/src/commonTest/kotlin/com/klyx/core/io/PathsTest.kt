package com.klyx.core.io

import com.klyx.core.dirs
import com.klyx.core.unwrap
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.files.Path

class PathsTest : FunSpec({
    test("Path.toString and String.intoPath") {
        Path("test").toString() shouldBe "test"
        "test".intoPath() shouldBe Path("test")
    }

    test("emptyPath returns empty path") {
        emptyPath() shouldBe Path("")
    }

    test("join() appends segments correctly") {
        val base = Path("/root")
        base.join("a", "b", "c") shouldBe Path("/root", parts = arrayOf("a", "b", "c"))
        base.join("a/b/c") shouldBe Path("/root", parts = arrayOf("a", "b", "c"))
    }

    test("test path") {
        Paths.dataDir shouldBe dirs.dataDir.unwrap().join("klyx")
    }
})
