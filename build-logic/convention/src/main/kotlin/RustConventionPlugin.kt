import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class RustConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "dev.gobley.cargo")
            apply(plugin = "dev.gobley.uniffi")
            apply(plugin = "org.jetbrains.kotlin.plugin.atomicfu")
        }
    }
}
