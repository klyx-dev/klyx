package com.klyx

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.klyx.ui.ComposeActivity

class MainActivity : ComposeActivity() {
    @Composable
    override fun Content() {
        Surface {
            Text("Hello, World.")
        }
    }
}
