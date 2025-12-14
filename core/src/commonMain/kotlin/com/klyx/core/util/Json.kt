package com.klyx.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun JsonElement.mergeWith(override: JsonElement) = mergeJsonValue(source = override, target = this)

private fun mergeJsonValue(source: JsonElement, target: JsonElement): JsonElement {
    return when (source) {
        is JsonObject if target is JsonObject -> {
            val merged = target.toMutableMap()

            for ((key, sourceValue) in source) {
                val targetValue = merged[key]
                merged[key] = if (targetValue != null) {
                    mergeJsonValue(sourceValue, targetValue)
                } else {
                    sourceValue
                }
            }

            JsonObject(merged)
        }

        is JsonArray if target is JsonArray -> JsonArray(target + source)
        else -> source
    }
}

fun emptyJsonObject() = JsonObject(emptyMap())
