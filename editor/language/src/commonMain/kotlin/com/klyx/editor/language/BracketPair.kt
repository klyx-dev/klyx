package com.klyx.editor.language

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Describes a single bracket pair and how an editor should react to e.g. inserting
 * an opening bracket or to a newline character insertion in between `start` and `end` characters.
 *
 * @property start Starting substring for a bracket.
 * @property end Ending substring for a bracket.
 * @property close True if `end` should be automatically inserted right after `start` characters.
 * @property newline True if an extra newline should be inserted while the cursor is in the middle
 *                   of that bracket pair.
 * @property surround True if selected text should be surrounded by `start` and `end` characters.
 */
@Serializable
data class BracketPair(
    val start: String,
    val end: String,
    val close: Boolean,
    val newline: Boolean,
    val surround: Boolean = true,
)

/**
 * Configuration of handling bracket pairs for a given language.
 *
 * This class includes settings for defining which pairs of characters are considered brackets and
 * also specifies any language-specific scopes where these pairs should be ignored for bracket matching purposes.
 *
 * @property pairs A list of character pairs that should be treated as brackets in the context of a given language.
 * @property disabledScopesByBracketIx A list of tree-sitter scopes for which a given bracket should not be active.
 * N-th entry in [disabledScopesByBracketIx] contains a list of disabled scopes for an n-th entry in [pairs]
 */
@Serializable(BracketPairConfigSerializer::class)
data class BracketPairConfig(
    val pairs: List<BracketPair> = emptyList(),
    val disabledScopesByBracketIx: List<List<String>> = emptyList()
) {
    fun isClosingBrace(c: Char): Boolean = pairs.any { it.end.startsWith(c) }
}

@Serializable
internal data class BracketPairContent(
    val start: String,
    val end: String,
    val close: Boolean,
    val newline: Boolean,
    val surround: Boolean = true,
    val notIn: List<String> = emptyList()
)

internal object BracketPairConfigSerializer : KSerializer<BracketPairConfig> {
    override val descriptor: SerialDescriptor = ListSerializer(BracketPairContent.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: BracketPairConfig) {
        val contents = value.pairs.zip(value.disabledScopesByBracketIx) { pair, scopes ->
            BracketPairContent(
                start = pair.start,
                end = pair.end,
                close = pair.close,
                newline = pair.newline,
                surround = pair.surround,
                notIn = scopes
            )
        }

        encoder.encodeSerializableValue(
            ListSerializer(BracketPairContent.serializer()),
            contents
        )
    }

    override fun deserialize(decoder: Decoder): BracketPairConfig {
        val contents = decoder.decodeSerializableValue(ListSerializer(BracketPairContent.serializer()))
        val pairs = ArrayList<BracketPair>(contents.size)
        val disabled = ArrayList<List<String>>(contents.size)

        for (content in contents) {
            pairs += content.toBracketPair()
            disabled += content.notIn
        }

        return BracketPairConfig(pairs, disabled)
    }

    private fun BracketPairContent.toBracketPair(): BracketPair {
        return BracketPair(
            start = start,
            end = end,
            close = close,
            newline = newline,
            surround = surround
        )
    }
}
