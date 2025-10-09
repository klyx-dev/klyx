package com.klyx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.klyx.core.cmd.CommandManager
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.logging.KxLog
import com.klyx.core.logging.MessageType
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.notification.ui.NotificationOverlay
import com.klyx.core.ui.Route
import com.klyx.core.ui.animatedComposable
import com.klyx.extension.api.Worktree
import com.klyx.filetree.FileTreeViewModel
import com.klyx.ui.DisclaimerDialog
import com.klyx.ui.component.PermissionDialog
import com.klyx.ui.component.log.LogBuffer
import com.klyx.ui.page.SettingsPage
import com.klyx.ui.page.main.MainPage
import com.klyx.ui.page.settings.about.AboutPage
import com.klyx.ui.page.settings.appearance.AppearancePreferences
import com.klyx.ui.page.settings.appearance.DarkThemePreferences
import com.klyx.ui.page.settings.editor.EditorPreferences
import com.klyx.ui.page.settings.general.GeneralPreferences
import com.klyx.ui.theme.KlyxTheme
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.StatusBarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

val LocalDrawerState = staticCompositionLocalOf<DrawerState> {
    noLocalProvidedFor<DrawerState>()
}

val LocalLogBuffer = staticCompositionLocalOf { LogBuffer(maxSize = 2000) }

private val TopDestinations = listOf(Route.HOME, Route.SETTINGS_PAGE)

@Composable
fun AppEntry() {
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val editorViewModel: EditorViewModel = koinViewModel()
    val klyxViewModel: KlyxViewModel = koinViewModel()
    val statusBarViewModel: StatusBarViewModel = koinViewModel()
    val fileTreeViewModel: FileTreeViewModel = koinViewModel()

    val project by klyxViewModel.openedProject.collectAsStateWithLifecycle()
    val appState by klyxViewModel.appState.collectAsStateWithLifecycle()

    val isTabOpen by editorViewModel.isTabOpen.collectAsStateWithLifecycle()

    val onNavigateBack: () -> Unit = {
        with(navController) {
            if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                popBackStack()
            }
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var currentTopDestination by rememberSaveable { mutableStateOf(currentRoute) }

    LaunchedEffect(currentRoute) {
        if (currentRoute in TopDestinations) {
            currentTopDestination = currentRoute
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.subscribeToEvent<KeyEvent> { event ->
            if (event.isCtrlPressed && event.isShiftPressed && event.key == Key.P) {
                CommandManager.showPalette()
            }
        }
    }

    CollectLogs(
        statusBarViewModel = statusBarViewModel
    )

    KlyxTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            WorktreeDrawer(
                project = project,
                drawerState = drawerState,
                onFileClick = { file, worktree ->
                    editorViewModel.openFile(file, worktree)
                },
                onDirectoryPicked = { directory ->
                    if (directory.isPermissionRequired(R_OK or W_OK)) {
                        klyxViewModel.showPermissionDialog()
                    } else {
                        klyxViewModel.openProject(Worktree(directory))

                        if (drawerState.isClosed) {
                            scope.launch { drawerState.open() }
                        }
                    }
                },
                gesturesEnabled = (drawerState.isOpen || !isTabOpen) && currentRoute == Route.HOME
            ) {
                NavHost(
                    modifier = Modifier.align(Alignment.Center),
                    navController = navController,
                    startDestination = Route.HOME
                ) {
                    animatedComposable(Route.HOME) {
                        MainPage(
                            modifier = Modifier.fillMaxSize(),
                            editorViewModel = editorViewModel,
                            klyxViewModel = klyxViewModel,
                            statusBarViewModel = statusBarViewModel,
                            fileTreeViewModel = fileTreeViewModel,
                            onNavigateToRoute = { route ->
                                navController.navigate(route = route) {
                                    launchSingleTop = true
                                }
                            },
                            currentRoute = currentRoute,
                            currentTopDestination = currentTopDestination
                        )
                    }

                    settingsGraph(
                        onNavigateBack = onNavigateBack,
                        onNavigateTo = { route ->
                            navController.navigate(route = route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            if (appState.showPermissionDialog) {
                PermissionDialog(
                    onDismissRequest = { klyxViewModel.dismissPermissionDialog() },
                    onRequestPermission = { requestFileAccessPermission() }
                )
            }

            var disclaimerAccepted by remember { mutableStateOf(DisclaimerManager.hasAccepted()) }

            if (!disclaimerAccepted) {
                DisclaimerDialog(
                    onAccept = { DisclaimerManager.setAccepted(); disclaimerAccepted = true }
                )
            }

            NotificationOverlay()
        }
    }
}

fun NavGraphBuilder.settingsGraph(
    onNavigateBack: () -> Unit,
    onNavigateTo: (route: String) -> Unit,
) {
    navigation(startDestination = Route.SETTINGS_PAGE, route = Route.SETTINGS) {
        animatedComposable(Route.SETTINGS_PAGE) {
            SettingsPage(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
        }
        animatedComposable(Route.GENERAL_PREFERENCES) {
            GeneralPreferences(onNavigateBack = onNavigateBack)
        }
        animatedComposable(Route.APPEARANCE) {
            AppearancePreferences(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo)
        }
        animatedComposable(Route.DARK_THEME) { DarkThemePreferences { onNavigateBack() } }
        animatedComposable(Route.EDITOR_PREFERENCES) { EditorPreferences(onNavigateBack) }
        animatedComposable(Route.ABOUT) { AboutPage(onNavigateBack) }
    }
}

@Composable
private fun CollectLogs(statusBarViewModel: StatusBarViewModel = koinViewModel()) {
    val logBuffer = LocalLogBuffer.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default.limitedParallelism(4)) {
            KxLog.logFlow.collect { log ->
                logBuffer.add(log)
                statusBarViewModel.setCurrentLogMessage(log, isProgressive = log.type == MessageType.Progress)
            }
        }
    }
}
