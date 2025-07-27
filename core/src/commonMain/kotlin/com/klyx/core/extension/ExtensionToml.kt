package com.klyx.core.extension

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * This is inspired by [zed](zed.dev) extension toml.
 *
 * Extension IDs and names should not contain `klyx` or `Klyx`, since they are all `Klyx` extensions.
 *
 * ```toml
 * id = "my-extension"
 * name = "My extension"
 * version = "0.0.1"
 * schema_version = 1
 * authors = ["Your Name <you@example.com>"]
 * description = "My cool extension"
 * repository = "https://github.com/your-name/my-klyx-extension"
 * ```
 */
@Serializable
data class ExtensionToml(
    val id: String,
    val name: String,
    val version: String = "0.0.1",
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    val authors: Array<String> = arrayOf(),
    val description: String = "",
    val repository: String = ""
) {
    companion object {
        fun from(toml: String): ExtensionToml {
            return Toml.decodeFromString(toml)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExtensionToml

        if (schemaVersion != other.schemaVersion) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (version != other.version) return false
        if (!authors.contentEquals(other.authors)) return false
        if (description != other.description) return false
        if (repository != other.repository) return false

        return true
    }

    override fun hashCode(): Int {
        var result = schemaVersion
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + authors.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + repository.hashCode()
        return result
    }
}
