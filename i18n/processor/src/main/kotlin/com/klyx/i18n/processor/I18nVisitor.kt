package com.klyx.i18n.processor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

internal class I18nVisitor(
    private val declarations: MutableList<KSAnnotated>
) : KSVisitorVoid() {
    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
        declarations += property
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        declarations += classDeclaration
    }
}
