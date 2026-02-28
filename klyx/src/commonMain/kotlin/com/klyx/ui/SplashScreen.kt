package com.klyx.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.core.app.BuildInfo
import com.klyx.core.app.LocalBuildInfo
import com.klyx.core.logging.KxLog
import com.klyx.icons.Icons
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxOutlined
import com.klyx.ui.theme.KlyxMono

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplashScreen(progress: (() -> Float)? = null) {
    Surface(modifier = Modifier.fillMaxSize()) {

        val buildInfo = LocalBuildInfo.current
        var currentLabel by mutableStateOf("initializing...")
        LaunchedEffect(Unit) {
            KxLog.logFlow.collect { msg ->
                if (msg.tag.lowercase() == "klyx") {
                    currentLabel = msg.message.lowercase()
                }
            }
        }

        AdaptiveLayout(
            expandedLayout = {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        KlyxIcon(modifier = Modifier.size(200.dp))
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    ) {
                        LoadingIndicatorCard(
                            currentLabel = currentLabel,
                            progress = progress,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(maxWidth / 2f)
                        )

                        VersionInfoText(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 20.dp),
                            buildInfo = buildInfo
                        )
                    }
                }
            },
            compactLayout = {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        KlyxIcon(modifier = Modifier.size(200.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicatorCard(
                            currentLabel = currentLabel,
                            progress = progress,
                            modifier = Modifier.width(this@BoxWithConstraints.maxWidth / 1.2f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VersionInfoText(buildInfo = buildInfo)
                    }
                }
            }
        )
    }
}

@Composable
private fun VersionInfoText(
    buildInfo: BuildInfo,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = buildString {
            append(buildInfo.versionName)
            buildInfo.gitCommit?.let { append("  ·  ${it.take(7)}") }
        },
        color = MaterialTheme.colorScheme.inverseSurface,
        fontSize = 10.sp,
        fontFamily = KlyxMono,
        letterSpacing = 0.3.sp
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LoadingIndicatorCard(
    currentLabel: String,
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(50.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            ContainedLoadingIndicator()
            Spacer(modifier = Modifier.width(10.dp))
            Text(currentLabel.lowercase())
        }

        if (progress != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun KlyxIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box {
        Icon(
            imageVector = Icons.KlyxOutlined,
            contentDescription = null,
            modifier = modifier
        )

        Icon(
            imageVector = Icons.Klyx,
            contentDescription = null,
            modifier = modifier.graphicsLayer { this.alpha = alpha }
        )
    }
}
