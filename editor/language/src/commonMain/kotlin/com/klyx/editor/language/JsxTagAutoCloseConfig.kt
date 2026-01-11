package com.klyx.editor.language

import kotlinx.serialization.Serializable

/**
 * The configuration for JSX tag auto-closing.
 *
 * @property openTagNodeName The name of the node for an opening tag
 * @property closeTagNodeName The name of the node for a closing tag
 * @property jsxElementNodeName The name of the node for a complete element with children for open and close tags
 * @property tagNameNodeName The name of the node found within both opening and closing
 *      tags that describes the tag name
 * @property tagNameNodeNameAlternates Alternate Node names for tag names.
 *      Specifically needed as TSX represents the name in `<Foo.Bar>`
 *      as `member_expression` rather than `identifier` as usual
 * @property erroneousCloseTagNodeName Some grammars are smart enough to detect a closing tag
 *      that is not valid i.e. doesn't match it's corresponding
 *      opening tag or does not have a corresponding opening tag.
 *      This should be set to the name of the node for invalid
 *      closing tags if the grammar contains such a node, otherwise
 *      detecting already closed tags will not work properly
 * @property erroneousCloseTagNameNodeName See above for [erroneousCloseTagNodeName] for details.
 *      This should be set if the node used for the tag name
 *      within erroneous closing tags is different from the
 *      normal tag name node name
 */
@Serializable
data class JsxTagAutoCloseConfig(
    val openTagNodeName: String,
    val closeTagNodeName: String,
    val jsxElementNodeName: String,
    val tagNameNodeName: String,
    val tagNameNodeNameAlternates: List<String> = emptyList(),
    val erroneousCloseTagNodeName: String? = null,
    val erroneousCloseTagNameNodeName: String? = null
)
