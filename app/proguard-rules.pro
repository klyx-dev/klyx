-dontwarn com.sun.nio.file.ExtendedOpenOption
-dontwarn org.joni.**
-dontwarn com.github.luben.zstd.**
-dontwarn javax.management.**
-dontwarn java.rmi.**
-dontwarn javax.security.auth.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.tomcat.jni.**
-dontwarn net.i2p.crypto.eddsa.**
-dontwarn org.bouncycastle.**

-keep class org.apache.sshd.** { *; }

-keep class kotlin.* { *; }
-keep class kotlin.UInt { *; }
-keep class kotlin.UShort { *; }
-keep class kotlin.Pair { *; }
-keep class kotlin.Triple { *; }

# LSPAny = JsonElement: plugins are compiled against kotlinx-serialization-json
# (compileOnly) and construct option objects at runtime from the host's classes.
# R8 prunes unused members, so keep the whole json package - including
# JsonObjectBuilder/JsonElementBuildersKt that only plugin code references -
# otherwise initializationOptions() fails with NoClassDefFoundError on release.
-keep class kotlinx.serialization.json.** { *; }

-keep class io.github.treesitter.ktreesitter.** { *; }

-keep class androidx.compose.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class com.klyx.native.** { *; }

-keep @androidx.annotation.Keep class * { *; }

-keep class com.klyx.** { *; }

-keepclassmembers enum * { *; }
-keepattributes *Annotation*

-keepattributes SourceFile,LineNumberTable
-dontobfuscate
