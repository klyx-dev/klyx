package com.klyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.klyx.editor.cursor.CursorPosition
import com.klyx.editor.compose.CodeEditor
import com.klyx.editor.rememberCodeEditorState
import com.klyx.ui.theme.rememberFontFamily
import kotlinx.coroutines.delay

class Test : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialCode = generateDummyCode(1000)

        setContent {
            val fontFamily = rememberFontFamily("JetBrains Mono")
            val editorState = rememberCodeEditorState(initialCode)
            var fontSize by remember { mutableStateOf(18.sp) }

            LaunchedEffect(Unit) {
                for (i in 1..10) {
                    delay(300)
                    //editorState.moveCursor(CursorPosition.Direction.Right)
                    editorState.insert("\na")
                }
            }

            Surface(color = Color.Black) {
                Box(
                    modifier = Modifier
                        .systemBarsPadding()
                        .imePadding()
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            editorState.moveCursorTo(CursorPosition(line = 65, column = 5))
//                            editorState.insert("\na")
                            //fontSize = (fontSize.value + 1).sp
                        }
                ) {
                    CodeEditor(
                        editorState = editorState,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
//                        lineSpacing = 4.dp,
//                        pinLineNumber = true,
//                        letterSpacing = 0.1.em,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun generateDummyCode(lines: Int): String {
        val sb = StringBuilder()
        for (i in 1..lines) {
            sb.append("""
                public static void main(String[] args) {
                    System.out.println("Hellow");
                }
                
                public class Test {
                    public static void main(String[] args) {
                        System.out.println("Hellow");
                    }
                }
                
            """.trimIndent())
        }
        return sb.toString()
    }
}
