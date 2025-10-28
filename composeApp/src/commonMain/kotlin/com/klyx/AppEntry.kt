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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.klyx.AppRoute.Settings.About
import com.klyx.AppRoute.Settings.Appearance
import com.klyx.AppRoute.Settings.DarkTheme
import com.klyx.AppRoute.Settings.EditorPreferences
import com.klyx.AppRoute.Settings.GeneralPreferences
import com.klyx.AppRoute.Settings.SettingsPage
import com.klyx.core.cmd.CommandManager
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.logging.KxLog
import com.klyx.core.logging.MessageType
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.notification.ui.NotificationOverlay
import com.klyx.core.registerGeneralCommands
import com.klyx.core.settings.currentAppSettings
import com.klyx.core.ui.animatedComposable
import com.klyx.extension.ExtensionManager
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

@Composable
fun AppEntry() {
    val appSettings = currentAppSettings
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.subscribeToEvent<KeyEvent> { event ->
            if (event.isCtrlPressed && event.isShiftPressed && event.key == Key.P) {
                CommandManager.showCommandPalette()
            }
        }
    }

    LaunchedEffect(appSettings.loadExtensionsOnStartup) {
        if (appSettings.loadExtensionsOnStartup) {
            ExtensionManager.loadExtensions()
        }
    }

    collectLogs(statusBarViewModel)

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
                gesturesEnabled = (drawerState.isOpen || !isTabOpen) && currentDestination?.hasRoute<AppRoute.Home>() == true
            ) {
                registerGeneralCommands(editorViewModel, klyxViewModel)

                NavHost(
                    modifier = Modifier.align(Alignment.Center),
                    navController = navController,
                    startDestination = AppRoute.Home
                ) {
                    animatedComposable<AppRoute.Home> {
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
                            }
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
    onNavigateTo: (route: Any) -> Unit,
) {
    navigation<AppRoute.Settings>(startDestination = SettingsPage) {
        animatedComposable<SettingsPage> { SettingsPage(onNavigateBack = onNavigateBack, onNavigateTo = onNavigateTo) }
        animatedComposable<GeneralPreferences> { GeneralPreferences(onNavigateBack = onNavigateBack) }
        animatedComposable<Appearance> {
            AppearancePreferences(
                onNavigateBack = onNavigateBack,
                onNavigateTo = onNavigateTo
            )
        }
        animatedComposable<DarkTheme> { DarkThemePreferences { onNavigateBack() } }
        animatedComposable<EditorPreferences> { EditorPreferences(onNavigateBack) }
        animatedComposable<About> { AboutPage(onNavigateBack) }
    }
}

@Suppress("ComposableNaming")
@Composable
private fun collectLogs(statusBarViewModel: StatusBarViewModel = koinViewModel()) {
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
