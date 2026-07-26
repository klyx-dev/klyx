package com.klyx.compiler.plugin.fir

import com.klyx.compiler.plugin.KlyxPluginIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement

object KlyxManifestErrors : KtDiagnosticsContainer() {

    val INVALID_PLUGIN_ID by error0<KtElement>()
    val RESERVED_DESCRIPTOR_NAME by error0<KtElement>()
    val INVALID_SEMVER by error2<KtElement, String, String>()
    val NOT_A_KLYX_PLUGIN by error0<KtElement>()
    val MISSING_NO_ARG_CONSTRUCTOR by error0<KtElement>()
    val BLANK_NAME_FALLS_BACK_TO_ID by warning1<KtElement, String?>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KlyxManifestDiagnosticRendererFactory()

    private class KlyxManifestDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {

        override val MAP by KtDiagnosticFactoryToRendererMap("PluginManifest") { map ->
            map.apply {
                put(INVALID_PLUGIN_ID, "PluginManifest.id must be reverse-DNS shaped, e.g. \"com.example.git-tools\"")
                put(
                    INVALID_SEMVER,
                    "PluginManifest.{0} must be valid semver, e.g. \"1.0.0\", got \"{1}\"",
                    rendererA = TO_STRING,
                    rendererB = TO_STRING
                )
                put(
                    BLANK_NAME_FALLS_BACK_TO_ID,
                    "PluginManifest.name is blank, the plugin''s display name will fall back to id (\"{0}\")",
                    rendererA = TO_STRING
                )
                put(NOT_A_KLYX_PLUGIN, "'@PluginManifest' can only annotate a class implementing KlyxPlugin")
                put(MISSING_NO_ARG_CONSTRUCTOR, "Plugin entry class must have a public no-argument constructor")
                put(
                    RESERVED_DESCRIPTOR_NAME,
                    "'descriptor' is reserved on '@PluginManifest' companions. The compiler plugin generates it. Rename your property."
                )
            }
        }
    }
}

class KlyxManifestCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker> = setOf(PluginManifestClassChecker)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(LookupPredicate.create { annotated(KlyxPluginIds.PLUGIN_MANIFEST_FQN) })
    }
}

@OptIn(DirectDeclarationsAccess::class)
private object PluginManifestClassChecker : FirClassChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val annotation = declaration.annotations.firstOrNull {
            it.resolvedType.classId == KlyxPluginIds.PLUGIN_MANIFEST_CLASS_ID
        } ?: return

        val args = annotation.argumentMapping.mapping
        val id = args.stringArg("id")
        val name = args.stringArg("name")
        val version = args.stringArg("version")
        val minAppVersion = args.stringArg("minAppVersion")
        val maxAppVersion = args.stringArg("maxAppVersion")

        val source = declaration.source ?: return

        if (id == null || !KlyxPluginIds.isValidPluginId(id)) {
            reporter.reportOn(source, KlyxManifestErrors.INVALID_PLUGIN_ID)
        }

        listOfNotNull(
            version?.let { "version" to it },
            minAppVersion?.let { "minAppVersion" to it },
            maxAppVersion?.takeIf { it.isNotBlank() }?.let { "maxAppVersion" to it }
        ).forEach { (label, value) ->
            if (!KlyxPluginIds.isValidSemver(value)) {
                reporter.reportOn(source, KlyxManifestErrors.INVALID_SEMVER, label, value)
            }
        }

        if (name.isNullOrBlank()) {
            reporter.reportOn(source, KlyxManifestErrors.BLANK_NAME_FALLS_BACK_TO_ID, id!!)
        }

        val implementsKlyxPlugin = declaration.superConeTypes.any {
            it.classId == KlyxPluginIds.KLYX_PLUGIN_CLASS_ID
        }

        if (!implementsKlyxPlugin) {
            reporter.reportOn(source, KlyxManifestErrors.NOT_A_KLYX_PLUGIN)
        }

        val ownDescriptor = declaration.declarations
            .filterIsInstance<FirRegularClass>()
            .firstOrNull { it.isCompanion }
            ?.declarations
            ?.any { (it as? FirCallableDeclaration)?.symbol?.callableId?.callableName == KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME }
            ?: false

        if (ownDescriptor) {
            reporter.reportOn(source, KlyxManifestErrors.RESERVED_DESCRIPTOR_NAME)
        }

        val hasPublicNoArgConstructor = declaration.declarations
            .map { it.symbol }
            .filterIsInstance<FirConstructorSymbol>()
            .any { it.valueParameterSymbols.isEmpty() && it.visibility.isPublicAPI }

        if (!hasPublicNoArgConstructor) {
            reporter.reportOn(source, KlyxManifestErrors.MISSING_NO_ARG_CONSTRUCTOR)
        }
    }
}

private fun Map<Name, FirExpression>.stringArg(name: String): String? {
    val expr = this[Name.identifier(name)] ?: return null
    return (expr as? FirLiteralExpression)?.value as? String
}
