# klyx-api consumer rules
# keep all public API classes for plugin access via DexClassLoader
-keep class com.klyx.api.** { *; }
-keep class com.klyx.data.** { *; }
-keep class com.klyx.event.** { *; }
-keep class com.klyx.platform.** { *; }

-keep class kotlin.Metadata { *; }
