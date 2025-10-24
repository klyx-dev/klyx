package com.klyx

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

class CustomNavType<T>(
    private val serializer: KSerializer<T>,
    isNullableAllowed: Boolean,
    private val json: Json = Json,
    override val name: String = serializer.descriptor.serialName
) : NavType<T>(isNullableAllowed = isNullableAllowed) {

    override fun get(bundle: SavedState, key: String): T {
        val (containsKey, value) = bundle.read {
            contains(key) to getStringOrNull(key)
        }

        if (value == null) {
            if (isNullableAllowed) {
                @Suppress("UNCHECKED_CAST")
                return null as T
            }

            if (containsKey) {
                throw SerializationException("found null for a non-nullable NavType: $name")
            } else {
                throw SerializationException("Key $key not found for a non-nullable NavType: $name")
            }
        }

        val decodedValue = value.decodeURLPart()
        return json.decodeFromString(serializer, decodedValue)
    }

    override fun put(bundle: SavedState, key: String, value: T) {
        bundle.write { putString(key, serializeAsValue(value)) }
    }

    override fun parseValue(value: String): T {
        val decodedValue = value.decodeURLPart()
        return json.decodeFromString(serializer, decodedValue)
    }

    override fun serializeAsValue(value: T): String {
        val jsonString = json.encodeToString(serializer, value)
        return jsonString.encodeURLPathPart()
    }
}

fun CustomNavType(
    kType: KType,
    json: Json = Json,
    name: String? = null,
): CustomNavType<Any?> {
    val serializer = serializer(kType)
    return CustomNavType(
        serializer = serializer,
        isNullableAllowed = kType.isMarkedNullable,
        json = json,
        name = name ?: serializer.descriptor.serialName
    )
}

fun navTypeMap(
    vararg kTypes: KType,
    json: Json = Json,
): Map<KType, CustomNavType<*>> = kTypes.associateWith { kType -> CustomNavType(kType, json) }
