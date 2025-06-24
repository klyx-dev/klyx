package com.klyx.core.file

interface FileWrapper {
    val absolutePath: String
    val canonicalPath: String

    val name: String
    val path: String
    val mimeType: String?
    val parent: String?
    val parentFile: FileWrapper?
    val isFile: Boolean
    val isDirectory: Boolean
    val canRestoreFromPath: Boolean
    val id: FileId
    val length: Long
    val lastModified: Long

    fun canRead(): Boolean
    fun canWrite(): Boolean
    fun exists(): Boolean

    fun list(): Array<out String>?
    fun listFiles(): List<FileWrapper>?
    fun listFiles(filter: (FileWrapper) -> Boolean): List<FileWrapper>?
    fun listFiles(filter: (FileWrapper) -> Boolean, recursive: Boolean): List<FileWrapper>?

    fun readText(): String
    fun writeText(text: String): Boolean
}

val FileWrapper.extension get() = name.substringAfterLast(".", "")
val FileWrapper.nameWithoutExtension get() = name.substringBeforeLast(".")

fun FileWrapper.find(name: String): FileWrapper? {
    return listFiles()?.find { it.name == name }
}

fun FileWrapper.language() = when (val ext = extension.lowercase()) {
    "kt", "kts" -> "kotlin"
    "js" -> "javascript"
    "ts" -> "typescript"
    "py" -> "python"
    "rs" -> "rust"
    "cpp", "cc", "cxx" -> "cpp"
    "cs" -> "csharp"
    "htm" -> "html"
    "sh" -> "bash"
    else -> ext.takeIf {
        it in setOf(
            "java", "json", "xml", "html", "css", "swift", "go", "c", "bash"
        )
    } ?: "text"
}
