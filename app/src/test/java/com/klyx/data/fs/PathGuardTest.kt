package com.klyx.data.fs

import com.klyx.api.data.fs.PathGuard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PathGuardTest : FunSpec({

    val guard = PathGuard.of(
        PathGuard.Rule.Prefix("/proc", "Kernel virtual filesystem"),
        PathGuard.Rule.Exact("/storage/self/primary", "Internal storage root"),
        PathGuard.Rule.Exact("/data/app/home", "Terminal Home")
    )

    test("exact rule blocks only the exact path") {
        guard.violation("/storage/self/primary") shouldBe "Internal storage root"
        guard.violation("/storage/self/primary/DCIM/test.txt").shouldBeNull()
    }

    test("prefix rule blocks path itself and all children") {
        guard.violation("/proc") shouldBe "Kernel virtual filesystem"
        guard.violation("/proc/123/status") shouldBe "Kernel virtual filesystem"
    }

    test("exact rule does not block siblings sharing a name prefix") {
        guard.violation("/data/app/home-backup/file.txt").shouldBeNull()
    }

    test("trailing slashes are normalized") {
        guard.violation("/data/app/home/") shouldBe "Terminal Home"
        guard.violation("/data/app/home/projects/myproject").shouldBeNull()
    }

    test("unprotected path returns null") {
        guard.violation("/sdcard/Download/x.txt").shouldBeNull()
        guard.violation("/").shouldBeNull()
    }
})
