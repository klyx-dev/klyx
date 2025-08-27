package com.klyx.ui.component.log

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerSheet(
    buffer: LogBuffer,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAsTabClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        dragHandle = null,
        sheetState = sheetState,
        sheetMaxWidth = with(density) { containerSize.width.toDp() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("LogViewer")
                    },
                    actions = {
                        TextButton(onClick = {
                            onOpenAsTabClick()

                            scope.launch { sheetState.hide() }
                        }) {
                            Text("Open as tab")
                        }
                    }
                )
            }
        ) { contentPadding ->
            LogViewer(
                buffer = buffer,
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            )
        }
    }
}
