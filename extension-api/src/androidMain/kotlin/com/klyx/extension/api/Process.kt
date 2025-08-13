@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.api

import com.klyx.pointer.Pointer
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmAny
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.readLoweredString
import com.klyx.wasm.type.HasWasmReader
import com.klyx.wasm.type.Option
import com.klyx.wasm.type.WasmInt
import com.klyx.wasm.type.WasmMemoryReader
import com.klyx.wasm.type.WasmString
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.collections.WasmList
import com.klyx.wasm.utils.writeInt32LE

@OptIn(ExperimentalWasmApi::class)
data class Command(
    val command: WasmString,
    val args: WasmList<WasmString>,
    val env: EnvVars
) : WasmAny {
    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("Command(")
            append("command=${command.toString(memory)}, ")
            append("args=${args.toString(memory)}, ")
            append("env=${env.toString(memory)}")
            append(")")
        }
    }

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        var currentOffset = offset
        command.writeToBuffer(buffer, currentOffset)
        currentOffset += command.sizeInBytes()

        args.writeToBuffer(buffer, currentOffset)
        currentOffset += args.sizeInBytes()

        env.writeToBuffer(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int {
        return command.sizeInBytes() +
                args.sizeInBytes() +
                env.sizeInBytes()
    }

    companion object : HasWasmReader<Command> {
        override val reader
            get() = object : WasmMemoryReader<Command> {
                val cmdReader = WasmString.reader
                val argsReader = WasmList.reader(WasmString.reader)
                val envReader = WasmList.reader(Tuple2.reader(WasmString.reader, WasmString.reader))

                override fun read(memory: WasmMemory, offset: Int): Command {
                    var currentOffset = offset

                    val cmd = cmdReader.read(memory, currentOffset)
                    currentOffset += cmdReader.elementSize

                    val arguments = argsReader.read(memory, currentOffset)
                    currentOffset += argsReader.elementSize

                    val environment = envReader.read(memory, currentOffset)
                    return Command(cmd, arguments, environment)
                }

                override val elementSize: Int
                    get() {
                        //error("Command has variable size; elementSize is not fixed")
                        return cmdReader.elementSize + argsReader.elementSize + envReader.elementSize
                    }
            }
    }
}

@OptIn(ExperimentalWasmApi::class)
fun WasmMemory.readCommandResult(pointer: Pointer) = readResult(
    pointer = pointer,
    readOk = Command.reader::read,
    readErr = WasmMemory::readLoweredString
)

data class Output(
    val status: Option<WasmInt>,
    val stdout: ByteArray,
    val stderr: ByteArray
) : WasmAny {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Output

        if (status != other.status) return false
        if (!stdout.contentEquals(other.stdout)) return false
        if (!stderr.contentEquals(other.stderr)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + stdout.contentHashCode()
        result = 31 * result + stderr.contentHashCode()
        return result
    }

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        var currentOffset = offset

        status.writeToBuffer(buffer, currentOffset)
        currentOffset += status.sizeInBytes()

        buffer.writeInt32LE(stdout.size, currentOffset)
        currentOffset += 4
        stdout.copyInto(buffer, currentOffset)
        currentOffset += stdout.size

        buffer.writeInt32LE(stderr.size, currentOffset)
        currentOffset += 4
        stderr.copyInto(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int {
        return status.sizeInBytes() +
                4 + stdout.size +
                4 + stderr.size
    }

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("Output(")
            append("status=${status.toString(memory)}, ")
            append("stdout=${stdout.contentToString()}, ")
            append("stderr=${stderr.contentToString()}")
            append(")")
        }
    }

    companion object : HasWasmReader<Output> {
        override val reader
            get() = object : WasmMemoryReader<Output> {
                private val statusReader = Option.reader(WasmInt.reader)

                override fun read(memory: WasmMemory, offset: Int) = run {
                    var currentOffset = offset

                    val statusValue = statusReader.read(memory, currentOffset)
                    currentOffset += statusReader.elementSize

                    val stdoutLen = memory.int32(currentOffset)
                    currentOffset += 4
                    val stdoutBytes = memory.readBytes(currentOffset, stdoutLen)
                    currentOffset += stdoutLen

                    val stderrLen = memory.int32(currentOffset)
                    currentOffset += 4
                    val stderrBytes = memory.readBytes(currentOffset, stderrLen)

                    Output(statusValue, stdoutBytes, stderrBytes)
                }

                override val elementSize: Int
                    get() = error("Output has variable size; elementSize is not fixed")
            }
    }
}
