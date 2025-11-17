package com.klyx.runner

import com.klyx.core.file.KxFile
import com.klyx.core.runner.CodeRunner

expect fun KxFile.runner(): CodeRunner

expect fun CodeRunner(): CodeRunner
