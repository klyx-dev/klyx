package com.klyx.language.extension

import com.klyx.editor.language.LanguageRegistry
import com.klyx.extension.ExtensionHostProxy

fun initLanguageExtensions(
    hostProxy: ExtensionHostProxy,
    languageRegistry: LanguageRegistry
) {
    val proxy = LanguageServerRegistryProxy(languageRegistry)
    hostProxy.registerLanguageServerProxy(proxy)
    hostProxy.registerGrammarProxy(proxy)
    //hostProxy.registerLanguageProxy(proxy)
}
