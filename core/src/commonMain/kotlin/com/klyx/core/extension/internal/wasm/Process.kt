@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.core.extension.internal.wasm

import com.klyx.core.extension.util.readResult
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.HasWasmReader
import com.klyx.wasm.type.Option
import com.klyx.wasm.type.Vec
import com.klyx.wasm.type.WasmAny
import com.klyx.wasm.type.WasmInt
import com.klyx.wasm.type.WasmMemoryReader
import com.klyx.wasm.type.WasmType
import com.klyx.wasm.type.WasmUByte
import com.klyx.wasm.type.collections.WasmList
import com.klyx.wasm.type.int32
import com.klyx.wasm.type.list
import com.klyx.wasm.type.str
import com.klyx.wasm.type.tuple2
import com.klyx.wasm.type.u8

typealias EnvVars = list<tuple2<str, str>>

data class Command(
    val command: str,
    val args: list<str>,
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
                val cmdReader = str.reader
                val argsReader = list.reader(str.reader)
                val envReader = list.reader(tuple2.reader(str.reader, str.reader))

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

fun WasmMemory.readCommandResult(pointer: Int) = readResult(
    pointer = pointer,
    readOk = Command.reader::read,
    readErr = WasmMemory::readLengthPrefixedUtf8String
)

data class Output(
    val status: Option<int32>,
    val stdout: Vec<u8>,
    val stderr: Vec<u8>
) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        var currentOffset = offset
        status.writeToBuffer(buffer, currentOffset)
        currentOffset += status.sizeInBytes()
        stdout.writeToBuffer(buffer, currentOffset)
        currentOffset += stdout.sizeInBytes()
        stderr.writeToBuffer(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int {
        return status.sizeInBytes() + stdout.sizeInBytes() + stderr.sizeInBytes()
    }

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("Output(")
            append("status=${status.toString(memory)}, ")
            append("stdout=${stdout.toString(memory)}, ")
            append("stderr=${stderr.toString(memory)}")
            append(")")
        }
    }

    override fun createReader() = reader

    companion object : HasWasmReader<Output> {
        override val reader
            get() = object : WasmMemoryReader<Output> {
                private val statusReader = Option.reader(WasmInt.reader)
                private val stdoutReader = WasmList.reader(WasmUByte.reader)
                private val stderrReader = WasmList.reader(WasmUByte.reader)

                override fun read(memory: WasmMemory, offset: Int) = run {
                    var currentOffset = offset

                    val statusValue = statusReader.read(memory, currentOffset)
                    currentOffset += statusReader.elementSize

                    val stdoutValue = stdoutReader.read(memory, currentOffset)
                    currentOffset += stdoutReader.elementSize

                    val stderrValue = stderrReader.read(memory, currentOffset)

                    Output(statusValue, stdoutValue, stderrValue)
                }

                override val elementSize: Int
                    get() = error("`Output` has variable size; elementSize is not fixed")
            }
    }
}
