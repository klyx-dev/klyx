@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.klyx.core.process.systemProcess
import com.klyx.extension.api.Output
import com.klyx.extension.internal.toWasmOutput
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.Err
import com.klyx.wasm.type.Ok
import com.klyx.wasm.type.Result
import com.klyx.wasm.type.list
import com.klyx.wasm.type.str
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.tuple2
import com.klyx.wasm.type.wstr
import org.koin.core.component.KoinComponent

@Suppress("FunctionName")
@HostModule("klyx:extension/process")
object Process : KoinComponent {
    @HostFunction
    suspend fun runCommand(
        memory: WasmMemory,
        command: String,
        argsPtr: Int,
        argsLen: Int,
        envPtr: Int,
        envLen: Int,
        returnPtr: Int
    ) {
        val argsReader = list.reader(str.reader)
        val envReader = list.reader(tuple2.reader(str.reader, str.reader))

        val args = argsReader.read(memory, argsPtr, argsLen)
        val env = envReader.read(memory, envPtr, envLen)

        with(memory) {
            val result = internal_runCommand(command, args, env)
            write(returnPtr, result.toBuffer())
        }
    }

    private suspend fun WasmMemory.internal_runCommand(
        command: String,
        args: list<str>,
        env: list<tuple2<str, str>>
    ): Result<Output, str> {
        val args = args.map { it.value }
        val env = env.associate { (k, v) -> k.value to v.value }

        return try {
            val output = systemProcess(command, *args.toTypedArray()) {
                environment { putAll(env) }
            }.output().let { output ->
                com.klyx.extension.internal.Output(
                    output.processInfo.exitCode,
                    output.stdout,
                    output.stderr
                )
            }
            Ok(output.toWasmOutput())
        } catch (e: Exception) {
            Err("$e".wstr)
        }
    }
}
