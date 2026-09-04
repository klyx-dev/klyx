package com.klyx.presentation.components.filetree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klyx.R
import com.klyx.app.icons.DeleteOutline
import com.klyx.app.icons.FolderOpen
import com.klyx.i18n.strings
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.orSnap
import com.klyx.ui.theme.uiFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DrawerActionBar(
    onAddFolderClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTrashClick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = onAddFolderClick,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.add_folder),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            onClick = onSearchClick,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = strings.search,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (onTrashClick != null) {
            Surface(
                onClick = onTrashClick,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = strings.trash,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyState(
    isOpening: Boolean,
    isFullyClosed: Boolean,
    onOpenProjectClick: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current

    val heroScale = remember { Animatable(0.5f) }
    val heroAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(40f) }
    val buttonScale = remember { Animatable(0.8f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(isOpening, isFullyClosed) {
        if (isOpening) {
            launch { heroAlpha.animateTo(1f, tween<Float>(300).orSnap(reduceMotion)) }
            launch {
                heroScale.animateTo(
                    1f,
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ).orSnap(reduceMotion)
                )
            }

            delay(100.milliseconds)

            launch { textAlpha.animateTo(1f, tween<Float>(400).orSnap(reduceMotion)) }
            launch {
                textOffsetY.animateTo(
                    0f,
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ).orSnap(reduceMotion)
                )
            }

            delay(100.milliseconds)

            launch { buttonAlpha.animateTo(1f, tween<Float>(200).orSnap(reduceMotion)) }
            launch {
                buttonScale.animateTo(
                    1f,
                    spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ).orSnap(reduceMotion)
                )
            }
        } else if (isFullyClosed) {
            heroScale.snapTo(0.5f)
            heroAlpha.snapTo(0f)
            textAlpha.snapTo(0f)
            textOffsetY.snapTo(40f)
            buttonScale.snapTo(0.8f)
            buttonAlpha.snapTo(0f)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 20000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotationAngle"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = heroScale.value
                            scaleY = heroScale.value
                            alpha = heroAlpha.value
                        },
                    contentAlignment = Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                rotationZ = rotationAngle
                            }
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialShapes.Cookie12Sided.toShape()
                            )
                    )

                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = strings.folderIcon,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textOffsetY.value
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ready_to_code),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = uiFontFamily(),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.open_folder_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 360.dp)
                    )
                }

                OpenProjectButton(
                    onClick = onOpenProjectClick,
                    modifier = Modifier.graphicsLayer {
                        scaleX = buttonScale.value
                        scaleY = buttonScale.value
                        alpha = buttonAlpha.value
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OpenProjectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(buttonHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
    ) {
        Icon(
            painterResource(R.drawable.folder_open_24px),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonHeight)))
        Text(
            text = stringResource(R.string.open_project),
            style = ButtonDefaults.textStyleFor(buttonHeight),
            fontFamily = uiFontFamily(),
            fontWeight = FontWeight.Bold
        )
    }
}
