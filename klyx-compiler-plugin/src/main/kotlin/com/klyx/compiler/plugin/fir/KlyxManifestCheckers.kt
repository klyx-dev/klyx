package com.klyx.compiler.plugin.fir

import com.klyx.compiler.plugin.KlyxPluginIds
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
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
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement

object KlyxManifestErrors : KtDiagnosticsContainer() {

    val INVALID_PLUGIN_ID by error1<KtElement, String>()
    val RESERVED_DESCRIPTOR_NAME by error0<KtElement>()
    val INVALID_SEMVER by error2<KtElement, String, String>()
    val NOT_A_KLYX_PLUGIN by error0<KtElement>()
    val MISSING_NO_ARG_CONSTRUCTOR by error0<KtElement>()
    val BLANK_NAME_FALLS_BACK_TO_ID by warning1<KtElement, String?>()
    val MISSING_PLUGIN_MANIFEST by error1<KtElement, Name>()
    val DUPLICATE_PLUGIN_MANIFEST by error1<KtElement, String>()
    val INVALID_ENTRY_CLASS_MODIFIERS by error1<KtElement, String>()
    val MIN_VERSION_EXCEEDS_MAX by error2<KtElement, String, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KlyxManifestDiagnosticRendererFactory()

    private class KlyxManifestDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {

        override val MAP by KtDiagnosticFactoryToRendererMap("PluginManifest") { map ->
            map.apply {
                put(
                    INVALID_PLUGIN_ID,
                    "PluginManifest.id must be reverse-DNS shaped, e.g. \"com.example.git-tools\" (got \"{0}\")",
                    rendererA = TO_STRING
                )
                put(
                    INVALID_SEMVER,
                    "PluginManifest.{0} must be valid semver, e.g. \"1.0.0\" (got \"{1}\")",
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
                    "'descriptor' is reserved on '@PluginManifest' companions. The compiler plugin generates it, rename your property."
                )
                put(
                    MISSING_PLUGIN_MANIFEST,
                    "Class ''{0}'' implements KlyxPlugin but is missing ''@PluginManifest(...)''. " +
                            "Every KlyxPlugin implementation must be annotated.",
                    rendererA = TO_STRING
                )
                put(
                    DUPLICATE_PLUGIN_MANIFEST,
                    "Only one class per module may be annotated '@PluginManifest'. Also found: {0}",
                    rendererA = TO_STRING
                )
                put(
                    INVALID_ENTRY_CLASS_MODIFIERS,
                    "'@PluginManifest' class must be a concrete, non-inner, non-local class (found: {0})",
                    rendererA = TO_STRING
                )
                put(
                    MIN_VERSION_EXCEEDS_MAX,
                    "PluginManifest.minAppVersion (\"{0}\") is greater than maxAppVersion (\"{1}\"). " +
                            "No app version could ever satisfy this plugin.",
                    rendererA = TO_STRING, rendererB = TO_STRING
                )

            }
        }
    }
}

internal val manifestPredicate: LookupPredicate = LookupPredicate.create {
    annotated(KlyxPluginIds.PLUGIN_MANIFEST_FQN)
}

class KlyxManifestCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker> = setOf(
            PluginManifestClassChecker,
            KlyxPluginImplementationChecker
        )
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(manifestPredicate)
    }
}

