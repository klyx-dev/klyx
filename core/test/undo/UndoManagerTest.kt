package com.klyx.core.undo

import com.klyx.core.FakeOwner
import com.klyx.core.Owner
import com.klyx.core.newManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** A minimal reversible op over a StringBuilder, standing in for a real text edit. */
class AppendOp(
    private val sb: StringBuilder,
    private val text: String,
    override val owner: Owner? = null,
    override val coalescingKey: CoalescingKey? = null,
    var failExecute: Boolean = false,
) : UndoableOperation {
    override val label = "Append $text"
    override suspend fun execute() {
        if (failExecute) error("boom")
        sb.append(text)
    }

    override suspend fun undo() {
        sb.setLength(sb.length - text.length)
    }

    override fun mergeWith(next: UndoableOperation): UndoableOperation? {
        if (next !is AppendOp) return null
        return AppendOp(sb, text + next.text, owner, coalescingKey)
    }
}

class UndoManagerTest : FunSpec({
    test("basic undo redo") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "A"))
        m.perform(AppendOp(sb, "B"))
        m.perform(AppendOp(sb, "C"))

        sb.toString() shouldBe "ABC"

        m.undo().shouldBeTrue()
        sb.toString() shouldBe "AB"
        m.undo().shouldBeTrue()
        sb.toString() shouldBe "A"
        m.redo().shouldBeTrue()
        sb.toString() shouldBe "AB"
    }

    test("redo invalidated after new edit") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "A"))
        m.perform(AppendOp(sb, "B"))
        m.perform(AppendOp(sb, "C"))
        m.undo() // removes C

        m.canRedo.value.shouldBeTrue()

        m.perform(AppendOp(sb, "D"))
        m.canRedo.value.shouldBeFalse()

        sb.toString() shouldBe "ABD"
    }

    test("undo redo do not create history entries") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "A"))
        m.undo()
        m.redo()
        // A single entry the whole time: one more undo should empty the buffer, and a second undo is a no-op.

        m.undo().shouldBeTrue()
        sb.toString().shouldBeEmpty()
        m.undo().shouldBeFalse()
    }

    test("can undo, can redo, track state") {
        val m = newManager()
        m.canUndo.value.shouldBeFalse()
        m.canRedo.value.shouldBeFalse()

        val sb = StringBuilder()
        m.perform(AppendOp(sb, "A"))
        m.canUndo.value.shouldBeTrue()
        m.canRedo.value.shouldBeFalse()

        m.undo()

        m.canUndo.value.shouldBeFalse()
        m.canRedo.value.shouldBeTrue()
    }

    test("transaction groups into one undo step") {
        val sb = StringBuilder()
        val m = newManager()
        m.transaction("Format") {
            execute(AppendOp(sb, "1"))
            execute(AppendOp(sb, "2"))
            execute(AppendOp(sb, "3"))
        }

        sb.toString() shouldBe "123"
        m.undo().shouldBeTrue()
        sb.toString().shouldBeEmpty()
        m.undo().shouldBeFalse()
    }

    test("nested transactions flatten") {
        val sb = StringBuilder()
        val m = newManager()
        m.transaction("Outer") {
            execute(AppendOp(sb, "1"))
            transaction("Inner") {
                execute(AppendOp(sb, "2"))
                execute(AppendOp(sb, "3"))
            }
            execute(AppendOp(sb, "4"))
        }

        sb.toString() shouldBe "1234"
        m.undo().shouldBeTrue()
        sb.toString().shouldBeEmpty()
        m.undo().shouldBeFalse()
    }

    test("failed transaction rolls back and records nothing") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "base"))

        shouldThrow<IllegalStateException> {
            m.transaction("Bad") {
                execute(AppendOp(sb, "x"))
                execute(AppendOp(sb, "y", failExecute = true))
            }
        }

        sb.toString() shouldBe "base" // "x" rolled back
        m.canUndo.value.shouldBeTrue()
        m.undo()
        sb.toString().shouldBeEmpty() // only "base" was ever recorded
    }

    test("failed execute does not corrupt history") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "A"))
        shouldThrow<IllegalStateException> {
            m.perform(AppendOp(sb, "x", failExecute = true))
        }
        sb.toString() shouldBe "A"
        m.canUndo.value.shouldBeTrue()
        m.canRedo.value.shouldBeFalse()
        m.undo()
        sb.toString().shouldBeEmpty()
    }

    test("cancellation does not corrupt history") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "A"))
        val cancellingOp = object : UndoableOperation {
            override val label = "cancel-me"
            override suspend fun execute() {
                throw CancellationException("stop")
            }

            override suspend fun undo() {}
        }

        shouldThrow<CancellationException> {
            m.perform(cancellingOp)
        }
        sb.toString() shouldBe "A"
        undoDepth(m) shouldBe 1
    }

    test("coalescing merges adjacent typing") {
        val sb = StringBuilder()
        val m = newManager()
        val key = CoalescingKey("typing")
        m.perform(AppendOp(sb, "h", coalescingKey = key))
        m.perform(AppendOp(sb, "e", coalescingKey = key))
        m.perform(AppendOp(sb, "l", coalescingKey = key))
        m.perform(AppendOp(sb, "l", coalescingKey = key))
        m.perform(AppendOp(sb, "o", coalescingKey = key))
        "hello" shouldBe sb.toString()
        undoDepth(m) shouldBe 1
        m.undo()
        sb.toString().shouldBeEmpty()
    }

    test("coalescing respects time window") {
        val sb = StringBuilder()
        val m = newManager(coalescingWindowMs = 30)
        val key = CoalescingKey("typing")
        m.perform(AppendOp(sb, "h", coalescingKey = key))
        delay(80.milliseconds) // exceed the window
        m.perform(AppendOp(sb, "i", coalescingKey = key))
        undoDepth(m) shouldBe 2
    }

    test("different coalescing keys do not merge") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "a", coalescingKey = CoalescingKey("typing")))
        m.perform(AppendOp(sb, "b", coalescingKey = CoalescingKey("paste")))
        undoDepth(m) shouldBe 2
    }

    test("bounded history drops oldest") {
        val sb = StringBuilder()
        val m = newManager(maxEntries = 3)
        repeat(5) { m.perform(AppendOp(sb, "$it")) }
        undoDepth(m) shouldBe 3
        // Undo everything reachable; the two oldest ("0","1") were evicted and are not reachable.
        repeat(3) { m.undo() }
        "01" shouldBe sb.toString()
        m.undo().shouldBeFalse()
    }

    test("explicit clear drops history") {
        val sb = StringBuilder()
        val m = newManager()
        m.perform(AppendOp(sb, "A"))
        m.clear()
        m.canUndo.value.shouldBeFalse()
        m.canRedo.value.shouldBeFalse()
    }

    test("dispose rejects further use") {
        val m = newManager()
        m.dispose()
        shouldThrow<HistoryDisposedException> {
            m.perform(AppendOp(StringBuilder(), "A"))
        }
    }

    test("owner invalidation drops owned and newer entries") {
        val sb = StringBuilder()
        val m = newManager()
        val core = FakeOwner("core")
        val plugin = FakeOwner("plugin")
        m.perform(AppendOp(sb, "1", owner = core))
        m.perform(AppendOp(sb, "2", owner = plugin))
        m.perform(AppendOp(sb, "3", owner = core)) // newer than the plugin entry
        m.invalidateOwner(plugin)
        // "2" (plugin) and everything above it ("3") must go; "1" survives.
        undoDepth(m) shouldBe 1
        m.undo()
        sb.toString().shouldBeEmpty()
    }

    test("concurrent dispatch is serialized") {
        val sb = StringBuilder()
        val m = newManager()
        val jobs = (1..50).map {
            launch { m.perform(AppendOp(sb, "x")) }
        }
        jobs.joinAll()
        sb.length shouldBe 50
        undoDepth(m) shouldBe 50
    }

    test("document specific histories are independent") {
        val sbA = StringBuilder()
        val sbB = StringBuilder()
        val mA = newManager()
        val mB = newManager()
        mA.perform(AppendOp(sbA, "A1"))
        mB.perform(AppendOp(sbB, "B1"))
        mA.undo()
        sbA.toString().shouldBeEmpty()
        "B1" shouldBe sbB.toString() // untouched
    }
})

private suspend fun undoDepth(m: UndoManager): Int {
    var n = 0
    while (m.canUndo.value) {
        m.undo()
        n++
    }
    repeat(n) { m.redo() }
    return n
}
