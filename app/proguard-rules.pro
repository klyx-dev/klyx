-dontwarn com.sun.nio.file.ExtendedOpenOption
-dontwarn org.joni.**
-dontwarn com.github.luben.zstd.**
-dontwarn javax.management.**
-dontwarn org.bouncycastle.**

-keep class kotlin.* { *; }
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

-keep class com.klyx.** { *; }

-keepclassmembers enum * { *; }
-keepattributes *Annotation*

-keepattributes SourceFile,LineNumberTable
