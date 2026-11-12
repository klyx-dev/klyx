import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.klyx.Screen
import com.klyx.di.initApp
import io.github.vinceglb.filekit.FileKit

fun main() {
    initApp {
        printLogger()
    }

    FileKit.init("com.klyx")

    application {
        Window(onCloseRequest = ::exitApplication) {
            Screen()
        }
    }
}
