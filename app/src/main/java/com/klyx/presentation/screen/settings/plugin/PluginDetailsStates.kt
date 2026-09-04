package com.klyx.presentation.screen.settings.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.fs.pluginsDir
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.app.icons.ErrorOutline
import com.klyx.i18n.strings
import com.klyx.network.fetchBody
import com.klyx.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val CDN = PluginManager.CDN
private val pluginJson = Json { ignoreUnknownKeys = true }

suspend fun fetchTextContent(id: String, fileName: String): String? =
    withContext(Dispatchers.IO) {
        val pluginDir = Paths.pluginsDir.resolve(id)
        val localFile = pluginDir.resolve(fileName)
        val localText = runCatching { localFile.readText().takeIf { it.isNotBlank() } }.getOrNull()
        localText ?: try {
            fetchBody<String?>("$CDN/$id/$fileName")
        } catch (_: Exception) {
            null
        }
    }

suspend fun fetchPluginDescriptor(id: String): PluginDescriptor? =
    withContext(Dispatchers.IO) {
        val pluginDir = Paths.pluginsDir.resolve(id)

        val localJson = pluginDir.resolve("plugin.json")
        if (localJson.exists()) {
            val local = runCatching {
                pluginJson.decodeFromString<PluginDescriptor>(localJson.readText())
            }.getOrNull()
            if (local != null) return@withContext local
        }

        for (fileName in listOf("metadata.json", "plugin.json")) {
            val fetched = runCatching {
                fetchBody<PluginDescriptor?>("$CDN/$id/$fileName")
            }.getOrNull()
            if (fetched != null) return@withContext fetched
        }
        null
    }

@Composable
fun SkeletonBar(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
}

@Composable
fun PluginDetailsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBar(modifier = Modifier.fillMaxWidth(0.5f), height = 22.dp)
        SkeletonBar(modifier = Modifier.fillMaxWidth())
        SkeletonBar(modifier = Modifier.fillMaxWidth())
        SkeletonBar(modifier = Modifier.fillMaxWidth(0.85f))
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBar(modifier = Modifier.fillMaxWidth(0.4f), height = 22.dp)
        SkeletonBar(modifier = Modifier.fillMaxWidth())
        SkeletonBar(modifier = Modifier.fillMaxWidth(0.7f))
    }
}

@Composable
fun PluginLoadError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = strings.couldntLoadPluginDetails,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = strings.checkConnectionAndRetry,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.retry)
        }
    }
}
