package com.klyx.plugin

import android.util.Log
import com.klyx.api.plugin.PluginDescriptor

internal fun readCompiledDescriptor(cls: Class<*>): PluginDescriptor? {
    return try {
        val companionField = cls.getDeclaredField("Companion").also { it.isAccessible = true }
        val companion = companionField.get(null) ?: return null

        val getter = companion.javaClass.getMethod("getDescriptor").also { it.isAccessible = true }
        getter.invoke(companion) as? PluginDescriptor
    } catch (_: NoSuchFieldException) {
        null
    } catch (_: NoSuchMethodException) {
        null
    } catch (t: Throwable) {
        Log.w("PluginManager", "Failed to read compiled descriptor from ${cls.name}", t)
        null
    }
}

internal fun verifyDescriptorIntegrity(cls: Class<*>, declaredInBundle: PluginDescriptor) {
    val compiled = readCompiledDescriptor(cls) ?: run {
        Log.i(
            "PluginManager",
            "Plugin '${declaredInBundle.id}' has no compiled descriptor, (built before @PluginManifest) trusting plugin.json as-is."
        )
        return
    }

    if (compiled.id != declaredInBundle.id || compiled.version != declaredInBundle.version) {
        throw PluginLoadException(
            "Plugin descriptor mismatch for '${declaredInBundle.entryClass}': " +
                    "plugin.json declares id='${declaredInBundle.id}' version='${declaredInBundle.version}', " +
                    "but the compiled class declares id='${compiled.id}' version='${compiled.version}'. " +
                    "The bundle may be corrupted, tampered with, or built from mismatched sources, " +
                    "rebuild and re-export it."
        )
    }
}
