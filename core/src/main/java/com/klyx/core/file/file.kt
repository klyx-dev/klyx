package com.klyx.core.file

import java.io.File

typealias FileId = String

val File.id: FileId
    get() = "$name:${length()}:${lastModified()}"
