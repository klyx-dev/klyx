package com.klyx.core.app

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.uuid.Uuid

object IdentityManager : SynchronizedObject() {
    private const val KEY_DEVICE_ID = "publisher_device_id"
    private val settings by lazy { Settings() }

    val deviceId: String
        get() = synchronized(this) {
            var id = settings.getStringOrNull(KEY_DEVICE_ID)
            if (id == null) {
                id = Uuid.generateV7().toHexString()
                settings[KEY_DEVICE_ID] = id
            }
            id
        }
}
