import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.klyx.Screen
import com.klyx.di.initKoin

fun main() {
    initKoin {
        printLogger()
    }

    application {
        Window(onCloseRequest = ::exitApplication) {
            Screen()
        }
    }
}
