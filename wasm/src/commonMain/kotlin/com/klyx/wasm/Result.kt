package com.klyx.wasm

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.error.ChasmError.DecodeError
import io.github.charlietap.chasm.embedding.error.ChasmError.ExecutionError
import io.github.charlietap.chasm.embedding.error.ChasmError.ValidationError
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.fold

internal fun <V, E> ChasmResult<V, E>.asResult(): Result<V, WasmException> where E : ChasmError {
    return fold(
        onSuccess = { Ok(it) },
        onError = {
            when (val e = it) {
                is DecodeError -> Err(WasmException(e.error))
                is ExecutionError -> Err(WasmException(e.error))
                is ValidationError -> Err(WasmException(e.error))
            }
        }
    )
}
