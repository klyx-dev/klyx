@file:UseSerializers(OkioPathSerializer::class)

package com.klyx.extension

import com.klyx.core.serializers.path.OkioPathSerializer
import com.klyx.editor.language.LanguageMatcher
import com.klyx.editor.language.LanguageName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import okio.Path

@Serializable
data class ExtensionIndex(
    val extensions: MutableMap<String, ExtensionIndexEntry> = mutableMapOf(),
    val themes: MutableMap<String, ExtensionIndexThemeEntry> = mutableMapOf(),
    val languages: MutableMap<LanguageName, ExtensionIndexLanguageEntry> = mutableMapOf(),
    val iconThemes: MutableMap<String, ExtensionIndexIconThemeEntry> = mutableMapOf()
)

@Serializable
data class ExtensionIndexEntry(val manifest: ExtensionManifest, val dev: Boolean)

@Serializable
data class ExtensionIndexThemeEntry(val extension: String, val path: Path) : Comparable<ExtensionIndexThemeEntry> {
    override fun compareTo(other: ExtensionIndexThemeEntry): Int {
        return compareValuesBy(this, other, { it.extension }, { it.path })
    }
}

@Serializable
data class ExtensionIndexIconThemeEntry(
    val extension: String,
    val path: Path
) : Comparable<ExtensionIndexIconThemeEntry> {
    override fun compareTo(other: ExtensionIndexIconThemeEntry): Int {
        return compareValuesBy(this, other, { it.extension }, { it.path })
    }
}

@Serializable
data class ExtensionIndexLanguageEntry(
    val extension: String,
    val path: Path,
    val matcher: LanguageMatcher,
    val hidden: Boolean,
    val grammar: String?
) : Comparable<ExtensionIndexLanguageEntry> {
    override fun compareTo(other: ExtensionIndexLanguageEntry): Int {
        return compareValuesBy(
            this,
            other,
            { it.extension },
            { it.path },
            { it.matcher },
            { it.hidden },
            { it.grammar }
        )
    }
}
