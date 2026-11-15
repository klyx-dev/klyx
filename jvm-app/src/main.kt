import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.klyx.Screen
import com.klyx.document.DocumentPersistence
import com.klyx.di.initApp
import io.github.vinceglb.filekit.FileKit

fun main() {
    val koinApplication = initApp {
        printLogger()
    }

    FileKit.init("com.klyx")

    val persistence = koinApplication.koin.get<DocumentPersistence>()
    application {
        val requestExit = { persistence.requestExit(::exitApplication) }
        Window(onCloseRequest = requestExit) {
            Screen(onExit = ::exitApplication)
        }
    }
}
