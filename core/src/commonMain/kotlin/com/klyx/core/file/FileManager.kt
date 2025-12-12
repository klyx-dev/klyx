package com.klyx.core.file

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitOpenFileSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.io.files.SystemFileSystem
import okio.FileSystem
import okio.SYSTEM

object FileManager {
    suspend inline fun openFilePicker(
        type: FileKitType = FileKitType.File(),
        title: String? = null,
        directory: KxFile? = null,
        dialogSettings: FileKitDialogSettings = FileKitDialogSettings.createDefault(),
        crossinline onPickFile: suspend (KxFile?) -> Unit
    ) {
        onPickFile(
            FileKit.openFilePicker(
                type = type,
                title = title,
                directory = directory?.toPlatformFile(),
                dialogSettings = dialogSettings
            )?.toKxFile()
        )
    }

    suspend inline fun <A, B> openFilePicker(
        type: FileKitType = FileKitType.File(),
        mode: FileKitMode<A, B>,
        title: String? = null,
        directory: KxFile? = null,
        dialogSettings: FileKitDialogSettings = FileKitDialogSettings.createDefault(),
        crossinline onResult: suspend (A) -> Unit
    ) {
        onResult(
            FileKit.openFilePicker(
                type = type,
                mode = mode,
                title = title,
                directory = directory?.toPlatformFile(),
                dialogSettings = dialogSettings
            )
        )
    }

    suspend inline fun openDirectoryPicker(
        title: String? = null,
        directory: KxFile? = null,
        dialogSettings: FileKitDialogSettings = FileKitDialogSettings.createDefault(),
        crossinline onPickDirectory: suspend (KxFile?) -> Unit
    ) {
        onPickDirectory(
            FileKit.openDirectoryPicker(
                title = title,
                directory = directory?.toPlatformFile(),
                dialogSettings = dialogSettings
            )?.toKxFile()
        )
    }

    suspend inline fun openFileSaver(
        suggestedName: String,
        extension: String? = null,
        directory: KxFile? = null,
        dialogSettings: FileKitDialogSettings = FileKitDialogSettings.createDefault(),
        crossinline onResult: suspend (KxFile?) -> Unit
    ) {
        onResult(
            FileKit.openFileSaver(
                suggestedName = suggestedName,
                extension = extension,
                directory = directory?.toPlatformFile(),
                dialogSettings = dialogSettings
            )?.toKxFile()
        )
    }

    fun openFileWithDefaultApplication(
        file: KxFile,
        openFileSettings: FileKitOpenFileSettings = FileKitOpenFileSettings.createDefault()
    ) {
        FileKit.openFileWithDefaultApplication(file.toPlatformFile(), openFileSettings)
    }
}
