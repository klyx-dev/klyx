package com.klyx.wasm

class WasmException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception()

fun WasmRuntimeException(e: WasmException) = WasmRuntimeException(e.message, e.cause)

class WasmRuntimeException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException()

data class ImportSignature(
    val module: String,
    val name: String,
    val params: List<String>,
    val results: List<String>
) {
    override fun toString(): String {
        return "$module :: $name(${params.joinToString(", ").lowercase()}) ->" +
                " ${if (results.isEmpty()) "null" else results.joinToString(", ")}"
    }
}

@Suppress("MaxLineLength")
fun parseImports(message: String): List<ImportSignature> {
    val regex = Regex(
        """Import\(moduleName=NameValue\(name=([^)]*)\), entityName=NameValue\(name=([^)]*)\), descriptor=Function\(.*?params=ResultType\(types=\[([^\]]*)\]\), results=ResultType\(types=\[([^\]]*)\]\)"""
    )

    return regex.findAll(message).map { match ->
        val module = match.groupValues[1].trim()
        val name = match.groupValues[2].trim()

        val params = match.groupValues[3]
            .split(", ")
            .filter { it.isNotBlank() }
            .map { extractType(it) }

        val results = match.groupValues[4]
            .split(", ")
            .filter { it.isNotBlank() }
            .map { extractType(it) }

        ImportSignature(module, name, params, results)
    }.toList()
}

fun extractType(typeStr: String): String = when {
    "numberType=" in typeStr -> typeStr.substringAfter("numberType=").substringBefore(")").trim()
    "Reference" in typeStr -> typeStr.substringAfterLast(".").substringBefore(")").trim()
    "VectorValue.V128" in typeStr -> "V128"
    else -> typeStr.trim()
}
