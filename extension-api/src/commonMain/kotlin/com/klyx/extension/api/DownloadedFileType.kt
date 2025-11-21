package com.klyx.extension.api

import com.klyx.extension.api.DownloadedFileType.Uncompressed


enum class DownloadedFileType(val value: Int) {
    GZip(0),
    GZipTar(1),
    Zip(2),
    Uncompressed(3);

    companion object {
        fun fromValue(value: Int) = when (value) {
            0 -> GZip
            1 -> GZipTar
            2 -> Zip
            3 -> Uncompressed
            else -> throw IllegalArgumentException("Invalid value: $value")
        }
    }
}

fun DownloadedFileType.isExtractable() = this != Uncompressed

