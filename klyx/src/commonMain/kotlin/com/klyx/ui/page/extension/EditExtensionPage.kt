package com.klyx.ui.page.extension

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klyx.core.app.globalOf
import com.klyx.core.platform.LocalPlatform
import com.klyx.core.platform.showToast
import com.klyx.extension.nodegraph.ExtensionManager
import com.klyx.extension.nodegraph.createInitialGraph
import com.klyx.icons.Icons
import com.klyx.icons.Save
import com.klyx.nodegraph.GraphDefaults
import com.klyx.nodegraph.GraphEditor
import com.klyx.nodegraph.GraphState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExtensionPage(
    filePath: String,
    edit: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ExtensionViewModel = viewModel()
) {
    val extensionManager = globalOf<ExtensionManager>()
    val platform = LocalPlatform.current
    val density = LocalDensity.current

    LaunchedEffect(filePath) {
        viewModel.loadOrCreateGraph(filePath) {
            GraphState(
                density = density,
                registry = extensionManager.registry
            ).also(::createInitialGraph)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val state = viewModel.graphState

        if (viewModel.isLoading || state == null) {
            CircularProgressIndicator()
        } else {
            GraphEditor(
                modifier = Modifier.fillMaxSize(),
                state = state,
                isViewOnly = !edit,
                settings = GraphDefaults.settings(showMinimap = !edit),
                extraToolbarButtons = {
                    if (edit) {
                        ExtendedToolbarButton(
                            text = "Save",
                            icon = Icons.Save,
                            onClick = {
                                viewModel.saveGraph(filePath, extensionManager)
                                platform.showToast("Saved")
                            }
                        )
                    }
                }
            )

//            if (edit) {
//                ExtendedFloatingActionButton(
//                    onClick = { viewModel.saveGraph(filePath, extensionManager) },
//                    modifier = Modifier
//                        .align(Alignment.BottomEnd)
//                        .padding(24.dp),
//                    icon = { Icon(Icons.Save, contentDescription = "Save Extension") },
//                    text = { Text("Save Graph") }
//                )
//            }
        }
    }
}
