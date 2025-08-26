@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import android.content.Context
import com.klyx.extension.api.Output
import com.klyx.terminal.localProcess
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.Err
import com.klyx.wasm.type.Ok
import com.klyx.wasm.type.Result
import com.klyx.wasm.type.Some
import com.klyx.wasm.type.asWasmU8Array
import com.klyx.wasm.type.list
import com.klyx.wasm.type.str
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.tuple2
import com.klyx.wasm.type.wasm
import com.klyx.wasm.type.wstr
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("FunctionName")
@HostModule("klyx:extension/process")
object Process : KoinComponent {
    private val context by inject<Context>()

    @HostFunction
    fun runCommand(
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

    private fun WasmMemory.internal_runCommand(
        command: String,
        args: list<str>,
        env: list<tuple2<str, str>>
    ): Result<Output, str> = runBlocking {
        val args = args.map { it.value }.toTypedArray()
        val env = env.associate { (k, v) -> k.value to v.value }

        try {
            with(context) {
                val processResult = localProcess(arrayOf(command, *args)) {
                    env(env)
                }.execute()

                Ok(
                    Output(
                        Some(processResult.exitCode.wasm),
                        processResult.output.asWasmU8Array(),
                        processResult.error.asWasmU8Array()
                    )
                )
            }
        } catch (e: Exception) {
            Err("$e".wstr)
        }
    }
}
