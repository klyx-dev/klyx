package com.klyx.nodegraph.core

import androidx.compose.ui.graphics.Color

object StandardNodeColors {

    /**
     * Colors for Pin Types (The little connection dots and wires)
     */
    object Types {
        // Execution Flow (The main white wire that triggers actions)
        val Exec = Color(0xFFFFFFFF)

        val Boolean = Color(0xFFE53935)
        val Integer = Color(0xFF00ACC1)
        val Float = Color(0xFF7CB342)
        val String = Color(0xFFD81B60)

        val ObjectReference = Color(0xFF1976D2)
        val AnyType = Color(0xFF9E9E9E)

        val Enum = Color(0xFF1B5E20)
    }

    /**
     * Colors for Node Headers (The top bar of the node block)
     */
    object Headers {
        val Event = Color(0xFFB71C1C)
        val Action = Color(0xFF1565C0)
        val Pure = Color(0xFF2E7D32)
        val ControlFlow = Color(0xFFE65100)
        val Variable = Color(0xFF00695C)

        val System = Color(0xFF673AB7)
    }
}
