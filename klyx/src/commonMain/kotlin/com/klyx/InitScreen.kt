package com.klyx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.app.InitStepStatus
import com.klyx.core.app.Initialization
import com.klyx.core.app.InitializationState
import com.klyx.core.app.LocalApp
import com.klyx.core.app.LocalBuildInfo
import com.klyx.core.identityHashCode
import com.klyx.core.logging.KxLog
import com.klyx.core.logging.Level
import com.klyx.core.logging.Message
import com.klyx.icons.Icons
import com.klyx.icons.Klyx
import com.klyx.ui.theme.KlyxMono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HeaderBg = Color(0xFF0E0E0E)
private val DividerColor = Color(0xFF181818)
private val LogBg = Color(0xFF0B0B0B)
private val ProgressTrack = Color(0xFF161616)
private val ProgressFill = Color(0xFF4FC3F7)

private val ColTag = Color(0xFF595959)
private val ColV = Color(0xFF4A5568)
private val ColD = Color(0xFF4A7A9B)
private val ColI = Color(0xFF3D8B6E)
private val ColW = Color(0xFFA07A20)
private val ColE = Color(0xFFB03050)
private val ColMsgDefault = Color(0xFFA8A8A8)
private val ColError = Color(0xFFCF6679)

private const val MAX_LOG_LINES = 300

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InitScreen() {
    val initState by Initialization.state.collectAsStateWithLifecycle()
    val app = LocalApp.current

    var disclaimerDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        DisclaimerManager.init(app)
        disclaimerDone = true
    }

    if (!initState.isComplete || !disclaimerDone) {
        Splash(initState)
    } else {
        MainScreen()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Splash(initState: InitializationState) {
    val buildInfo = LocalBuildInfo.current
    val hasError = initState.error != null
    val currentLabel = if (hasError) "initialization failed" else initState.currentLabel

    val stepsDone = initState.steps.count { it.status == InitStepStatus.Done }
    val stepsTotal = initState.steps.size

    val messages = remember { mutableStateListOf<Message>() }
    LaunchedEffect(Unit) {
        KxLog.logFlow.collect { msg ->
            if (messages.size >= MAX_LOG_LINES) messages.removeAt(0)
            if (msg.tag.lowercase() == "klyx") {
                messages.add(msg)
            }
        }
    }

    val headerAlpha = remember { Animatable(0f) }
    val bodyAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { headerAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch {
            delay(250)
            bodyAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
    }

    val progress by animateFloatAsState(
        targetValue = initState.progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LogBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBg)
                .graphicsLayer { alpha = headerAlpha.value }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Image(
                    imageVector = Icons.Klyx,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp),
                )

                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "KLYX",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 5.sp,
                    fontFamily = KlyxMono
                )

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = buildString {
                        append(buildInfo.versionName)
                        buildInfo.gitCommit?.let { append("  ·  ${it.take(7)}") }
                    },
                    color = ColTag,
                    fontSize = 10.sp,
                    fontFamily = KlyxMono,
                    letterSpacing = 0.3.sp
                )
            }

            HorizontalDivider(color = DividerColor)
        }

        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(LogBg)
                .graphicsLayer { alpha = bodyAlpha.value }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                items(messages, key = { it.identityHashCode() }) { msg ->
                    LogLine(msg)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(LogBg, Color.Transparent)))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(HeaderBg)
                .border(Dp.Hairline, DividerColor, RoundedCornerShape(50.dp))
                .graphicsLayer { alpha = bodyAlpha.value }
        ) {
            //HorizontalDivider(color = DividerColor)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cursorAlpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        cursorAlpha.animateTo(0.08f, tween(500))
                        cursorAlpha.animateTo(1f, tween(500))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (hasError) ColError else ProgressFill,
                            shape = CircleShape,
                        )
                        .graphicsLayer { alpha = cursorAlpha.value },
                )
                Spacer(Modifier.width(10.dp))

                Text(
                    text = currentLabel.lowercase(),
                    color = if (hasError) ColError else ColMsgDefault,
                    fontSize = 11.sp,
                    fontFamily = KlyxMono,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.weight(1f),
                )

                if (stepsTotal > 0) {
                    Text(
                        text = "$stepsDone / $stepsTotal",
                        color = ColTag,
                        fontSize = 10.sp,
                        fontFamily = KlyxMono,
                        letterSpacing = 0.6.sp,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(ProgressTrack),
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (hasError) ColError else ProgressFill,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Square,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun LogLine(msg: Message) {
    val msgColor = when (msg.level) {
        Level.Verbose -> ColV
        Level.Debug -> ColD
        Level.Info -> ColI
        Level.Warning -> ColW
        Level.Error, Level.Assert -> ColE
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 1.dp),
    ) {
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildString {
                append(msg.message)
                msg.throwable?.message?.let { append("  -- $it") }
            },
            color = msgColor,
            fontSize = 10.sp,
            fontFamily = KlyxMono,
        )
    }
}
