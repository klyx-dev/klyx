package com.klyx.core.io

/** Read permission. */
const val R_OK = 1 shl 0

/** Write permission. */
const val W_OK = 1 shl 1

/**
 * Permission to manage all files.
 * This corresponds to `MANAGE_EXTERNAL_STORAGE` on Android 11 (API level 30) and higher.
 */
const val MANAGE_ALL_FILES = 1 shl 2 // for MANAGE_EXTERNAL_STORAGE (Android 11+)

/** Execute permission. */
const val X_OK = 1 shl 3

const val ALL_PERMISSIONS = R_OK or W_OK or MANAGE_ALL_FILES or X_OK
