package com.klyx.lsp.capabilities

import com.klyx.lsp.MarkupKind
import kotlinx.serialization.Serializable

/**
 * The client supports the following `SignatureInformation`
 * specific properties.
 */
@Serializable
data class SignatureInformationCapabilities(
    /**
     * Client supports the following content formats for the documentation
     * property. The order describes the preferred format of the client.
     */
    var documentationFormat: List<MarkupKind>? = null,

    /**
     * Client capabilities specific to parameter information.
     */
    var parameterInformation: ParameterInformationCapabilities? = null,

    /**
     * The client supports the `activeParameter` property on
     * `SignatureInformation` literal.
     *
     * @since 3.16.0
     */
    var activeParameterSupport: Boolean? = null,

    /**
     * The client supports the `activeParameter` property on
     * `SignatureHelp`/`SignatureInformation` being set to `null` to
     * indicate that no parameter should be active.
     *
     * @since 3.18.0
     */
    var noActiveParameterSupport: Boolean? = null
)

/**
 * Client capabilities specific to parameter information.
 */
@Serializable
data class ParameterInformationCapabilities(
    /**
     * The client supports processing label offsets instead of a
     * simple label string.
     *
     * @since 3.14.0
     */
    var labelOffsetSupport: Boolean? = null
)
