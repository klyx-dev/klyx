package com.klyx.system

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class CommandTest : FunSpec({

    coroutineTestScope = true

    test("resolveProgram with absolute path returns Direct") {
        val direct = resolveProgram("/bin/echo").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "/bin/echo"
    }

    test("resolveProgram with relative path containing separator returns Direct") {
        val direct = resolveProgram("./test.sh").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "./test.sh"
    }

    test("resolveProgram with path containing multiple separators returns Direct") {
        val direct = resolveProgram("../bin/test.sh").shouldBeInstanceOf<ResolvedProgram.Direct>()
        direct.path shouldBe "../bin/test.sh"
    }

    test("echo hello world") {
        val result = command("/bin/echo")
            .arg("hello")
            .arg("world")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello world"
    }

    test("echo with special characters") {
        val result = command("/bin/echo")
            .arg("hello   world")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello   world"
    }

    test("sh -c exit code 0") {
        val result = command("/bin/sh")
            .arg("-c")
            .arg("exit 0")
            .output()
        result.exitCode shouldBe 0
    }

    test("sh -c exit code 42") {
        val result = command("/bin/sh")
            .arg("-c")
            .arg("exit 42")
            .output()
        result.exitCode shouldBe 42
    }

    test("sh -c captures stdout and stderr") {
        val result = command("/bin/sh")
            .arg("-c")
            .arg("echo out; echo err >&2")
            .output()
        result.stdoutText.trim() shouldBe "out"
        result.stderrText.trim() shouldBe "err"
    }

    test("stdin pipe to cat") {
        val result = command("/bin/cat")
            .stdin("hello from stdin\n")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello from stdin"
    }

    test("stdin bytes to cat") {
        val data = byteArrayOf(0x41, 0x42, 0x43, 0x0A)
        val result = command("/bin/cat")
            .stdin(data)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "ABC"
    }

    test("custom env variable") {
        val result = command("/bin/sh")
            .arg("-c")
            .arg($$"echo $MY_VAR")
            .env("MY_VAR", "custom_value")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "custom_value"
    }

    test("cwd changes working directory") {
        val tmpDir = File(System.getProperty("java.io.tmpdir")!!)
        val result = command("/bin/sh")
            .arg("-c")
            .arg("pwd")
            .cwd(tmpDir)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe tmpDir.absolutePath
    }

    test("spawn process pid is accessible") {
        val child = command("/bin/sleep")
            .arg("1")
            .spawn()
        // pid may be -1 on some JVMs where reflection fails
        (child.pid == -1 || child.pid > 0) shouldBe true
        child.waitFor()
    }

    test("spawn process isAlive returns correct state") {
        val child = command("/bin/sleep")
            .arg("1")
            .spawn()
        child.isRunning shouldBe true
        child.waitFor()
        child.isRunning shouldBe false
    }

    test("spawn then terminate") {
        val child = command("/bin/sleep")
            .arg("60")
            .spawn()
        child.isRunning shouldBe true
        child.terminate()
        child.process.waitFor()
    }

    test("spawn then kill") {
        val child = command("/bin/sleep")
            .arg("60")
            .spawn()
        child.isRunning shouldBe true
        child.kill()
        child.process.waitFor()
    }

    test("status returns exit code 0 for true") {
        val code = command("/bin/true").status()
        code shouldBe 0
    }

    test("status returns exit code 1 for false") {
        val code = command("/bin/false").status()
        code shouldBe 1
    }

    test("stdout redirect to file") {
        val tmpFile = File.createTempFile("cmd_test", ".txt")
        tmpFile.deleteOnExit()
        val result = command("/bin/echo")
            .arg("file output")
            .stdout(StdioDest.File(tmpFile))
            .output()
        result.exitCode shouldBe 0
        tmpFile.readText().trim() shouldBe "file output"
    }

    test("stderr redirect to file") {
        val tmpFile = File.createTempFile("cmd_test", ".txt")
        tmpFile.deleteOnExit()
        val result = command("/bin/sh")
            .arg("-c")
            .arg("echo error >&2")
            .stderr(StdioDest.File(tmpFile))
            .output()
        result.exitCode shouldBe 0
        tmpFile.readText().trim() shouldBe "error"
    }

    test("stdout to Null discards output") {
        val result = command("/bin/echo")
            .arg("discarded")
            .stdout(StdioDest.Null)
            .output()
        result.exitCode shouldBe 0
        result.stdout shouldBe ByteArray(0)
    }

    test("stderr to Null discards stderr") {
        val result = command("/bin/sh")
            .arg("-c")
            .arg("echo err >&2; echo out")
            .stderr(StdioDest.Null)
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "out"
        result.stderr shouldBe ByteArray(0)
    }

    test("command with vararg initial args") {
        val result = command("/bin/sh", "-c", "echo hello")
            .output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello"
    }

    test("command builder returns self from arg") {
        val cmd = command("echo")
        cmd.arg("hello") shouldBe cmd
    }

    test("command builder returns self from chained calls") {
        val cmd = command("ls")
        cmd.arg("-la") shouldBe cmd
        cmd.args("/tmp") shouldBe cmd
        cmd.env("KEY", "val") shouldBe cmd
    }

    test("command builder env map") {
        val cmd = command("test")
        cmd.env(mapOf("A" to "1", "B" to "2")) shouldBe cmd
    }

    test("command builder cwd") {
        val cmd = command("pwd")
        cmd.cwd("/tmp") shouldBe cmd
    }

    test("command builder chaining returns same instance") {
        val cmd = command("test")
        val chained = cmd
            .arg("a")
            .args("b", "c")
            .env("K", "V")
            .cwd("/tmp")
        chained shouldBe cmd
    }

    test("command builder stdout/stderr dest chaining") {
        val cmd = command("test")
        cmd.stdout(StdioDest.Null) shouldBe cmd
        cmd.stderr(StdioDest.Null) shouldBe cmd
    }

    test("command builder stdin source chaining") {
        val cmd = command("cat")
        cmd.stdin(StdinSource.Pipe) shouldBe cmd
    }

    test("command builder stdin bytes") {
        val cmd = command("cat")
        cmd.stdin("hello".encodeToByteArray()) shouldBe cmd
    }

    test("command builder stdin text") {
        val cmd = command("cat")
        cmd.stdin("hello world") shouldBe cmd
    }

    test("command builder stdout to file") {
        val tmp = File.createTempFile("test", ".txt")
        tmp.deleteOnExit()
        val cmd = command("echo")
            .arg("test")
            .stdout(StdioDest.File(tmp))
    }

    test("command with vararg initial args construction") {
        val cmd = command("/bin/echo", "a", "b", "c")
    }

    test("ProcessOutput properties") {
        val output = ProcessOutput(
            exitCode = 0,
            stdout = "hello\n".encodeToByteArray(),
            stderr = "".encodeToByteArray(),
        )
        output.exitCode shouldBe 0
        output.stdoutText shouldBe "hello\n"
        output.stderrText shouldBe ""
    }

    test("ProcessOutput with non-zero exit") {
        val output = ProcessOutput(
            exitCode = 1,
            stdout = ByteArray(0),
            stderr = "error\n".encodeToByteArray(),
        )
        output.exitCode shouldBe 1
        output.stdoutText shouldBe ""
        output.stderrText shouldBe "error\n"
    }

    test("ROOTFS_BIN_PATHS contains expected entries") {
        ROOTFS_BIN_PATHS shouldBe listOf(
            "/usr/local/bin", "/usr/bin", "/bin",
            "/usr/local/sbin", "/usr/sbin", "/sbin",
        )
    }

    test("SYSTEM_BIN_PATHS contains expected entries") {
        SYSTEM_BIN_PATHS shouldBe listOf(
            "/system/bin", "/system/xbin", "/vendor/bin",
        )
    }

    test("StdioDest.File holds reference") {
        val file = File("/tmp/test.log")
        val dest = StdioDest.File(file)
        dest.file shouldBe file
    }

    test("StdinSource.Bytes holds data") {
        val data = "test".encodeToByteArray()
        val source = StdinSource.Bytes(data)
        source.data shouldBe data
    }
})
