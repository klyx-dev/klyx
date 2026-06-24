package com.klyx.system

import androidx.test.platform.app.InstrumentationRegistry
import com.klyx.data.fs.Paths
import com.klyx.terminal.rootFs
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.runner.junit4.KotestTestRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(KotestTestRunner::class)
class CommandInstrumentedTest : FreeSpec({

    coroutineTestScope = true

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    "resolveProgram finds system echo" {
        val direct = resolveProgram("echo").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "/system/bin/echo"
    }

    "resolveProgram finds system sh" {
        val direct = resolveProgram("sh").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "/system/bin/sh"
    }

    "resolveProgram with absolute path returns Direct as-is" {
        val direct = resolveProgram("/system/bin/echo").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "/system/bin/echo"
    }

    "resolveProgram with non-existent program returns Direct fallback" {
        val direct = resolveProgram("nonexistent_cmd_xyz").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "nonexistent_cmd_xyz"
    }

    "echo hello world" {
        val result = command("/system/bin/echo")
            .arg("hello")
            .arg("world")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello world"
    }

    "echo with special characters" {
        val result = command("/system/bin/echo")
            .arg("hello   world")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello   world"
    }

    "sh -c exit code 0" {
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("exit 0")
            .output()
        result.exitCode shouldBe 0
    }

    "sh -c exit code 42" {
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("exit 42")
            .output()
        result.exitCode shouldBe 42
    }

    "sh -c captures stdout and stderr" {
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("echo out; echo err >&2")
            .output()
        result.stdoutText.trim() shouldBe "out"
        result.stderrText.trim() shouldBe "err"
    }

    "status returns exit code 0 for true" {
        val code = command("/system/bin/true").status()
        code shouldBe 0
    }

    "status returns exit code 1 for false" {
        val code = command("/system/bin/false").status()
        code shouldBe 1
    }

    "stdin pipe to cat" {
        val result = command("/system/bin/cat")
            .stdin("hello from stdin\n")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello from stdin"
    }

    "stdin bytes to cat" {
        val data = byteArrayOf(0x41, 0x42, 0x43, 0x0A)
        val result = command("/system/bin/cat")
            .stdin(data)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "ABC"
    }

    "spawn and interact with stdin/stdout" {
        val child = command("/system/bin/sh")
            .stdin(StdinSource.Pipe)
            .spawn()

        child.pid shouldNotBe 0
        child.isRunning shouldBe true

        run {
            child.stdin.write("echo hello\n".toByteArray())
            child.stdin.write("exit\n".toByteArray())
            child.stdin.flush()
        }

        val result = child.waitFor()
        result.exitCode shouldBe 0
        result.stdoutText shouldContain "hello"
    }

    "spawn process pid is valid" {
        val child = command("/system/bin/sh")
            .arg("-c")
            .arg("sleep 1")
            .spawn()
        child.pid shouldNotBe 0
        child.waitFor()
    }

    "spawn then terminate" {
        val child = command("/system/bin/sh")
            .arg("-c")
            .arg("sleep 60")
            .spawn()
        child.isRunning shouldBe true
        child.pid shouldNotBe 0
        child.terminate()
        child.waitFor()
        child.isRunning shouldBe false
    }

    "spawn then kill" {
        val child = command("/system/bin/sh")
            .arg("-c")
            .arg("sleep 60")
            .spawn()
        child.isRunning shouldBe true
        child.kill()
        child.waitFor()
    }

    "spawn with timeout" {
        val child = command("/system/bin/sh")
            .arg("-c")
            .arg("echo done; sleep 60")
            .spawn()
        val result = child.waitForTimeout(1000)
        if (result == null) {
            child.kill()
        } else {
            result.stdoutText.trim() shouldBe "done"
        }
    }

    "custom env variable" {
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg($$"echo $MY_VAR")
            .env("MY_VAR", "custom_value")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "custom_value"
    }

    "cwd changes working directory" {
        val tmpDir = appContext.cacheDir
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("pwd")
            .cwd(tmpDir)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe tmpDir.absolutePath
    }

    "stdout to Null discards output" {
        val result = command("/system/bin/echo")
            .arg("discarded")
            .stdout(StdioDest.Null)
            .output()
        result.exitCode shouldBe 0
        result.stdout shouldBe ByteArray(0)
    }

    "stderr to Null discards stderr" {
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("echo err >&2; echo out")
            .stderr(StdioDest.Null)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "out"
        result.stderr shouldBe ByteArray(0)
    }

    "stdout to file" {
        val tmpFile = File(appContext.cacheDir, "cmd_test_stdout.txt")
        tmpFile.delete()
        val result = command("/system/bin/echo")
            .arg("file output")
            .stdout(StdioDest.File(tmpFile))
            .output()
        result.exitCode shouldBe 0
        tmpFile.readText().trim() shouldBe "file output"
        tmpFile.delete()
    }

    "stderr to file" {
        val tmpFile = File(appContext.cacheDir, "cmd_test_stderr.txt")
        tmpFile.delete()
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("echo error >&2")
            .stderr(StdioDest.File(tmpFile))
            .output()
        result.exitCode shouldBe 0
        tmpFile.readText().trim() shouldBe "error"
        tmpFile.delete()
    }

    "command with vararg initial args" {
        val result = command("/system/bin/sh", "-c", "echo hello")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello"
    }

    "stdout inherit does not capture" {
        val result = command("/system/bin/echo")
            .arg("inherited")
            .stdout(StdioDest.Inherit)
            .output()
        result.exitCode shouldBe 0
        result.stdout shouldBe ByteArray(0)
    }

    "stderr inherit does not capture" {
        val result = command("/system/bin/sh")
            .arg("-c")
            .arg("echo err >&2; echo out")
            .stderr(StdioDest.Inherit)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "out"
        result.stderr shouldBe ByteArray(0)
    }

    val rootFsInstalled = try {
        Paths.rootFs.exists()
    } catch (_: Exception) {
        false
    }

    "resolveProgram with absolute rootfs path resolves to PRoot" {
        if (!rootFsInstalled) return@invoke

        val shInRootfs = File(Paths.rootFs, "bin/sh")
        if (!shInRootfs.exists()) return@invoke

        val proot = resolveProgram("/bin/sh").shouldBeInstanceOf<ResolvedProgram.PRoot>()
        proot.path shouldBe shInRootfs.absolutePath
    }

    "resolveProgram with /usr/bin/ path inside rootfs resolves to PRoot" {
        if (!rootFsInstalled) return@invoke

        val shInRootfs = File(Paths.rootFs, "usr/bin/sh")
        if (!shInRootfs.exists()) return@invoke

        val proot = resolveProgram("/usr/bin/sh").shouldBeInstanceOf<ResolvedProgram.PRoot>()
        proot.path shouldBe shInRootfs.absolutePath
    }

    "resolveProgram finds bash in rootfs when installed" {
        if (!rootFsInstalled) return@invoke

        val bashInRootfs = File(Paths.rootFs, "bin/bash")
        if (!bashInRootfs.exists()) return@invoke

        val result = resolveProgram("bash")
        result.shouldBeInstanceOf<ResolvedProgram.PRoot>()
    }

    "resolveProgram finds ls in rootfs when installed" {
        if (!rootFsInstalled) return@invoke

        val lsInRootfs = File(Paths.rootFs, "bin/ls")
        if (!lsInRootfs.exists()) return@invoke

        val result = resolveProgram("ls")
        result.shouldBeInstanceOf<ResolvedProgram.PRoot>()
    }
})
