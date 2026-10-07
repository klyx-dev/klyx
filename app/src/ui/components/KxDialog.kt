package com.klyx.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDialog
import com.composeunstyled.theme.Theme
import com.klyx.ui.background as backgroundToken
import com.klyx.ui.bodySmall as bodySmallToken
import com.klyx.ui.border as borderToken
import com.klyx.ui.colors as colorsProp
import com.klyx.ui.surface as surfaceToken
import com.klyx.ui.textMuted as textMutedToken
import com.klyx.ui.textPrimary as textPrimaryToken
import com.klyx.ui.title as titleToken
import com.klyx.ui.typography as typographyProp

@Composable
fun KxDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    UnstyledDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        overlay = {
            Scrim(
                scrimColor = Theme[colorsProp][backgroundToken].copy(alpha = 0.7f),
                enter = fadeIn(),
                exit = fadeOut(),
            )
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            DialogPanel(
                modifier = modifier
                    .padding(20.dp)
                    .safeDrawingPadding()
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RectangleShape)
                    .background(Theme[colorsProp][surfaceToken])
                    .border(1.dp, Theme[colorsProp][borderToken], RectangleShape),
                paneTitle = title,
                enter = scaleIn(initialScale = 0.96f, transformOrigin = TransformOrigin.Center) + fadeIn(),
                exit = scaleOut(targetScale = 0.96f, transformOrigin = TransformOrigin.Center) + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp)) {
                        Text(
                            text = title,
                            style = Theme[typographyProp][titleToken],
                            color = Theme[colorsProp][textPrimaryToken],
                        )
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                style = Theme[typographyProp][bodySmallToken],
                                color = Theme[colorsProp][textMutedToken],
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        content()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}
