package com.klyx.core.file.archive

enum class ArchiveType {
    TarGz, Zip;

    override fun toString() = when (this) {
        TarGz -> "tar.gz"
        Zip -> "zip"
    }
}
