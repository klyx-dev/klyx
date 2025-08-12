package com.klyx.extension.api

import com.klyx.pointer.Pointer
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.readLoweredString
import com.klyx.wasm.readStringList

data class Command(
    val command: String,
    val args: List<String>,
    val env: EnvVars
)

@OptIn(ExperimentalWasmApi::class)
fun WasmMemory.readCommandResult(pointer: Pointer) = readResult(
    pointer = pointer,
    readOk = { offset ->
        val cmd = readLoweredString(offset)
        val args = readStringList(offset + 8)

        val envPtr = uint32(offset + 16)
        val envLen = uint32(offset + 20)
        val env = (0u until envLen).map { i ->
            val tupleBase = envPtr + i * 16u
            val key = readLoweredString(tupleBase)
            val value = readLoweredString(tupleBase + 8u)
            key to value
        }

        Command(cmd, args, env)
    },
    readErr = WasmMemory::readLoweredString
)
