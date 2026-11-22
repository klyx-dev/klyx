package com.klyx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.runtime.action.ActivateProject
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.project.rememberProjects
import com.klyx.ui.theme.background
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.headline
import com.klyx.ui.theme.label
import com.klyx.ui.theme.labelSmall
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography

@Composable
internal fun WelcomeScreen(
    onOpenFolder: () -> Unit,
    onBrowseFiles: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val [projects, projectIds] = rememberProjects()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme[colors][background])
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Klyx", style = Theme[typography][headline], color = Theme[colors][textPrimary])
            Text(
                "Your workspace, ready for code.",
                style = Theme[typography][bodySmall],
                color = Theme[colors][textMuted],
            )
            Spacer(Modifier.height(12.dp))
            Text("Start", style = Theme[typography][label], color = Theme[colors][textPrimary])
            KxButton(onClick = onOpenFolder, modifier = Modifier.fillMaxWidth()) {
                Text("Open Folder")
            }
            if (projects.isNotEmpty()) {
                KxButton(
                    onClick = onBrowseFiles,
                    variant = KxButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Browse Files")
                }
            }
            KxButton(onClick = onSettings, variant = KxButtonVariant.Ghost) {
                Text("Settings")
            }
            if (projectIds.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Open folders", style = Theme[typography][label], color = Theme[colors][textPrimary])
                projectIds.mapNotNull { projects[it] }.forEach { project ->
                    KxButton(
                        onClick = {
                            action(ActivateProject(project.id))()
                            onBrowseFiles()
                        },
                        variant = KxButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = project.name,
                                style = Theme[typography][label],
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = project.root.toString(),
                                style = Theme[typography][labelSmall],
                                color = Theme[colors][textMuted],
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Select a file in the explorer to start editing. Double-click a preview tab to keep it open.",
                style = Theme[typography][bodySmall],
                color = Theme[colors][textMuted],
            )
        }
    }
}