private object KlyxPluginImplementationChecker : FirClassChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val implementsKlyxPlugin = declaration.superConeTypes.any {
            it.classId == KlyxPluginIds.KLYX_PLUGIN_CLASS_ID
        }
        if (!implementsKlyxPlugin) return

        val hasManifest = declaration.annotations.any {
            it.resolvedType.classId == KlyxPluginIds.PLUGIN_MANIFEST_CLASS_ID
        }

        if (!hasManifest) {
            val source = declaration.source ?: return
            reporter.reportOn(source, KlyxManifestErrors.MISSING_PLUGIN_MANIFEST, declaration.classId.shortClassName)
        }
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
        val classSource = declaration.source ?: return

        fun sourceFor(argName: String) = args[Name.identifier(argName)]?.source
            ?: annotation.source
            ?: classSource

        val id = args.stringArg("id")
        val name = args.stringArg("name")
        val version = args.stringArg("version")
        val minAppVersion = args.stringArg("minAppVersion")
        val maxAppVersion = args.stringArg("maxAppVersion")

        if (id == null || !KlyxPluginIds.isValidPluginId(id)) {
            reporter.reportOn(sourceFor("id"), KlyxManifestErrors.INVALID_PLUGIN_ID, id.orEmpty())
        }

        listOfNotNull(
            version?.let { "version" to it },
            minAppVersion?.let { "minAppVersion" to it },
            maxAppVersion?.takeIf { it.isNotBlank() }?.let { "maxAppVersion" to it }
        ).forEach { (label, value) ->
            if (!KlyxPluginIds.isValidSemver(value)) {
                reporter.reportOn(sourceFor(label), KlyxManifestErrors.INVALID_SEMVER, label, value)
            }
        }

        if (
            minAppVersion != null &&
            !maxAppVersion.isNullOrBlank() &&
            KlyxPluginIds.isValidSemver(minAppVersion) &&
            KlyxPluginIds.isValidSemver(maxAppVersion)
        ) {
            if (compareSemver(minAppVersion, maxAppVersion) > 0) {
                reporter.reportOn(
                    sourceFor("minAppVersion"),
                    KlyxManifestErrors.MIN_VERSION_EXCEEDS_MAX,
                    minAppVersion,
                    maxAppVersion
                )
            }
        }

        if (name.isNullOrBlank()) {
            reporter.reportOn(sourceFor("name"), KlyxManifestErrors.BLANK_NAME_FALLS_BACK_TO_ID, id)
        }

        val implementsKlyxPlugin = declaration.superConeTypes.any {
            it.classId == KlyxPluginIds.KLYX_PLUGIN_CLASS_ID
        }

        if (!implementsKlyxPlugin) {
            reporter.reportOn(classSource, KlyxManifestErrors.NOT_A_KLYX_PLUGIN)
        }

        if (declaration is FirRegularClass) {
            val badModifiers = listOfNotNull(
                "abstract".takeIf { declaration.isAbstract },
                "inner".takeIf { declaration.isInner },
                "local".takeIf { declaration.isLocal }
            )
            if (badModifiers.isNotEmpty()) {
                reporter.reportOn(
                    classSource, KlyxManifestErrors.INVALID_ENTRY_CLASS_MODIFIERS,
                    badModifiers.joinToString(", ")
                )
            }

        }

        val ownDescriptor = declaration.declarations
            .filterIsInstance<FirRegularClass>()
            .firstOrNull { it.isCompanion }
            ?.declarations
            ?.filterIsInstance<FirCallableDeclaration>()
            ?.firstOrNull { it.symbol.callableId?.callableName == KlyxPluginIds.DESCRIPTOR_PROPERTY_NAME }

        if (ownDescriptor != null) {
            reporter.reportOn(ownDescriptor.source ?: classSource, KlyxManifestErrors.RESERVED_DESCRIPTOR_NAME)
        }

        val hasPublicNoArgConstructor = declaration.declarations
            .map { it.symbol }
            .filterIsInstance<FirConstructorSymbol>()
            .any { it.valueParameterSymbols.isEmpty() && it.visibility.isPublicAPI }

        if (!hasPublicNoArgConstructor) {
            reporter.reportOn(classSource, KlyxManifestErrors.MISSING_NO_ARG_CONSTRUCTOR)
        }

        val allManifestClasses = context.session.predicateBasedProvider
            .getSymbolsByPredicate(manifestPredicate)
            .filterIsInstance<FirRegularClassSymbol>()

        if (allManifestClasses.size > 1) {
            val others = allManifestClasses
                .filterNot { it.classId == declaration.symbol.classId }
                .joinToString(", ") { it.classId.asFqNameString() }
            reporter.reportOn(classSource, KlyxManifestErrors.DUPLICATE_PLUGIN_MANIFEST, others)
        }
    }
}

private fun compareSemver(a: String, b: String): Int {
    fun parts(v: String) = v.substringBefore('-').substringBefore('+')
        .split('.').map { it.toIntOrNull() ?: 0 }
    val (a1, b1) = parts(a) to parts(b)
    for (i in 0..2) {
        val cmp = (a1.getOrElse(i) { 0 }).compareTo(b1.getOrElse(i) { 0 })
        if (cmp != 0) return cmp
    }
    return 0
}

private fun Map<Name, FirExpression>.stringArg(name: String): String? {
    val expr = this[Name.identifier(name)] ?: return null
    return (expr as? FirLiteralExpression)?.value as? String
}
