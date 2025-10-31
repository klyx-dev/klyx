package com.klyx.core.file

import com.klyx.core.Environment
import com.klyx.core.io.R_OK
import com.klyx.core.logging.logger
import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import io.github.irgaly.kfswatch.KfsEvent
import io.github.irgaly.kfswatch.KfsLogger
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemPathSeparator
import kotlinx.io.okio.asOkioSink
import kotlinx.io.okio.asOkioSource
import okio.Path.Companion.toPath

/**
 * Represents a file or directory in the file system.
 */
expect open class KxFile {
    val name: String
    val path: String
    val absolutePath: String
    val parent: String?
    val parentFile: KxFile?
    val exists: Boolean
    val canRead: Boolean
    val canWrite: Boolean
    val canExecute: Boolean
    val length: Long
    val lastModified: Long
    val extension: String
    val isHidden: Boolean
    val isFile: Boolean
    val isDirectory: Boolean

    fun mkdirs(): Boolean
    fun mkdir(): Boolean
    fun createNewFile(): Boolean
    fun delete(): Boolean
    fun deleteRecursively(): Boolean
    fun renameTo(dest: KxFile): Boolean
    fun setReadable(readable: Boolean, ownerOnly: Boolean = true): Boolean
    fun setWritable(writable: Boolean, ownerOnly: Boolean = true): Boolean
    fun setExecutable(executable: Boolean, ownerOnly: Boolean = true): Boolean

    fun list(): Array<String>?
    fun listFiles(): Array<KxFile>?
    fun listFiles(filter: (KxFile) -> Boolean): Array<KxFile>?

    fun readBytes(): ByteArray
    fun readText(charset: String = "UTF-8"): String
    fun writeBytes(bytes: ByteArray)
    fun writeText(text: String, charset: String = "UTF-8")
    fun readLines(charset: String = "UTF-8"): List<String>

    override fun toString(): String
}

expect fun KxFile.source(): RawSource
expect fun KxFile.sink(): RawSink

fun KxFile.isKlyxTempFile() = absolutePath == "/untitled" && name == "untitled"

fun KxFile.toKotlinxIoPath() = Path(absolutePath)
fun KxFile.toOkioPath() = absolutePath.toPath(true)

fun KxFile.okioSource() = source().asOkioSource()
fun KxFile.okioSink() = sink().asOkioSink()

fun okio.Path.toKxFile() = KxFile(toString())
fun Path.toKxFile() = KxFile(toString())

val KxFile.size get() = SystemFileSystem.metadataOrNull(this.toKotlinxIoPath())?.size

expect fun KxFile.mimeType(): String?

expect fun KxFile(path: String): KxFile
expect fun KxFile(parent: KxFile, child: String): KxFile
expect fun KxFile(parent: String, child: String): KxFile
expect fun KxFile(parent: KxFile, child: KxFile): KxFile

fun KxFile.deleteAndCreate() = run { delete(); createNewFile() }

fun KxFile.find(name: String): KxFile? = listFiles()?.find { it.name == name }

fun KxFile.resolve(relative: KxFile): KxFile {
    val baseName = this.toString()
    return if (baseName.isEmpty() || baseName.endsWith(SystemPathSeparator)) {
        KxFile(baseName + relative)
    } else {
        KxFile(baseName + SystemPathSeparator + relative)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun KxFile.resolve(relative: String): KxFile = resolve(KxFile(relative))

fun PlatformFile.toKxFile() = KxFile(absolutePath())
fun KxFile.toPlatformFile() = PlatformFile(absolutePath)

fun Collection<PlatformFile>.toKxFiles() = map { it.toKxFile() }

/**
 * Converts a string representing a file path to a KxFile object.
 *
 * @receiver The string representing the file path.
 * @return A KxFile object representing the file or directory at the specified path.
 */
fun String.toKxFile() = KxFile(this)

val KxFile.isHomeDirectory get() = this.absolutePath == Environment.DeviceHomeDir
fun KxFile.resolveName() = if (isHomeDirectory) "Home" else name

expect fun KxFile.isPermissionRequired(permissionFlags: Int = R_OK): Boolean

private fun KxFile.watcher(dispatcher: CoroutineDispatcher) = run {
    val logger = logger("$name-watcher")

    KfsDirectoryWatcher(
        scope = CoroutineScope(dispatcher),
        logger = object : KfsLogger {
            override fun debug(message: String) {
                logger.debug { message }
            }

            override fun error(message: String) {
                logger.error { message }
            }
        }
    )
}

context(scope: CoroutineScope)
fun KxFile.watchAndReload(
    coroutineDispatcher: CoroutineDispatcher,
    onReload: suspend () -> Unit
) {
    var oldContent = readText()
    val watcher = watcher(coroutineDispatcher)

    scope.launch {
        launch {
            watcher.onEventFlow.collect { (_, _, event) ->
                if (event == KfsEvent.Modify) {
                    val newContent = readText()
                    if (!isTextEqualTo(oldContent)) {
                        onReload()
                        oldContent = newContent
                    }
                }
            }
        }

        watcher.add(parentFile?.absolutePath ?: absolutePath)
    }
}

context(scope: CoroutineScope)
fun KxFile.watchExistence(
    coroutineDispatcher: CoroutineDispatcher,
    onChange: suspend () -> Unit
) {
    val watcher = watcher(coroutineDispatcher)

    scope.launch {
        launch {
            watcher.onEventFlow.collect { (_, _, event) ->
                if (event == KfsEvent.Delete || event == KfsEvent.Create) {
                    onChange()
                }
            }
        }

        watcher.add(parentFile?.absolutePath ?: absolutePath)
    }
}

context(scope: CoroutineScope)
fun KxFile.watchEvents(
    coroutineDispatcher: CoroutineDispatcher,
    onEvent: suspend (targetDirectory: String, path: String, event: KfsEvent) -> Unit
) {
    val watcher = watcher(coroutineDispatcher)

    scope.launch {
        launch {
            watcher.onEventFlow.collect { (targetDirectory, path, event) ->
                onEvent(targetDirectory, path, event)
            }
        }

        watcher.add(parentFile?.absolutePath ?: absolutePath)
    }
}
