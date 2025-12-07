package com.klyx.wasm

import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.error.ChasmError.DecodeError
import io.github.charlietap.chasm.embedding.error.ChasmError.ExecutionError
import io.github.charlietap.chasm.embedding.error.ChasmError.ValidationError
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.fold
import io.itsvks.anyhow.Err
import io.itsvks.anyhow.Ok
import io.itsvks.anyhow.Result

internal fun <V, E> ChasmResult<V, E>.asResult(): Result<V, WasmException> where E : ChasmError {
    return fold(::Ok) {
        when (it) {
            is DecodeError -> Err(WasmException(it.error))
            is ExecutionError -> Err(WasmException(it.error))
            is ValidationError -> Err(WasmException(it.error))
        }
    }
}
