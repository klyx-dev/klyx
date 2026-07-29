package com.klyx.compiler.plugin.fir

import com.klyx.compiler.plugin.KlyxPluginIds
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class KlyxDescriptorGenerationExtension(
    session: FirSession,
    private val messageCollector: MessageCollector
) : FirDeclarationGenerationExtension(session) {

    val provider = session.predicateBasedProvider

    private val manifestPredicate = LookupPredicate.create {
        annotated(KlyxPluginIds.PLUGIN_MANIFEST_FQN)
    }

    private fun info(message: String) {
        messageCollector.report(CompilerMessageSeverity.INFO, message)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(manifestPredicate)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext
    ): Set<Name> {
        if (!provider.matches(manifestPredicate, classSymbol)) return emptySet()
        if (classSymbol.hasCompanionObject()) return emptySet()
        return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val key = (classSymbol.origin as? FirDeclarationOrigin.Plugin)?.key
        if (key is Key) return setOf(KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME, SpecialNames.INIT)

        if (classSymbol.isCompanion) {
            val owner = classSymbol.getContainingClassSymbol() ?: return emptySet()
            if (provider.matches(manifestPredicate, owner) && !classSymbol.declaresOwnDescriptor()) {
                return setOf(KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME)
            }
        }

        return emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        if (!provider.matches(manifestPredicate, owner)) return null

        val klass = createCompanionObject(owner, Key)
        return klass.symbol
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        if (callableId.callableName != KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME) return emptyList()
        val owner = context?.owner ?: return emptyList()

        val property = createMemberProperty(
            owner = owner,
            key = Key,
            name = KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME,
            returnTypeProvider = { pluginDescriptorType() },
            isVal = true
        )
        return listOf(property.symbol)
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun FirClassSymbol<*>.hasCompanionObject(): Boolean =
        fir.declarations.filterIsInstance<FirRegularClass>().any { it.isCompanion }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun FirClassSymbol<*>.declaresOwnDescriptor() = fir.declarations.any { decl ->
        decl.origin !is FirDeclarationOrigin.Plugin &&
                (decl as? FirCallableDeclaration)?.symbol?.callableId?.callableName == KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner

        val key = (owner.origin as? FirDeclarationOrigin.Plugin)?.key
        if (key !is Key) return emptyList()

        val constructor = createConstructor(
            owner = owner,
            key = Key,
            isPrimary = true,
            generateDelegatedNoArgConstructorCall = true
        )
        return listOf(constructor.symbol)
    }

    private fun pluginDescriptorType() = ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(KlyxPluginIds.PLUGIN_DESCRIPTOR_CLASS_ID),
        typeArguments = emptyArray(),
        isMarkedNullable = false
    )

    object Key : GeneratedDeclarationKey()
}
