package com.klyx.system

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CommandStreamingTest : FunSpec({

    coroutineTestScope = true

    test("Stdout holds data and text") {
        val e = ProcessOutputEvent.Stdout("hello\n".encodeToByteArray())
        e.text shouldBe "hello\n"
        e.data shouldBe "hello\n".encodeToByteArray()
    }

    test("Stderr holds data and text") {
        val e = ProcessOutputEvent.Stderr("err\n".encodeToByteArray())
        e.text shouldBe "err\n"
    }

    test("ExitCode holds code") {
        ProcessOutputEvent.ExitCode(42).code shouldBe 42
    }

    test("Stdout data equality") {
        ProcessOutputEvent.Stdout("a".encodeToByteArray()) shouldBe
                ProcessOutputEvent.Stdout("a".encodeToByteArray())
    }

    test("Stderr data equality") {
        ProcessOutputEvent.Stderr("b".encodeToByteArray()) shouldBe
                ProcessOutputEvent.Stderr("b".encodeToByteArray())
    }

    test("Stdout and Stderr are not equal") {
        ProcessOutputEvent.Stdout("x".encodeToByteArray()) shouldNotBe
                ProcessOutputEvent.Stderr("x".encodeToByteArray())
    }

    test("shell runs script via sh -c") {
        val result = shell("echo hello shell").output()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "hello shell"
    }

    test("shell with exit code") {
        shell("exit 42").status() shouldBe 42
    }

    test("commandExists true for /bin/echo") {
        commandExists("/bin/echo") shouldBe true
    }

    test("commandExists true for known command") {
        commandExists("/bin/echo") shouldBe true
    }

    test("commandExists false for nonexistent") {
        commandExists("xyznonexistent_12345") shouldBe false
    }

    test("which returns path for absolute path") {
        val path = which("/bin/echo")
        path shouldNotBe null
        path shouldBe "/bin/echo"
    }

    test("which returns null for nonexistent") {
        which("xyznonexistent_12345") shouldBe null
    }

    test("which finds command by name via fallback") {
        val path = runBlocking { which("echo") }
        path shouldNotBe null
        path!!.endsWith("/echo") shouldBe true
    }

    test("firstAvailable returns first match") {
        val path = firstAvailable("xyznonexistent", "echo")
        path shouldNotBe null
        path!!.endsWith("/echo") shouldBe true
    }

    test("firstAvailable returns earliest match") {
        val path = firstAvailable("/bin/echo", "echo")
        path shouldBe "/bin/echo"
    }

    test("firstAvailable returns null when none found") {
        firstAvailable("cmd_a_12345", "cmd_b_67890") shouldBe null
    }

    test("firstAvailable with iterable") {
        val path = firstAvailable(listOf("fake", "/bin/true"))
        path shouldBe "/bin/true"
    }

    test("firstAvailable with vararg picks first") {
        val path = firstAvailable("/bin/true", "/bin/echo")
        path shouldBe "/bin/true"
    }

    test("stream emits Stdout events") {
        val events = command("/bin/echo", "stream test").stream().toList()
        events.any {
            it is ProcessOutputEvent.Stdout && it.text.trim() == "stream test"
        } shouldBe true
    }

    test("stream emits Stderr events") {
        val events = command("/bin/sh", "-c", "echo err_stream >&2").stream().toList()
        events.any {
            it is ProcessOutputEvent.Stderr && it.text.trim() == "err_stream"
        } shouldBe true
    }

    test("stream emits ExitCode 0 on success") {
        val events = command("/bin/true").stream().toList()
        events.filterIsInstance<ProcessOutputEvent.ExitCode>().single().code shouldBe 0
    }

    test("stream emits ExitCode 1 on failure") {
        val events = command("/bin/false").stream().toList()
        events.filterIsInstance<ProcessOutputEvent.ExitCode>().single().code shouldBe 1
    }

    test("stream includes both stdout and stderr") {
        val events = command("/bin/sh", "-c", "echo out; echo err >&2; exit 3").stream().toList()
        events.any { it is ProcessOutputEvent.Stdout && it.text.trim() == "out" } shouldBe true
        events.any { it is ProcessOutputEvent.Stderr && it.text.trim() == "err" } shouldBe true
        events.filterIsInstance<ProcessOutputEvent.ExitCode>().single().code shouldBe 3
    }

    test("ChildProcess.flow emits events") {
        val child = command("/bin/echo", "child flow").spawn()
        val events = child.flow().toList()
        events.any { it is ProcessOutputEvent.Stdout && it.text.trim() == "child flow" } shouldBe true
    }

    test("ChildProcess.flow emits ExitCode") {
        val child = command("/bin/sh", "-c", "exit 5").spawn()
        val events = child.flow().toList()
        events.filterIsInstance<ProcessOutputEvent.ExitCode>().single().code shouldBe 5
    }

    test("outputText returns stdout as string") {
        command("/bin/echo", "text output").outputText().trim() shouldBe "text output"
    }

    test("outputText with multiple lines") {
        val text = command("/bin/sh", "-c", "echo a; echo b; echo c").outputText()
        text.trimEnd('\n').split('\n') shouldBe listOf("a", "b", "c")
    }

    test("outputLines splits stdout into lines") {
        val lines = command("/bin/sh", "-c", "echo x; echo y").outputLines()
        lines shouldBe listOf("x", "y", "")
    }

    test("outputLines with single line") {
        command("/bin/echo", "single").outputLines() shouldBe listOf("single", "")
    }

    test("isSuccess true for exit 0") {
        command("/bin/true").isSuccess() shouldBe true
    }

    test("isSuccess false for non-zero exit") {
        command("/bin/false").isSuccess() shouldBe false
    }

    test("isSuccess false for exit 42") {
        command("/bin/sh", "-c", "exit 42").isSuccess() shouldBe false
    }

    test("isFailure false for exit 0") {
        command("/bin/true").isFailure() shouldBe false
    }

    test("isFailure true for non-zero exit") {
        command("/bin/false").isFailure() shouldBe true
    }

    test("outputWithTimeout returns ProcessOutput when fast enough") {
        val result = runBlocking {
            command("/bin/echo", "timed").outputWithTimeout(30.seconds)
        }
        result shouldNotBe null
        result!!.stdoutText.trim() shouldBe "timed"
    }

    test("outputWithTimeout returns null when process exceeds timeout") {
        val result = runBlocking {
            command("/bin/sleep", "10").outputWithTimeout(50.milliseconds)
        }
        result shouldBe null
    }

    test("retry 0 returns first result even on failure") {
        command("/bin/false").retry(0).exitCode shouldBe 1
    }

    test("retry returns successful result immediately") {
        command("/bin/echo", "ok").retry(3).stdoutText.trim() shouldBe "ok"
    }

    test("retry exhausts attempts on persistent failure") {
        command("/bin/false").retry(2).exitCode shouldBe 1
    }

    test("result returns Success") {
        command("/bin/true").result().isSuccess shouldBe true
    }

    test("result returns Success even for non-zero") {
        command("/bin/false").result().isSuccess shouldBe true
    }

    test("result holds ProcessOutput") {
        val r = command("/bin/echo", "res").result()
        r.getOrThrow().stdoutText.trim() shouldBe "res"
    }

    test("waitForText returns stdout") {
        val child = command("/bin/echo", "wait text").spawn()
        child.waitForText().trim() shouldBe "wait text"
    }

    test("waitForLines splits stdout") {
        val child = command("/bin/sh", "-c", "echo a; echo b").spawn()
        child.waitForLines() shouldBe listOf("a", "b", "")
    }

    test("waitForTimeoutText returns text when fast enough") {
        val child = command("/bin/echo", "fast text").spawn()
        val t = child.waitForTimeoutText(5.seconds)
        t shouldNotBe null
        t!!.trim() shouldBe "fast text"
    }

    test("waitForTimeoutText returns null on timeout") {
        val child = command("/bin/sleep", "10").spawn()
        try {
            child.waitForTimeoutText(50.milliseconds) shouldBe null
        } finally {
            child.kill()
        }
    }

    test("waitForTimeoutLines returns lines when fast enough") {
        val child = command("/bin/sh", "-c", "echo x; echo y").spawn()
        val lines = child.waitForTimeoutLines(5.seconds)
        lines shouldNotBe null
        lines shouldBe listOf("x", "y", "")
    }

    test("waitForTimeoutLines returns null on timeout") {
        val child = command("/bin/sleep", "10").spawn()
        try {
            child.waitForTimeoutLines(50.milliseconds) shouldBe null
        } finally {
            child.kill()
        }
    }

    test("streamLines emits complete lines") {
        val lines = command("/bin/sh", "-c", "echo a; echo b; echo c").streamLines().toList()
        lines shouldBe listOf("a", "b", "c")
    }

    test("streamLines with no newline final") {
        command("/bin/echo", "no-newline").streamLines().toList() shouldBe listOf("no-newline")
    }

    test("streamErrLines emits stderr lines") {
        val lines = command("/bin/sh", "-c", "echo e1 >&2; echo e2 >&2").streamErrLines().toList()
        lines shouldBe listOf("e1", "e2")
    }

    test("streamLines convenience") {
        val lines = command("/bin/echo", "hello lines").streamLines().toList()
        lines.any { it.trim() == "hello lines" } shouldBe true
    }

    test("streamErrLines convenience") {
        val lines = command("/bin/sh", "-c", "echo err_line >&2").streamErrLines().toList()
        lines.any { it.trim() == "err_line" } shouldBe true
    }

    test("ChildProcess streamLines") {
        val child = command("/bin/echo", "child lines").spawn()
        child.streamLines().toList().any { it.trim() == "child lines" } shouldBe true
    }

    test("ChildProcess streamErrLines") {
        val child = command("/bin/sh", "-c", "echo cerr >&2").spawn()
        child.streamErrLines().toList().any { it.trim() == "cerr" } shouldBe true
    }

    test("combinedLines merges stdout and stderr") {
        val lines = command("/bin/sh", "-c", "echo out; echo err >&2").combinedLines().toList()
        lines shouldContainExactlyInAnyOrder listOf("out", "err")
    }

    test("combinedLines interleaves as data arrives") {
        val lines = command("/bin/sh", "-c", "echo s1; echo s2 >&2; echo s3").combinedLines().toList()
        lines shouldContainExactlyInAnyOrder listOf("s1", "s2", "s3")
    }

    test("ChildProcess combinedLines") {
        val child = command("/bin/sh", "-c", "echo a; echo b").spawn()
        child.combinedLines().toList() shouldBe listOf("a", "b")
    }

    test("stdoutBytes emits raw chunks") {
        val chunks = command("/bin/echo", "raw bytes").stdoutBytes().toList()
        val all = chunks.fold(ByteArray(0)) { a, b -> a + b }
        all.decodeToString().trim() shouldBe "raw bytes"
    }

    test("stderrBytes emits raw stderr") {
        val chunks = command("/bin/sh", "-c", "echo rerr >&2").stderrBytes().toList()
        val all = chunks.fold(ByteArray(0)) { a, b -> a + b }
        all.decodeToString().trim() shouldBe "rerr"
    }

    test("CommandBuilder stdoutBytes shorthand") {
        val chunks = command("/bin/echo", "short").stdoutBytes().toList()
        chunks.fold(ByteArray(0)) { a, b -> a + b }.decodeToString().trim() shouldBe "short"
    }

    test("CommandBuilder stderrBytes shorthand") {
        val chunks = command("/bin/sh", "-c", "echo serr >&2").stderrBytes().toList()
        chunks.fold(ByteArray(0)) { a, b -> a + b }.decodeToString().trim() shouldBe "serr"
    }

    test("ChildProcess stdoutBytes") {
        val child = command("/bin/echo", "proc bytes").spawn()
        val chunks = child.stdoutBytes().toList()
        chunks.fold(ByteArray(0)) { a, b -> a + b }.decodeToString().trim() shouldBe "proc bytes"
    }

    test("ChildProcess stderrBytes") {
        val child = command("/bin/sh", "-c", "echo perr >&2").spawn()
        val chunks = child.stderrBytes().toList()
        chunks.fold(ByteArray(0)) { a, b -> a + b }.decodeToString().trim() shouldBe "perr"
    }

    test("pipeTo connects echo to cat") {
        val result = command("/bin/echo", "piped hello").pipeTo(command("/bin/cat")).waitFor()
        result.exitCode shouldBe 0
        result.stdoutText.trim() shouldBe "piped hello"
    }

    test("pipeTo preserves exit code of destination") {
        val result = command("/bin/echo", "x").pipeTo(command("/bin/cat")).waitFor()
        result.exitCode shouldBe 0
    }

    test("Flow.stdoutLines splits chunks") {
        val lines = command("/bin/sh", "-c", "echo f1; echo f2; echo f3")
            .stream()
            .stdoutLines()
            .toList()
        lines shouldBe listOf("f1", "f2", "f3")
    }

    test("Flow.stderrLines splits stderr chunks") {
        val lines = command("/bin/sh", "-c", "echo fe1 >&2; echo fe2 >&2")
            .stream()
            .stderrLines()
            .toList()
        lines shouldBe listOf("fe1", "fe2")
    }

    test("Flow.stdoutBytes raw access") {
        val chunks = command("/bin/echo", "flow bytes")
            .stream()
            .stdoutBytes()
            .toList()
        chunks.fold(ByteArray(0)) { a, b -> a + b }.decodeToString().trim() shouldBe "flow bytes"
    }

    test("Flow.stderrBytes raw access") {
        val chunks = command("/bin/sh", "-c", "echo fberr >&2")
            .stream()
            .stderrBytes()
            .toList()
        chunks.fold(ByteArray(0)) { a, b -> a + b }.decodeToString().trim() shouldBe "fberr"
    }

    test("Flow.combinedLines merges stdout and stderr") {
        val lines = command("/bin/sh", "-c", "echo fcout; echo fcerr >&2")
            .stream()
            .combinedLines()
            .toList()
        lines shouldContainExactlyInAnyOrder listOf("fcout", "fcerr")
    }
})
