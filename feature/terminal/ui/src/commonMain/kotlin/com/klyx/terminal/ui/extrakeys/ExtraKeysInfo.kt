package com.klyx.terminal.ui.extrakeys

import androidx.compose.runtime.Immutable
import com.klyx.terminal.ui.extrakeys.ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Immutable
class ExtraKeysInfo(
    propertiesInfo: String,
    extraKeyDisplayMap: Map<String, String>,
    extraKeyAliasMap: Map<String, String>
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowComments = true
        allowTrailingComma = true
        allowSpecialFloatingPointValues = true
    }

    /**
     * Matrix of buttons to be displayed in [ExtraKeys].
     */
    val matrix: List<List<ExtraKeyButton>>

    constructor(
        propertiesInfo: String,
        style: ExtraKeyStyle,
        extraKeyAliasMap: Map<String, String>
    ) : this(propertiesInfo, getCharDisplayMapForStyle(style), extraKeyAliasMap)

    init {
        matrix = initExtraKeysInfo(propertiesInfo, extraKeyDisplayMap, extraKeyAliasMap)
    }

    private fun initExtraKeysInfo(
        propertiesInfo: String,
        extraKeyDisplayMap: Map<String, String>,
        extraKeyAliasMap: Map<String, String>
    ): List<List<ExtraKeyButton>> {
        val matrix: List<List<JsonElement>> = json.decodeFromString(propertiesInfo)
        return matrix.map { row ->
            row.map { element ->
                val keyConfig = normalizeKeyConfig(element)

                if (!keyConfig.containsKey(ExtraKeyButton.KEY_POPUP)) {
                    ExtraKeyButton(keyConfig, extraKeyDisplayMap, extraKeyAliasMap)
                } else {
                    val popupJson = normalizeKeyConfig(keyConfig[ExtraKeyButton.KEY_POPUP]!!)

                    val popup = ExtraKeyButton(
                        config = popupJson,
                        extraKeyDisplayMap = extraKeyDisplayMap,
                        extraKeyAliasMap = extraKeyAliasMap
                    )

                    ExtraKeyButton(
                        config = keyConfig,
                        extraKeyDisplayMap = extraKeyDisplayMap,
                        extraKeyAliasMap = extraKeyAliasMap,
                        popup = popup
                    )
                }
            }
        }
    }

    private fun normalizeKeyConfig(element: JsonElement) = when (element) {
        is JsonPrimitive -> buildJsonObject {
            put(ExtraKeyButton.KEY_KEY_NAME, element.content)
        }

        is JsonObject -> element
        else -> error("A key in the extra-key matrix must be a string or an object")
    }

    companion object {
        fun getCharDisplayMapForStyle(style: ExtraKeyStyle) = when (style) {
            ExtraKeyStyle.ArrowsOnly -> EXTRA_KEY_DISPLAY_MAPS.ARROWS_ONLY_CHAR_DISPLAY
            ExtraKeyStyle.ArrowsAll -> EXTRA_KEY_DISPLAY_MAPS.LOTS_OF_ARROWS_CHAR_DISPLAY
            ExtraKeyStyle.All -> EXTRA_KEY_DISPLAY_MAPS.FULL_ISO_CHAR_DISPLAY
            ExtraKeyStyle.None -> emptyMap()
            ExtraKeyStyle.Default -> EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY
        }
    }
}
