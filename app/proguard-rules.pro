-dontwarn com.sun.nio.file.ExtendedOpenOption
-dontwarn org.joni.**

-keep class kotlin.UInt { *; }
-keep class kotlin.UShort { *; }
-keep class kotlin.Pair { *; }
-keep class kotlin.Triple { *; }

-keep class io.github.treesitter.ktreesitter.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers enum * { *; }
-keepattributes *Annotation*

-keepattributes SourceFile,LineNumberTable
