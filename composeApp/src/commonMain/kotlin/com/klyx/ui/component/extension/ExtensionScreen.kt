@file:Suppress("UnusedPrivateMember")

package com.klyx.ui.component.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator

@Composable
fun ExtensionScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Navigator(ExtensionListScreen())
    }
}

