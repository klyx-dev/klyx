package com.klyx.presentation.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxIcons
import com.klyx.api.ui.theme.GoogleSansRounded

@Composable
fun WelcomeScreen(
    onNewFileClick: () -> Unit,
    onOpenProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
//        Box(
//            modifier = Modifier
//                .size(96.dp)
//                .background(
//                    color = MaterialTheme.colorScheme.primaryContainer,
//                    shape = CircleShape
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = KlyxIcons.Klyx,
//                contentDescription = null,
//                modifier = Modifier.size(48.dp),
//                tint = MaterialTheme.colorScheme.onPrimaryContainer
//            )
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "K L Y X",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 4.sp
        )

        Text(
            text = "The native mobile editor.",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = GoogleSansRounded,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

//        WelcomeActionCard(
//            title = "New File",
//            subtitle = "Create a blank document",
//            icon = Icons.AutoMirrored.Rounded.NoteAdd,
//            containerColor = MaterialTheme.colorScheme.primaryContainer,
//            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
//            onClick = onNewFileClick
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        WelcomeActionCard(
//            title = "Open Project",
//            subtitle = "Browse your file system",
//            icon = Icons.Rounded.FolderOpen,
//            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
//            contentColor = MaterialTheme.colorScheme.onSurface,
//            onClick = onOpenProjectClick
//        )
//
//        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Swipe from the left edge to open the file explorer.",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = GoogleSansRounded,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun WelcomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f)) // Subtle inner icon background
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = GoogleSansRounded,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
