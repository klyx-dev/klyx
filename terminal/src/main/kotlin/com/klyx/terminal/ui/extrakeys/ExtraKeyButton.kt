package com.klyx.terminal.ui.extrakeys

import com.klyx.terminal.ui.extrakeys.ExtraKeyButton.Companion.KEY_DISPLAY_NAME
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * @constructor Initialize a [ExtraKeyButton].
 *
 * @param config The [JsonObject] containing the info to create the [ExtraKeyButton].
 * @param extraKeyDisplayMap The [ExtraKeysConstants.ExtraKeyDisplayMap] that defines the
 *                          display text mapping for the keys if a custom value is not defined by [KEY_DISPLAY_NAME].
 * @param extraKeyAliasMap The [ExtraKeysConstants.ExtraKeyDisplayMap] that defines the aliases for the actual key names.
 * @property popup The [ExtraKeyButton] containing the information of the popup button (triggered by swipe up).
 */
class ExtraKeyButton(
    config: JsonObject,
    extraKeyDisplayMap: Map<String, String>,
    extraKeyAliasMap: Map<String, String>,
    val popup: ExtraKeyButton? = null,
) {

    /**
     * If the key is a macro, i.e. a sequence of keys separated by space.
     */
    val isMacro: Boolean

    /**
     * The key that will be sent to the terminal, either a control character, like defined in
     * [ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS] (LEFT, RIGHT, PGUP...) or some text.
     */
    val key: String

    /**
     * The text that will be displayed on the button.
     */
    val display: String

    init {
        val keyFromConfig = config[KEY_KEY_NAME]?.jsonPrimitive?.contentOrNull
        val macroFromConfig = config[KEY_MACRO]?.jsonPrimitive?.contentOrNull

        val keys = if (keyFromConfig != null && macroFromConfig != null) {
            throw SerializationException("Both key and macro can't be set for the same key. key: \"$keyFromConfig\", macro: \"$macroFromConfig\"")
        } else if (keyFromConfig != null) {
            isMacro = false
            mutableListOf(keyFromConfig)
        } else if (macroFromConfig != null) {
            isMacro = true
            macroFromConfig.split(" ").toMutableList()
        } else {
            throw SerializationException("All keys have to specify either key or macro")
        }

        for (i in keys.indices) {
            keys[i] = replaceAlias(extraKeyAliasMap, keys[i])
        }

        key = keys.joinToString(" ")

        val displayFromConfig = config[KEY_DISPLAY_NAME]?.jsonPrimitive?.contentOrNull
        display = displayFromConfig ?: keys.joinToString(" ") { extraKeyDisplayMap.getOrElse(it) { it } }
    }

    companion object {
        /** The key name for the name of the extra key if using a dict to define the extra key. {key: name, ...} */
        const val KEY_KEY_NAME = "key"

        /** The key name for the macro value of the extra key if using a dict to define the extra key. {macro: value, ...} */
        const val KEY_MACRO = "macro"

        /** The key name for the alternate display name of the extra key if using a dict to define the extra key. {display: name, ...} */
        const val KEY_DISPLAY_NAME = "display"

        /** The key name for the nested dict to define popup extra key info if using a dict to define the extra key. {popup: {key: name, ...}, ...} */
        const val KEY_POPUP = "popup"

        fun replaceAlias(extraKeyAliasMap: Map<String, String>, key: String): String {
            return extraKeyAliasMap.getOrElse(key) { key }
        }
    }
}
