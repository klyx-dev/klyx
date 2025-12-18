import com.android.build.api.dsl.ApplicationExtension
import com.klyx.buildlogic.configureBuildType
import com.klyx.buildlogic.configureKotlinAndroid
import com.klyx.buildlogic.currentVersion
import com.klyx.buildlogic.currentVersionCode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import java.io.File
import java.util.Properties

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.android")
            apply(plugin = "klyx.android.lint")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)

                namespace = "com.klyx.app"

                defaultConfig {
                    targetSdk = 28
                    applicationId = "com.klyx"

                    versionCode = currentVersionCode()
                    versionName = project.findProperty("versionName") as? String ?: currentVersion()
                }

                signingConfigs {
                    create("release") {
                        val isCI = System.getenv("GITHUB_ACTIONS")?.toBoolean() ?: false

                        val propPath = if (isCI) {
                            "/tmp/sign.properties"
                        } else {
                            "${System.getenv("HOME")}/klyx/key/sign.properties"
                        }

                        val propFile = File(propPath)
                        if (propFile.exists()) {
                            val properties = Properties().also {
                                it.load(propFile.inputStream())
                            }

                            keyAlias = properties.getProperty("keyAlias")
                            keyPassword = properties.getProperty("keyPassword")
                            storeFile = if (isCI) {
                                File("/tmp/klyx.keystore")
                            } else {
                                File(properties.getProperty("storeFile"))
                            }
                            storePassword = properties.getProperty("storePassword")
                        } else {
                            println("Sign properties file not found at $propPath")
                        }
                    }

                    getByName("debug") {
                        storeFile = file(rootProject.file("composeApp/testkey.keystore"))
                        storePassword = "testkey"
                        keyAlias = "testkey"
                        keyPassword = "testkey"
                    }
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                configureBuildType(this)
            }
        }
    }
}
