import com.klyx.buildlogic.currentStableVersion
import com.klyx.buildlogic.currentVersionCode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

class ComposeDesktopConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "klyx.multiplatform.compose")

            extensions.configure<ComposeExtension> {
                configure<DesktopExtension> {
                    application {
                        nativeDistributions {
                            targetFormats(
                                TargetFormat.Dmg,
                                TargetFormat.Msi,
                                TargetFormat.Deb,
                                TargetFormat.Rpm
                            )
                            packageName = "klyx"
                            packageVersion = currentStableVersion()
                            description = "Klyx - Kotlin Multiplatform Code Editor"
                            copyright = "Klyx"
                            vendor = "Klyx"

                            linux {
                                iconFile.set(rootProject.file("klyxApp/src/commonMain/resources/icon.png"))
                                packageName = "klyx"
                                menuGroup = "Development"
                                appCategory = "Development"

                                modules("jdk.security.auth")
                            }

                            windows {
                                iconFile.set(rootProject.file("klyxApp/src/commonMain/resources/icon.png"))
                                packageName = "Klyx"
                                dirChooser = true
                                perUserInstall = true
                                menuGroup = "Development"
                            }

                            macOS {
                                iconFile.set(rootProject.file("klyxApp/src/commonMain/resources/icon.png"))
                                packageName = "Klyx"
                                dmgPackageVersion = currentStableVersion()
                                packageBuildVersion = currentVersionCode().toString()
                                appCategory = "public.app-category.developer-tools"
                            }
                        }
                    }
                }
            }
        }
    }
}
