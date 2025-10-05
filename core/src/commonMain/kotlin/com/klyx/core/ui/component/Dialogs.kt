package com.klyx.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.klyx.core.LocalFixedColorRoles
import com.klyx.core.res.Res
import com.klyx.core.res.got_it
import com.klyx.core.res.how_does_it_work
import org.jetbrains.compose.resources.stringResource

private val DialogVerticalPadding = PaddingValues(vertical = 24.dp)
private val IconPadding = PaddingValues(bottom = 16.dp)
private val DialogHorizontalPadding = PaddingValues(horizontal = 24.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)
private val TextPadding = PaddingValues(bottom = 24.dp)
private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 12.dp

@Composable
fun HelpDialog(
    text: String,
    onDismissRequest: () -> Unit = {},
    dismissButton: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit = {
        ConfirmButton(text = stringResource(resource = Res.string.got_it)) { onDismissRequest() }
    },
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(resource = Res.string.how_does_it_work)) },
        icon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
        text = { Text(text = text) },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KlyxDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit)?,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
    ) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .padding(DialogVerticalPadding)
                    .padding(horizontal = 12.dp)
            ) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            Modifier.padding(IconPadding)
                                .padding(DialogHorizontalPadding)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            icon()
                        }
                    }
                }
                title?.let {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        val textStyle =
                            MaterialTheme.typography.headlineSmall.copy(
                                textAlign = TextAlign.Center
                            )
                        ProvideTextStyle(textStyle) {
                            Box(
                                // Align the title to the center when an icon is present.
                                Modifier.padding(TitlePadding)
                                    .padding(DialogHorizontalPadding)
                                    .align(
                                        if (icon == null) {
                                            Alignment.Start
                                        } else {
                                            Alignment.CenterHorizontally
                                        }
                                    )
                            ) {
                                title()
                            }
                        }
                    }
                }
                text?.let {
                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                        val textStyle = MaterialTheme.typography.bodyMedium
                        ProvideTextStyle(textStyle) {
                            Box(
                                Modifier.weight(weight = 1f, fill = false)
                                    .padding(TextPadding)
                                    .align(Alignment.Start)
                            ) {
                                text()
                            }
                        }
                    }
                }
                Box(modifier = Modifier.align(Alignment.End).padding(DialogHorizontalPadding)) {
                    val textStyle = MaterialTheme.typography.labelLarge
                    ProvideTextStyle(value = textStyle) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(ButtonsMainAxisSpacing),
                            verticalArrangement = Arrangement.spacedBy(ButtonsCrossAxisSpacing),
                        ) {
                            dismissButton?.invoke()
                            confirmButton?.invoke()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KlyxDialogButtonVariant(
    modifier: Modifier = Modifier,
    shape: Shape = MiddleButtonShape,
    text: String,
    onClick: () -> Unit,
) {
    Box {
        Surface(
            modifier = modifier.clickable(onClick = onClick).fillMaxWidth().height(48.dp),
            color = LocalFixedColorRoles.current.secondaryFixed,
            shape = shape,
        ) {}

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = LocalFixedColorRoles.current.onSecondaryFixed,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

val TopButtonShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

val MiddleButtonShape = RoundedCornerShape(4.dp)

val BottomButtonShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlyxDialogVariant(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(modifier = Modifier.padding(DialogVerticalPadding)) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            Modifier.padding(IconPadding)
                                .padding(DialogHorizontalPadding)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            icon()
                        }
                    }
                }
                title?.let {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        val textStyle = MaterialTheme.typography.headlineSmall
                        ProvideTextStyle(textStyle.copy(textAlign = TextAlign.Center)) {
                            Box(
                                // Align the title to the center when an icon is present.
                                Modifier.padding(TitlePadding)
                                    .padding(DialogHorizontalPadding)
                                    .align(
                                        if (icon == null) {
                                            Alignment.Start
                                        } else {
                                            Alignment.CenterHorizontally
                                        }
                                    )
                            ) {
                                title()
                            }
                        }
                    }
                }
                text?.let {
                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                        val textStyle = MaterialTheme.typography.bodyMedium
                        ProvideTextStyle(textStyle) {
                            Box(
                                Modifier.weight(weight = 1f, fill = false)
                                    .padding(TextPadding)
                                    .align(Alignment.Start)
                            ) {
                                text()
                            }
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(DialogHorizontalPadding),
                ) {
                    buttons?.invoke()
                }
            }
        }
    }
}

@Composable
fun DialogSubtitle(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier =
            modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelLarge,
    )
}
