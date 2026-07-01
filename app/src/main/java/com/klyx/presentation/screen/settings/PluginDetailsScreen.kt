package com.klyx.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.util.openUrl
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.pluginsDir
import com.klyx.network.fetchBody
import com.klyx.plugin.PluginManager
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.viewmodel.PluginStoreViewModel
import com.klyx.presentation.viewmodel.StorePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

private const val CDN = PluginManager.CDN
private const val API = PluginManager.API
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class PluginMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: PluginAuthor,
    val minAppVersion: String,
    val maxAppVersion: String? = null,
    val downloadCount: Int = 0,
    val links: PluginLinks? = null,
)

@Serializable
private data class PluginAuthor(
    val name: String,
    val github: String? = null,
    val url: String? = null,
)

@Serializable
private data class PluginLinks(
    val source: String? = null,
    val issues: String? = null,
    val website: String? = null,
)

private suspend fun findMetaReadmeChangelog(id: String): Triple<PluginMetadata, String?, String?> =
    withContext(Dispatchers.IO) {
        val pluginDir = Paths.pluginsDir.resolve(id)

        val meta: PluginMetadata = try {
            fetchBody<PluginMetadata>("$CDN/$id/metadata.json")
        } catch (_: Exception) {
            json.decodeFromString(pluginDir.resolve("plugin.json").readText())
        }

        val readme: String? = try {
            fetchBody<String?>("$CDN/$id/readme.md")
        } catch (_: Exception) {
            runCatching { pluginDir.resolve("readme.md").readText() }.getOrNull()
        }

        val changelog: String? = try {
            fetchBody<String?>("$CDN/$id/changelog.md")
        } catch (_: Exception) {
            runCatching { pluginDir.resolve("changelog.md").readText() }.getOrNull()
        }

        Triple(meta, readme, changelog)
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PluginDetailsScreen(id: String) {
    val toastHostState = LocalToastHostState.current
    val navigator = LocalNavigator.current
    val storeViewModel: PluginStoreViewModel = koinViewModel()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val storeUiState by storeViewModel.uiState.collectAsStateWithLifecycle()

    var metadata by remember { mutableStateOf<PluginMetadata?>(null) }
    var readme by remember { mutableStateOf<String?>(null) }
    var changelog by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        try {
            val (meta, read, change) = findMetaReadmeChangelog(id)
            metadata = meta
            readme = read
            changelog = change
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    storeUiState.installState?.let {
        PluginInstallingDialog(
            plugin = it.plugin,
            progressText = it.message ?: "",
            progress = it.progress
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = metadata?.name ?: "Plugin Details",
                        //style = MaterialTheme.typography.headlineLarge,
                        //fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        shape = CircleShape,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                loading -> {
                    ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> Text(
                    text = "Failed to load plugin: $error",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    textAlign = TextAlign.Center
                )

                metadata != null -> {
                    val meta = metadata!!
                    var isInstalling by remember { mutableStateOf(storeUiState.installState != null) }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data("$CDN/$id/icon.png")
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "${meta.name} icon",
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                            placeholder = rememberVectorPainter(Icons.Outlined.Extension),
                                            error = rememberVectorPainter(Icons.Outlined.Extension),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = meta.name,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "by ${meta.author.name}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = meta.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "v${meta.version}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                        Text(
                                            text = "app ${meta.minAppVersion}${meta.maxAppVersion?.let { " – $it" } ?: "+"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                        if (meta.downloadCount > 0) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${meta.downloadCount} downloads",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    meta.links?.let { links ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            links.source?.let { LinkChip(label = "Source", url = it) }
                                            links.issues?.let { LinkChip(label = "Issues", url = it) }
                                            links.website?.let { LinkChip(label = "Website", url = it) }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    isInstalling = true
                                    storeViewModel.installPlugin(
                                        plugin = StorePlugin(
                                            id = meta.id,
                                            name = meta.name,
                                            description = meta.description,
                                            author = meta.author.name,
                                            version = meta.version,
                                            minAppVersion = meta.minAppVersion,
                                            maxAppVersion = meta.maxAppVersion,
                                            downloadCount = meta.downloadCount,
                                            iconUrl = "$CDN/$id/icon.png",
                                            downloadUrl = "$API/dl/$id/${meta.version}",
                                        )
                                    ) {
                                        isInstalling = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isInstalling
                            ) {
                                if (isInstalling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Installing...", style = MaterialTheme.typography.titleMedium)
                                } else {
                                    Icon(
                                        Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Install Plugin", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        if (readme != null) {
                            item {
                                SectionCard(title = "Readme", content = readme!!)
                            }
                        }

                        if (changelog != null) {
                            item {
                                SectionCard(title = "Changelog", content = changelog!!)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkChip(label: String, url: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { openUrl(url) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
