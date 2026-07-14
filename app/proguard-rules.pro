-dontwarn com.sun.nio.file.ExtendedOpenOption
-dontwarn org.joni.**
-dontwarn com.github.luben.zstd.**

-keep class kotlin.UInt { *; }
-keep class kotlin.UShort { *; }
-keep class kotlin.Pair { *; }
-keep class kotlin.Triple { *; }

-keep class io.github.treesitter.ktreesitter.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class com.klyx.native.** { *; }

-keep @androidx.annotation.Keep class * { *; }

# Plugin API. must not be obfuscated (plugins reference by FQN at runtime)
-keep class com.klyx.api.** { *; }
-keep class com.klyx.data.fs.** { *; }
-keep class com.klyx.data.file.** { *; }
-keep class com.klyx.data.terminal.** { *; }
-keep class com.klyx.data.editor.** { *; }
-keep class com.klyx.data.diagnostics.** { *; }
-keep class com.klyx.event.** { *; }
-keep class com.klyx.platform.** { *; }
-keep class com.klyx.core.** { *; }

-keep class com.klyx.api.data.terminal.TerminalSessionBinder { *; }
-keep class com.klyx.api.data.terminal.TerminalSessionManager { *; }

-keepclassmembers enum * { *; }
-keepattributes *Annotation*

-keepattributes SourceFile,LineNumberTable
-dontobfuscate
