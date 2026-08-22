package com.klyx.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.klyx.R
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.showFailureToast
import com.klyx.app.icons.DeleteForever
import com.klyx.app.icons.DeleteOutline
import com.klyx.app.icons.RestoreFromTrash
import com.klyx.data.database.TrashEntity
import com.klyx.data.repository.TrashRepository
import com.klyx.i18n.strings
import com.klyx.presentation.components.dialogs.DeletePermanentlyConfirmationDialog
import com.klyx.presentation.components.dialogs.EmptyTrashConfirmationDialog
import com.klyx.presentation.navigation.LocalNavigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel
import java.text.DateFormat
import java.util.Date

@KoinViewModel
class TrashViewModel(
    private val trashRepository: TrashRepository
) : ViewModel() {

    val items = trashRepository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(
        id: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = trashRepository.restore(id)) {
                is TrashRepository.Result.Success -> onSuccess()
                is TrashRepository.Result.Failure -> onError(result.message)
            }
        }
    }

    fun deletePermanently(id: Long, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (trashRepository.deletePermanently(id) is TrashRepository.Result.Failure) {
                onError("Could not delete item.")
            }
        }
    }

    fun emptyTrash(onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { trashRepository.emptyTrash() }
                .onFailure { onError(it.message ?: "Could not empty Trash.") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen() {
    val navigator = LocalNavigator.current
    val viewModel: TrashViewModel = koinViewModel()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var emptyTrashTarget by remember { mutableStateOf<TrashEntity?>(null) }
    var showEmptyAllDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val toastHostState = LocalToastHostState.current

    emptyTrashTarget?.let { target ->
        DeletePermanentlyConfirmationDialog(
            itemName = target.displayName,
            onDismiss = { emptyTrashTarget = null },
            onConfirm = {
                emptyTrashTarget = null
                viewModel.deletePermanently(
                    id = target.id,
                    onError = { message ->
                        scope.launch { toastHostState.showFailureToast(message) }
                    }
                )
            }
        )
    }

    if (showEmptyAllDialog) {
        EmptyTrashConfirmationDialog(
            itemCount = items.size,
            onDismiss = { showEmptyAllDialog = false },
            onConfirm = {
                showEmptyAllDialog = false
                viewModel.emptyTrash(
                    onError = { message ->
                        scope.launch { toastHostState.showFailureToast(message) }
                    }
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(strings.trash) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        FilledIconButton(
                            modifier = Modifier.padding(end = 12.dp, top = 4.dp),
                            onClick = { showEmptyAllDialog = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = strings.emptyTrash
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            TrashEmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { entity ->
                    val restoredMessage = strings.restoredItem(entity.displayName)
                    TrashItemRow(
                        entity = entity,
                        onRestore = {
                            viewModel.restore(
                                id = entity.id,
                                onSuccess = {
                                    scope.launch {
                                        toastHostState.showToast(restoredMessage)
                                    }
                                },
                                onError = { message ->
                                    scope.launch { toastHostState.showFailureToast(message) }
                                }
                            )
                        },
                        onDeleteRequest = { emptyTrashTarget = entity }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrashEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialShapes.Cookie12Sided.toShape()
                        )
                )

                Icon(
                    imageVector = Icons.Rounded.RestoreFromTrash,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.trashIsEmpty,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.trashIsEmptyDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrashItemRow(
    entity: TrashEntity,
    onRestore: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (entity.isDirectory) R.drawable.folder_24px else R.drawable.description_24px
                ),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (entity.isDirectory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entity.originalPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(
                            strings.deletedAgo(
                                DateFormat.getDateInstance(DateFormat.MEDIUM)
                                    .format(Date(entity.deletedAt))
                            )
                        )
                        if (entity.sizeBytes > 0) {
                            append(" · ")
                            append(android.text.format.Formatter.formatShortFileSize(context, entity.sizeBytes))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onRestore,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestoreFromTrash,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(strings.restore, style = MaterialTheme.typography.labelLarge)
            }

            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = strings.deletePermanently,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
