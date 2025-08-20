package com.klyx.core.file

import java.io.File
import java.nio.file.Path

fun File.toKxFile(): KxFile = KxFile(absolutePath)

fun KxFile.rawFile(): File = File(absolutePath)
fun KxFile.toPath(): Path = rawFile().toPath()
fun Path.toKxFile() = toFile().toKxFile()

actual fun KxFile(parent: KxFile, child: String) = File(parent.absolutePath, child).toKxFile()
actual fun KxFile(parent: String, child: String) = File(parent, child).toKxFile()
actual fun KxFile(parent: KxFile, child: KxFile) = File(parent.absolutePath, parent.name).toKxFile()
