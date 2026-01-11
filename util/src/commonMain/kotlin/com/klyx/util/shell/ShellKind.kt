package com.klyx.util.shell

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmField

@Serializable
enum class ShellKind {
    Posix,
    Csh,
    Tcsh,
    Rc,
    Fish,

    /**
     * Pre-installed "legacy" powershell for windows
     */
    PowerShell,

    /**
     * PowerShell 7.x
     */
    Pwsh,
    Nushell,
    Cmd,
    Xonsh,
    Elvish;

    companion object {
        @JvmField
        val default = Posix
    }
}
