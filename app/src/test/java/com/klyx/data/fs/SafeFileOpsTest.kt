package com.klyx.data.fs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class SafeFileOpsTest : FunSpec({

    val tmp = TemporaryFolder()

    fun path(vararg segments: String): Path {
        var p = tmp.root.toPath()
        segments.forEach { p = p.resolve(it) }
        return p
    }

    beforeSpec {
        tmp.create()
    }

    afterSpec {
        tmp.delete()
    }

    test("deleting a symlink removes only the link") {
        val targetDir = Files.createDirectories(path("precious"))
        val targetFile = Files.writeString(targetDir.resolve("data.txt"), "hello")
        val link = Files.createSymbolicLink(path("link"), targetDir)

        SafeFileOps.delete(link)

        Files.exists(link) shouldBe false
        Files.exists(targetFile) shouldBe true
        targetFile.readText() shouldBe "hello"
    }

    test("deleting a directory unlinks nested symlinks without touching their targets") {
        val outside = Files.createDirectories(path("outside"))
        val precious = Files.writeString(outside.resolve("keep.txt"), "keep")

        val dir = Files.createDirectories(path("project"))
        Files.writeString(dir.resolve("main.kt"), "fun main() {}")
        Files.createSymbolicLink(dir.resolve("escape"), outside)

        SafeFileOps.delete(dir)

        Files.exists(dir) shouldBe false
        Files.exists(precious) shouldBe true
        precious.readText() shouldBe "keep"
    }

    test("delete is a no-op for missing paths") {
        SafeFileOps.delete(path("does-not-exist"))
    }

    test("moveInto renames within same directory tree") {
        val src = Files.createDirectories(path("src"))
        val file = Files.writeString(src.resolve("a.txt"), "abc")
        val destDir = path("dest")

        val moved = SafeFileOps.moveInto(file, destDir)

        Files.exists(file) shouldBe false
        destDir.resolve(moved.fileName).readText() shouldBe "abc"
    }

    test("moveInto preserves symlinks instead of copying their contents") {
        val outside = Files.createDirectories(path("outside"))
        Files.writeString(outside.resolve("f.txt"), "x")
        val link = Files.createSymbolicLink(path("src-link"), outside)

        val moved = SafeFileOps.moveInto(link, path("items"))

        Files.isSymbolicLink(moved) shouldBe true
        Files.readSymbolicLink(moved).absolutePathString() shouldContain outside.absolutePathString()
    }

    test("sizeOf reports regular files and skips symlink targets") {
        val dir = Files.createDirectories(path("d"))
        Files.writeString(dir.resolve("small.txt"), "12345")
        val outside = Files.write(path("big-outside.txt"), ByteArray(1000))
        Files.createSymbolicLink(dir.resolve("sneaky"), outside)

        SafeFileOps.sizeOf(dir) shouldBe 5L
    }
})
