package com.klyx.lsp

import android.content.Context
import android.system.Os
import com.klyx.core.App
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val maxSourceBytes = 16 * 1024 * 1024
private val snapshotRoot get() = File(App.global.get<Context>().cacheDir, "termux-definitions")

internal actual fun termuxSnapshotSource(file: PlatformFile): PlatformFile? {
    val relative = termuxRelativePath(snapshotRoot.path, file.path) ?: return null
    return PlatformFile(File(termuxPrefix, relative))
}

private const val readSourceScript = $$"""
    exec 3<>/dev/tcp/127.0.0.1/$2 || exit 1
    printf '%s' "$3" >&3
    IFS= read -r -t 5 reply <&3 && [[ $reply == "$4" ]] || exit 1
    prefix=$("$PREFIX/bin/realpath" -e -- "$PREFIX") || exit 1
    target=$("$PREFIX/bin/realpath" -e -- "$PREFIX/$1") || exit 1
    [[ $target == "$prefix/"* && -f $target ]] || exit 1
    exec 4<"$target" || exit 1
    opened=$("$PREFIX/bin/readlink" -f -- /proc/self/fd/4) || exit 1
    [[ $opened == "$prefix/"* && -f /proc/self/fd/4 ]] || exit 1
    size=$("$PREFIX/bin/stat" -Lc %s -- /proc/self/fd/4) || exit 1
    (( size <= $5 )) || exit 1
    printf '%08x' "$size" >&3
    "$PREFIX/bin/head" -c "$size" <&4 >&3
""".trimIndent()

internal suspend fun readTermuxDefinition(relative: String): PlatformFile {
    check(hasRunCommandPermissionPlatform()) { "Grant Klyx the Termux Run Command permission to read library definitions." }
    val clientToken = UUID.randomUUID().toString()
    val serverToken = UUID.randomUUID().toString()

    val bytes = withContext(Dispatchers.IO) {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { listener ->
            listener.soTimeout = 10000
            val dispatched = runCommandInTermuxPlatform(
                command = "$termuxPrefix/bin/bash",
                arguments = listOf(
                    "-c", readSourceScript, "klyx-read-definition", relative, listener.localPort.toString(),
                    clientToken, serverToken, maxSourceBytes.toString()
                ),
                workDir = termuxPrefix,
                background = true,
            )
            check(dispatched) { "Termux could not start the library reader." }
            listener.accept().use { connection ->
                connection.soTimeout = 10000
                val input = DataInputStream(connection.getInputStream())

                fun readAscii(size: Int) = ByteArray(size).also(input::readFully).toString(Charsets.US_ASCII)
                check(readAscii(clientToken.length) == clientToken) { "Unexpected Termux reader connection." }

                currentCoroutineContext().ensureActive()
                connection.getOutputStream().write((serverToken + "\n").toByteArray(Charsets.US_ASCII))

                val size = try {
                    readAscii(8).toInt(16)
                } catch (e: IOException) {
                    throw IOException("Termux could not read this library file (16 MiB limit).", e)
                }
                require(size in 0..maxSourceBytes) { "Invalid Termux source size." }
                ByteArray(size).also(input::readFully)
            }
        }
    }

    currentCoroutineContext().ensureActive()
    require(0.toByte() !in bytes) { "This definition is a binary file." }

    val destination = File(snapshotRoot, relative)
    val parent = requireNotNull(destination.parentFile)
    check(parent.mkdirs() || parent.isDirectory) { "Cannot create the library cache." }

    val temp = withContext(Dispatchers.IO) {
        File.createTempFile(".source-", ".tmp", parent)
    }
    try {
        temp.writeBytes(bytes)
        check(temp.setReadOnly()) { "Cannot protect the cached library source." }
        Os.rename(temp.path, destination.path)
    } finally {
        temp.delete()
    }
    return PlatformFile(destination)
}
