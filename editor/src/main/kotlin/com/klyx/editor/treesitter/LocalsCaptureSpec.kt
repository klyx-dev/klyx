package com.klyx.editor.treesitter

/**
 * [LocalsCaptureSpec] helps our parser to recognize local variables and generate different spans
 * to highlight them.
 *
 * @author Rosemoe
 */
open class LocalsCaptureSpec {

    companion object {
        val DEFAULT = LocalsCaptureSpec()
    }

    open fun isDefinitionValueCapture(captureName: String) = captureName == "local.definition-value"

    open fun isDefinitionCapture(captureName: String) = captureName == "local.definition"

    open fun isReferenceCapture(captureName: String) = captureName == "local.reference"

    open fun isScopeCapture(captureName: String) = captureName == "local.scope"

    /**
     * Usually, variables in a scope take effect after their declarations. This special scope
     * indicates that, all variables in this scope (but not in its sub-scope), take effect in this
     * scope, no matter where they are.
     * For example, class member fields.
     */
    open fun isMembersScopeCapture(captureName: String) = captureName == "local.scope.members"

}
