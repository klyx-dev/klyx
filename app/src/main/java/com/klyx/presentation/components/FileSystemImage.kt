package com.klyx.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.klyx.api.data.fs.FileSystem
import org.koin.compose.koinInject

private sealed interface SftpImageState {
    data object Loading : SftpImageState
    data class Success(val bitmap: Bitmap) : SftpImageState
    data object Error : SftpImageState
}

@Composable
fun FileSystemImage(
    uri: Uri,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    filterQuality: FilterQuality = FilterQuality.High,
) {
    if (uri.scheme == "sftp") {
        val fileSystem: FileSystem = koinInject()
        val state by produceState<SftpImageState>(SftpImageState.Loading, uri) {
            try {
                val bytes = fileSystem.inputStream(uri).use { it.readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                value = if (bitmap != null) SftpImageState.Success(bitmap) else SftpImageState.Error
            } catch (_: Exception) {
                value = SftpImageState.Error
            }
        }
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is SftpImageState.Success -> Image(
                    painter = BitmapPainter(s.bitmap.asImageBitmap()),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )

                is SftpImageState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )

                is SftpImageState.Error -> {}
            }
        }
    } else {
        AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            contentScale = contentScale,
            filterQuality = filterQuality,
            modifier = modifier
        )
    }
}