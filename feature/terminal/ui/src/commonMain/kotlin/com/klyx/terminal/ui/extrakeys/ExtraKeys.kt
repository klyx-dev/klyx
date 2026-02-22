package com.klyx.terminal.ui.extrakeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ExtraKeys(
    extraKeysInfo: ExtraKeysInfo,
    client: ExtraKeysClient = rememberExtraKeysClient { },
    state: ExtraKeysState = rememberExtraKeysState(),
    modifier: Modifier = Modifier
) {
    val buttons = extraKeysInfo.matrix
    val rowCount = buttons.size
    if (rowCount == 0) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        for (row in buttons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (button in row) {
                    ExtraKeyButtonCell(
                        button = button,
                        state = state,
                        client = client,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraKeyButtonCell(
    button: ExtraKeyButton,
    state: ExtraKeysState,
    client: ExtraKeysClient,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val isSpecial = state.isSpecialButton(button.key)

    val specialButton = remember(button.key) {
        if (isSpecial) SpecialButton.valueOf(button.key) else null
    }
    val specialState by remember(specialButton) {
        derivedStateOf {
            specialButton?.let { state.specialButtons[it]?.value }
        }
    }

    var isPressed by remember { mutableStateOf(false) }
    var popupButton by remember { mutableStateOf<ExtraKeyButton?>(null) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressCount by remember { mutableIntStateOf(0) }

    val colorScheme = MaterialTheme.colorScheme

    fun effectiveTextColor() = when {
        isSpecial && specialState?.isActive == true -> colorScheme.onPrimary
        else -> colorScheme.onSurface
    }

    fun effectiveBgColor() = when {
        isSpecial && specialState?.isActive == true -> colorScheme.primary
        isPressed -> colorScheme.surfaceDim
        else -> Color.Transparent
    }

    fun doHapticIfNeeded(button: ExtraKeyButton) {
        if (client.performExtraKeyButtonHapticFeedback(button)) return
        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
    }

    suspend fun dispatchClick(button: ExtraKeyButton) {
        doHapticIfNeeded(button)
        client.onExtraKeyButtonClick(button)
    }

    fun cancelLongPress() {
        longPressJob?.cancel()
        longPressJob = null
    }

    fun onPress() {
        isPressed = true
        longPressCount = 0
        cancelLongPress()

        longPressJob = coroutineScope.launch {
            when {
                // repetitive key: fire repeatedly after timeout
                state.repetitiveKeys.contains(button.key) -> {
                    delay(state.longPressTimeoutMs)
                    while (isActive) {
                        longPressCount++
                        dispatchClick(button)
                        delay(state.longPressRepeatDelayMs)
                    }
                }

                // special button: lock after long hold
                isSpecial -> {
                    delay(state.longPressTimeoutMs)
                    if (isActive) {
                        doHapticIfNeeded(button)
                        state.onSpecialButtonLongPress(button.key)
                        longPressCount++
                    }
                }
            }
        }
    }

    fun onRelease(fromSwipePopup: Boolean) {
        isPressed = false
        cancelLongPress()

        when {
            fromSwipePopup -> {
                // popup key was triggered by swipe-up gesture
                popupButton?.let { popup ->
                    popupButton = null
                    coroutineScope.launch { dispatchClick(popup) }
                }
            }

            longPressCount == 0 -> {
                // normal tap
                if (isSpecial) {
                    doHapticIfNeeded(button)
                    state.onSpecialButtonClick(button.key)
                } else {
                    coroutineScope.launch { dispatchClick(button) }
                }
            }

            // else: repetitive key already fired, nothing to do on release
        }
        longPressCount = 0
    }

    Box(modifier = modifier) {
        Button(
            onClick = {/* handled via pointerInput */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = effectiveBgColor(),
                contentColor = effectiveTextColor()
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(button) {
                    var swipeStartY = 0f
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue

                            when (event.type) {
                                PointerEventType.Press -> {
                                    swipeStartY = change.position.y
                                    onPress()
                                    change.consume()
                                }

                                PointerEventType.Move -> {
                                    val dy = change.position.y - swipeStartY
                                    if (button.popup != null) {
                                        if (popupButton == null && dy < -40f) {
                                            // swipe up: show popup
                                            cancelLongPress()
                                            isPressed = false
                                            popupButton = button.popup
                                        } else if (popupButton != null && dy > 0f) {
                                            // swipe back down: dismiss popup
                                            isPressed = true
                                            popupButton = null
                                        }
                                    }
                                    change.consume()
                                }

                                PointerEventType.Release -> {
                                    onRelease(fromSwipePopup = popupButton != null)
                                    change.consume()
                                }

                                PointerEventType.Exit, PointerEventType.Unknown -> {
                                    isPressed = false
                                    cancelLongPress()
                                    longPressCount = 0
                                    popupButton = null
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            val label = if (state.buttonTextAllCaps) {
                button.display.uppercase()
            } else {
                button.display
            }

            Text(
                text = label,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // popup shown on swipe-up
        if (popupButton != null) {
            val popup = popupButton!!

            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -120)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .background(colorScheme.primary)
                ) {
                    Text(
                        text = if (state.buttonTextAllCaps) popup.display.uppercase() else popup.display,
                        color = colorScheme.onPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
