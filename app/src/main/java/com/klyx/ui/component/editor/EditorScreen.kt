package com.klyx.ui.component.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.klyx.core.file.FileWrapper
import com.klyx.core.file.KWatchEvent
import com.klyx.core.file.asWatchChannel
import com.klyx.core.file.requiresPermission
import com.klyx.core.hasStoragePermission
import com.klyx.core.rememberTypeface
import com.klyx.core.requestStoragePermission
import com.klyx.editor.compose.KlyxCodeEditor
import com.klyx.editor.compose.LocalEditorViewModel
import com.klyx.editor.language.language
import com.klyx.ui.component.KlyxDialog
import com.klyx.viewmodel.isFileTab
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun EditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = LocalEditorViewModel.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val state by viewModel.state.collectAsState()
    val openTabs by remember { derivedStateOf { state.openTabs } }
    val activeTabId by remember { derivedStateOf { state.activeTabId } }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { openTabs.size })
    val scope = rememberCoroutineScope()
    val typeface by rememberTypeface("IBM Plex Mono")

    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingFile: FileWrapper? by remember { mutableStateOf(null) }
    var permissionGranted by rememberSaveable { mutableStateOf(context.hasStoragePermission()) }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasStoragePermission()
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            pendingFile?.let {
                viewModel.openFile(it)
                pendingFile = null
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { openTabs }.collect { tabs ->
            tabs.filter { it.editorState != null && it.data != null }.fastForEach {
                val editorState = it.editorState!!
                val file = (it.data as? FileWrapper)?.asRawFile() ?: return@fastForEach

//                file.asWatchChannel().consumeEach { event ->
//                    when (event.kind) {
//                        KWatchEvent.Kind.Initialized -> {}
//                        KWatchEvent.Kind.Created -> {}
//                        KWatchEvent.Kind.Modified -> {
//                            editorState.updateText(event.file.readText())
//                            editorState.markAsSaved()
//                        }
//
//                        KWatchEvent.Kind.Deleted -> {
//                            viewModel.closeTab(it.id)
//                        }
//                    }
//                }
            }
        }
    }

    LaunchedEffect(pagerState, openTabs.size) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            openTabs.getOrNull(page)?.let { tab ->
                viewModel.setActiveTab(tab.id)
            }
        }
    }

    LaunchedEffect(activeTabId, pagerState) {
        val index = openTabs.indexOfFirst { it.id == activeTabId }
        if (index != -1 && index != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    Column(modifier = modifier) {
        if (openTabs.isNotEmpty()) {
            EditorTabBar(
                tabs = openTabs,
                selectedTab = pagerState.currentPage,
                onTabSelected = { page ->
                    keyboardController?.hide()

                    openTabs.getOrNull(page)?.let { tab ->
                        viewModel.setActiveTab(tab.id)
                        scope.launch { pagerState.animateScrollToPage(page) }
                    }
                },
                onClose = { index ->
                    openTabs.getOrNull(index)?.let { tab ->
                        viewModel.closeTab(tab.id)
                    }
                },
                isDirty = { index ->
                    val tab = openTabs.getOrNull(index)
                    tab?.editorState?.isModified == true
                }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) { page ->
                val tab = openTabs.getOrNull(page)

                when {
                    tab?.isFileTab == true -> {
                        val file = tab.data as? FileWrapper
                        if (file != null && tab.editorState != null) {
                            val editorState = tab.editorState!!

                            if (file.path != "untitled") {
                                LaunchedEffect(Unit) {
                                    if (file.requiresPermission(context, isWrite = true)) {
                                        viewModel.closeTab(tab.id)
                                        pendingFile = file
                                        showPermissionDialog = true
                                        return@LaunchedEffect
                                    }
                                }
                            }

                            KlyxCodeEditor(
                                editorState = editorState,
                                typeface = typeface,
                                editable = tab.type != "fileInternal",
                                language = file.language()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Invalid file")
                            }
                        }
                    }

                    else -> {
                        tab?.content?.invoke() ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${tab?.type?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: "Unknown"} Tab: ${tab?.name ?: ""}")
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tabs open")
            }
        }
    }

    if (showPermissionDialog && !permissionGranted) {
        PermissionDialog(
            onDismissRequest = { showPermissionDialog = false },
            onRequestPermission = context::requestStoragePermission
        )
    }
}

@Composable
fun PermissionDialog(
    onDismissRequest: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    KlyxDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = "Storage Permission Required",
        message = "To open this file, Klyx needs access to your device's storage. Please grant permission to continue.",
        positiveButton = {
            TextButton(
                onClick = {
                    onRequestPermission()
                    onDismissRequest()
                }
            ) {
                Text("Grant Permission")
            }
        },
        negativeButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
