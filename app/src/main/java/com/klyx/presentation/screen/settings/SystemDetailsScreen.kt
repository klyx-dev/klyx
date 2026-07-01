package com.klyx.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.R
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxIcons
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.viewmodel.DiagnosticsViewModel
import com.klyx.ui.util.ImageVectorOrPainter
import com.klyx.ui.util.asImageVectorOrPainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemDetailsScreen(viewModel: DiagnosticsViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("System Diagnostics") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = navigator::navigateBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                DeviceInfoSection(deviceInfo = state.deviceInfo)
            }

            state.displayCapabilities?.let { display ->
                item {
                    CapabilitySection(
                        title = "Display & Rendering",
                        icon = painterResource(R.drawable.mobile_2_24px).asImageVectorOrPainter
                    ) {
                        InfoRow("Max Refresh Rate", "${display.refreshRate} Hz")
                        InfoRow("OpenGL Version", display.glEsVersion)
                        InfoRow("Supports Vulkan", if (display.supportsVulkan) "Yes" else "No")
                        InfoRow("Supports HDR", if (display.supportsHdr) "Yes" else "No")
                        InfoRow("Wide Color Gamut", if (display.wideColorGamut) "Yes" else "No")
                    }
                }
            }

            state.runtimeCapabilities?.let { runtime ->
                item {
                    CapabilitySection(
                        title = "Runtime & Memory",
                        icon = Icons.Rounded.Memory.asImageVectorOrPainter
                    ) {
                        InfoRow("Total Memory", "${runtime.totalMemoryMb} MB")
                        InfoRow("Low RAM Device", if (runtime.lowRamDevice) "Yes" else "No")
                        InfoRow("Large Heap", if (runtime.lowRamDevice) "Yes" else "No")
                        InfoRow("Runtime", runtime.runtimeAbi)
                    }
                }
            }

            state.storageCapabilities?.let { storage ->
                item {
                    CapabilitySection(
                        title = "Storage",
                        icon = Icons.Rounded.Storage.asImageVectorOrPainter
                    ) {
                        InfoRow("Available Space", "${storage.freeStorageGb} GB")
                        InfoRow(
                            "Max Recommended File Size",
                            "${storage.maxRecommendedFileSizeMb} MB"
                        )
                        InfoRow(
                            "Supports External Storage",
                            if (storage.supportsExternalStorage) "Yes" else "No"
                        )
                    }
                }
            }

            state.editorInfo?.let { editorInfo ->
                item {
                    CapabilitySection("Editor Info", KlyxIcons.Klyx.asImageVectorOrPainter) {
                        InfoRow("Sora Editor Version", editorInfo.editorVersion)
                        InfoRow("Compose Version", editorInfo.composeVersion)
                        InfoRow("TreeSitter Version", editorInfo.treeSitterVersion)
                        //InfoRow("Rendering Backend", editorInfo.renderingBackend)
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilitySection(
    title: String,
    icon: ImageVectorOrPainter,
    content: @Composable () -> Unit
) {
    val sectionShape = AbsoluteSmoothCornerShape(28.dp, 60)
    Card(
        shape = sectionShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = AbsoluteSmoothCornerShape(16.dp, 60),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ) {
                        icon.fold(
                            onVector = {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            },
                            onPainter = {
                                Icon(
                                    painter = it,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(12.dp))
//                HorizontalDivider(
//                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
//                )
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Surface(
        shape = AbsoluteSmoothCornerShape(16.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.44f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.56f)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun orderedDeviceInfoEntries(deviceInfo: Map<String, String>): ImmutableList<Pair<String, String>> {
    val preferredOrder = listOf(
        "Manufacturer",
        "Model",
        "Brand",
        "Device",
        "Android Version",
        "SDK Version",
        "Hardware"
    )
    val orderedEntries = mutableListOf<Pair<String, String>>()
    val seenKeys = mutableSetOf<String>()

    preferredOrder.forEach { key ->
        deviceInfo[key]?.let { value ->
            orderedEntries += key to value
            seenKeys += key
        }
    }

    deviceInfo.forEach { (key, value) ->
        if (key !in seenKeys) {
            orderedEntries += key to value
        }
    }
    return orderedEntries.toImmutableList()
}

@Composable
fun DeviceInfoSection(deviceInfo: ImmutableMap<String, String>) {
    val orderedEntries = remember(deviceInfo) { orderedDeviceInfoEntries(deviceInfo) }
    val heroEntries = orderedEntries.take(2)
    val detailEntries = orderedEntries.drop(2)
    val sectionShape = AbsoluteSmoothCornerShape(30.dp, 60)

    Card(
        shape = sectionShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.52f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = AbsoluteSmoothCornerShape(16.dp, 60),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Device Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                //HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
                if (heroEntries.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        heroEntries.forEach { (label, value) ->
                            DeviceInfoHeroTile(
                                label = label,
                                value = value,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (heroEntries.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                detailEntries.chunked(2).forEach { rowEntries ->
                    if (rowEntries.size == 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DeviceInfoStatTile(
                                label = rowEntries[0].first,
                                value = rowEntries[0].second,
                                modifier = Modifier.weight(1f)
                            )
                            DeviceInfoStatTile(
                                label = rowEntries[1].first,
                                value = rowEntries[1].second,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        DeviceInfoStatTile(
                            label = rowEntries[0].first,
                            value = rowEntries[0].second,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoHeroTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(22.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeviceInfoStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(14.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
