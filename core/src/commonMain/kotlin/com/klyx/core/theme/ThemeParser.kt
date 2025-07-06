package com.klyx.core.theme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.serialization.json.Json

object ThemeParser {
    private val json = Json {
        isLenient = true
        prettyPrint = true
    }

    suspend fun parse(source: Source): Result<ThemeFile> = withContext(Dispatchers.IO) {
        try {
            Result.success(json.decodeFromString(source.readString()))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
