package com.klyx

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.klyx.api.NavDestination
import com.klyx.api.Navigator
import com.klyx.api.SpecialScreens
import com.klyx.api.data.terminal.TerminalManager
import com.klyx.api.event.terminal.TerminalNotificationTapEvent
import com.klyx.api.event.terminal.TerminateAllSessionEvent
import com.klyx.api.ui.Content
import com.klyx.api.ui.ScreenId
import com.klyx.api.ui.ScreenRegistration
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.toastHostState
import com.klyx.core.App
import com.klyx.core.event.subscribeIn
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.event.GlobalEventBus
import com.klyx.platform.service.TerminalService
import com.klyx.plugin.PluginManager
import com.klyx.presentation.components.dialogs.AllFilesAccessDialog
import com.klyx.presentation.components.dialogs.CrashReportDialog
import com.klyx.presentation.components.dialogs.LegacyStorageAccessDialog
import com.klyx.presentation.components.dialogs.NotificationPermissionDialog
import com.klyx.presentation.navigation.Navigator as PresentationNavigator
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.navigation.SettingsScreen
import com.klyx.presentation.navigation.rememberNavigator
import com.klyx.presentation.navigation.toEntries
import com.klyx.presentation.screen.HomeScreen
import com.klyx.presentation.screen.SettingsScreen
import com.klyx.presentation.screen.TerminalScreen
import com.klyx.presentation.screen.settings.AboutScreen
import com.klyx.presentation.screen.settings.AppearanceSettings
import com.klyx.presentation.screen.settings.DeveloperOptionsScreen
import com.klyx.presentation.screen.settings.EditorSettings
import com.klyx.presentation.screen.settings.FileTreeSettingsScreen
import com.klyx.presentation.screen.settings.LogScreen
import com.klyx.presentation.screen.settings.PluginDetailsScreen
import com.klyx.presentation.screen.settings.PluginSettingsScreen
import com.klyx.presentation.screen.settings.PluginsScreen
import com.klyx.presentation.screen.settings.SystemDetailsScreen
import com.klyx.presentation.screen.settings.TerminalSettings
import com.klyx.presentation.viewmodel.EditorViewModel
import com.klyx.presentation.viewmodel.FileTreeViewModel
import com.klyx.ui.ComposeActivity
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.orSnap
import com.klyx.ui.theme.ProvideGoogleSansTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComposeActivity() {

    @OptIn(UnsafeGlobalAccess::class)
    private val app by lazy { GlobalApp }

    private val editorViewModel by viewModel<EditorViewModel>()
    private val fileTreeViewModel by viewModel<FileTreeViewModel>()

    private val pendingTerminalNav = MutableStateFlow(false)

    private val terminalManager: TerminalManager by lazy { app.global() }

    private var pendingCrashData: CrashHandler.CrashData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingCrashData = CrashHandler.loadCrash(this)
        pendingCrashData?.let { data ->
            EditorViewModel.tabIdToSkipOnRestore = data.crashedTabId
        }

        GlobalEventBus.subscribeIn<TerminateAllSessionEvent>(lifecycleScope) {
            terminalManager.sessionBinder.unbind(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalManager.sessionBinder.unbind(this)
    }

    @Composable
    override fun BoxScope.Content() {
        val reduceMotion = LocalReduceMotion.current

        val crashData = pendingCrashData
        var showCrashDialog by remember(crashData) { mutableStateOf(crashData != null) }

        val permissions = rememberRuntimePermissions(this@MainActivity)

        // When the activity is (re)created directly from the terminal notification,
        // seed the back stack with Terminal on top so it renders instantly without
        // first showing Home and animating the transition.
        val launchedFromNotification = remember {
            intent?.action == TerminalService.ACTION_NOTIFICATION_TAP
        }
        LaunchedEffect(launchedFromNotification) {
            if (launchedFromNotification) {
                GlobalEventBus.publish(TerminalNotificationTapEvent)
            }
        }
        val navigator = rememberNavigator(
            initialScreenOnTop = if (launchedFromNotification) Screen.Terminal else null
        )
        val entryProvider = remember { appScreenEntryProvider() }

        // Auto-unregister transient screens (opened via Navigator.openScreen) when their
        // navigation entry is popped, so they don't leak composables or stale data.
        rememberTransientScreenCleanup(navigator, screenRegistry = app.global())

        rememberGlobalNavigator(navigator, app = app)

        val navigateToTerminal by pendingTerminalNav.collectAsStateWithLifecycle()

        LaunchedEffect(navigateToTerminal) {
            if (navigateToTerminal) {
                navigator.navigateTo(Screen.Terminal)
                pendingTerminalNav.value = false
            }
        }

        NavDisplay(
            entries = with(navigator) { entryProvider.toEntries() },
            onBack = navigator::navigateBack,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { transitionSpec(reduceMotion) },
            popTransitionSpec = { popTransitionSpec(reduceMotion) },
            predictivePopTransitionSpec = { popTransitionSpec(reduceMotion) }
        )

        runtimePermissionDialogs(
            permissions = permissions,
            context = this@MainActivity,
            onFinish = { finish() }
        )

        if (showCrashDialog && crashData != null) {
            CrashReportDialog(
                crash = crashData,
                onDismiss = { showCrashDialog = false }
            )
        }
    }

    private fun appScreenEntryProvider(): (Screen) -> NavEntry<Screen> = entryProvider(
        fallback = { key ->
            NavEntry(key = key) { screen ->
                UnknownScreen(screen)
            }
        }
    ) {
        entry<Screen.Home> { HomeScreen() }
        entry<Screen.Terminal> { TerminalScreen() }
        entry<Screen.Settings> { ProvideGoogleSansTypography { SettingsScreen() } }

        settingsEntry<SettingsScreen.Editor> { EditorSettings() }
        settingsEntry<SettingsScreen.Appearance> { AppearanceSettings() }
        settingsEntry<SettingsScreen.Terminal> { TerminalSettings() }
        settingsEntry<SettingsScreen.DeveloperOptions> { DeveloperOptionsScreen() }
        settingsEntry<SettingsScreen.Logs> { LogScreen() }
        settingsEntry<SettingsScreen.SystemDiagnostics> { SystemDetailsScreen() }
        settingsEntry<SettingsScreen.About> { AboutScreen() }
        settingsEntry<SettingsScreen.FileTree> { FileTreeSettingsScreen() }
        settingsEntry<SettingsScreen.Plugins> { PluginsScreen() }
        settingsEntry<SettingsScreen.PluginDetail> { PluginDetailsScreen(it.payload) }
        settingsEntry<SettingsScreen.PluginSettings> { PluginSettingsScreen(it.payload) }

        entry<Screen.Custom> { screen ->
            val ownerId = app.global<ScreenRegistry>().ownerOf(screen.id)
            CrashHandler.currentPluginId = ownerId
            if (ownerId != null && app.global<PluginManager>().isCrashed(ownerId)) {
                PluginCrashedScreen(screen.id)
            } else {
                app.global<ScreenRegistry>()[screen.id]?.invoke() ?: UnknownScreen(screen)
            }
        }
    }

    @Composable
    private fun UnknownScreen(screen: Screen) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (screen is Screen.Custom) {
                    Text(
                        text = "Screen \"${screen.id}\" is not registered.\n\n" +
                                "If you're a plugin developer, make sure to\n" +
                                "register the screen via screens.register()\n" +
                                "before navigating to it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Oops! Something went wrong.\nThis screen isn't available right now.\n\n(${screen::class.qualifiedName})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    @Composable
    private fun PluginCrashedScreen(id: ScreenId) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Screen \"$id\" is not available because the plugin crashed.\n" +
                            "Please open the Plugins settings to unload or reinstall it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    private inline fun <reified K : SettingsScreen> EntryProviderScope<Screen>.settingsEntry(
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (K) -> Unit,
    ) {
        entry<K>(
            //clazzContentKey = ::identity,
            metadata = metadata
        ) {
            ProvideGoogleSansTypography { content(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data

        if (uri != null && uri.scheme == "klyx") {
            when (uri.host) {
                "open" -> {
                    val path = uri.getQueryParameter("project")?.let { Uri.parse(it) }
                        ?: uri.getQueryParameter("file")?.let { Uri.parse(it) }

                    if (path != null) {
                        if (DocumentsContract.isTreeUri(path)) {
                            fileTreeViewModel.addRootNode(path)
                            lifecycleScope.launch {
                                app.toastHostState.showToast("Project opened")
                            }
                        } else {
                            val fileExists = path.scheme != "file" || java.io.File(path.path!!).exists()
                            if (fileExists) {
                                editorViewModel.openFile(path)
                            } else {
                                lifecycleScope.launch {
                                    app.toastHostState.showFailureToast("Invalid path: ${path.path} does not exist")
                                }
                            }
                        }
                    }
                }
            }
            setIntent(Intent())
            return
        }

        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
            if (uri != null) {
                editorViewModel.openFile(uri)
            }
        } else if (intent.action == TerminalService.ACTION_NOTIFICATION_TAP) {
            pendingTerminalNav.value = true
            lifecycleScope.launch {
                GlobalEventBus.publish(TerminalNotificationTapEvent)
            }
        }

        setIntent(Intent())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}

private fun hasLegacyStoragePermissions(context: Context): Boolean {
    val read = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    val write = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    return read && write
}

private fun hasStorageAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        hasLegacyStoragePermissions(context)
    }

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private class RuntimePermissionState(
    val hasStoragePermission: MutableState<Boolean>,
    val showNotificationDialog: MutableState<Boolean>,
    val legacyPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    val notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
)

@Composable
private fun rememberRuntimePermissions(context: Context): RuntimePermissionState {
    val hasStoragePermission = remember { mutableStateOf(hasStorageAccess(context)) }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        hasStoragePermission.value = readGranted && writeGranted
    }

    val showNotificationDialog = remember { mutableStateOf(!hasNotificationPermission(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        showNotificationDialog.value = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission.value = hasStorageAccess(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return RuntimePermissionState(
        hasStoragePermission = hasStoragePermission,
        showNotificationDialog = showNotificationDialog,
        legacyPermissionLauncher = legacyPermissionLauncher,
        notificationPermissionLauncher = notificationPermissionLauncher,
    )
}

@Composable
private fun runtimePermissionDialogs(
    permissions: RuntimePermissionState,
    context: Context,
    onFinish: () -> Unit,
) {
    if (!permissions.hasStoragePermission.value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AllFilesAccessDialog(
                onDismiss = onFinish,
                onConfirm = {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    context.startActivity(intent)
                }
            )
        } else {
            // Android 10 and below
            LegacyStorageAccessDialog(
                onDismiss = onFinish,
                onConfirm = {
                    permissions.legacyPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            )
        }
    } else if (permissions.showNotificationDialog.value) {
        NotificationPermissionDialog(
            onDismiss = { permissions.showNotificationDialog.value = false },
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissions.showNotificationDialog.value = false
                }
            }
        )
    }
}

@Composable
private fun rememberTransientScreenCleanup(
    navigator: PresentationNavigator,
    screenRegistry: ScreenRegistry,
) {
    LaunchedEffect(navigator) {
        var previous = navigator.mapNotNull { (it as? Screen.Custom)?.id }
        snapshotFlow { navigator.mapNotNull { (it as? Screen.Custom)?.id } }
            .collect { current ->
                previous.forEach { id ->
                    if (id !in current) {
                        screenRegistry.unregisterTransient(id)
                    }
                }
                previous = current
            }
    }
}

@Composable
private fun rememberGlobalNavigator(
    navigator: PresentationNavigator,
    app: App,
) {
    LaunchedEffect(navigator) {
        app.setGlobal<Navigator>(object : Navigator {
            override fun navigateTo(destination: NavDestination) {
                val screen: Screen = when (destination) {
                    is NavDestination.Home -> Screen.Home
                    is NavDestination.Settings -> Screen.Settings
                    is NavDestination.Terminal -> Screen.Terminal
                    is NavDestination.Custom -> {
                        when (destination.id) {
                            SpecialScreens.Home -> Screen.Home
                            SpecialScreens.Settings -> Screen.Settings
                            SpecialScreens.Terminal -> Screen.Terminal
                            else -> Screen.Custom(destination.id)
                        }
                    }
                }
                navigator.navigateTo(screen)
            }

            override fun navigateBack() {
                navigator.navigateBack()
            }

            override fun openScreen(screenId: ScreenId, content: Content): ScreenRegistration {
                val registry = app.global<ScreenRegistry>()
                registry.setTransient(screenId, content)
                navigator.navigateTo(Screen.Custom(screenId))
                return ScreenRegistration { registry.unregisterTransient(screenId) }
            }
        })
    }
}

private fun intOffsetSpec(reduceMotion: Boolean) =
    tween<IntOffset>(350, easing = FastOutSlowInEasing).orSnap(reduceMotion)

private fun floatSpec(reduceMotion: Boolean) =
    tween<Float>(350, easing = FastOutSlowInEasing).orSnap(reduceMotion)

private fun enterTransition(reduceMotion: Boolean) =
    slideInHorizontally(initialOffsetX = { it }, animationSpec = intOffsetSpec(reduceMotion))

private fun exitTransition(reduceMotion: Boolean) = slideOutHorizontally(
    targetOffsetX = { -it / 3 },
    animationSpec = intOffsetSpec(reduceMotion)
) + fadeOut(animationSpec = floatSpec(reduceMotion))

private fun popEnterTransition(reduceMotion: Boolean) = slideInHorizontally(
    initialOffsetX = { -it / 3 },
    animationSpec = intOffsetSpec(reduceMotion)
) + scaleIn(animationSpec = floatSpec(reduceMotion), initialScale = 0.9f)

private fun popExitTransition(reduceMotion: Boolean) =
    slideOutHorizontally(
        animationSpec = intOffsetSpec(reduceMotion),
        targetOffsetX = { it }
    ) + scaleOut(
        animationSpec = floatSpec(reduceMotion),
        targetScale = 0.75f,
        transformOrigin = TransformOrigin(0.5f, 0.5f)
    )

private fun transitionSpec(reduceMotion: Boolean) =
    enterTransition(reduceMotion) togetherWith exitTransition(reduceMotion)

private fun popTransitionSpec(reduceMotion: Boolean) =
    popEnterTransition(reduceMotion) togetherWith popExitTransition(reduceMotion)
