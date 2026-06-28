package com.klyx.api.data.diagnostics

data class DisplayCapabilities(
    val refreshRate: String,
    val glEsVersion: String,
    val supportsVulkan: Boolean,
    val supportsHdr: Boolean,
    val wideColorGamut: Boolean
)

data class RuntimeCapabilities(
    val totalMemoryMb: Long,
    val lowRamDevice: Boolean,
    val largeHeapEnabled: Boolean,
    val runtimeAbi: String
)

data class StorageCapabilities(
    val freeStorageGb: String,
    val maxRecommendedFileSizeMb: Long,
    val supportsExternalStorage: Boolean
)

data class EditorInfo(
    val editorVersion: String,
    val composeVersion: String,
    val treeSitterVersion: String,
    val renderingBackend: String
)
