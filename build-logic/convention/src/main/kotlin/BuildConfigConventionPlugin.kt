import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import com.klyx.buildlogic.currentVersion
import com.klyx.buildlogic.currentVersionCode
import com.klyx.buildlogic.libs
import com.klyx.buildlogic.toCamelCase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class BuildConfigConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.codingfeline.buildkonfig")

            extensions.configure<BuildKonfigExtension> {
                packageName = "com.klyx.${project.name.toCamelCase()}"
                objectName = "KlyxBuildConfig"
                exposeObjectWithName = "KlyxBuildConfig"

                val isRelease = gradle.startParameter.taskNames.any {
                    it.contains("Release", ignoreCase = true)
                }
                val buildType = if (isRelease) "release" else "debug"
                fun env(name: String): String? = System.getenv(name)

                defaultConfigs {
                    buildConfigField(Type.STRING, "APP_NAME", "Klyx", const = true)
                    buildConfigField(
                        Type.STRING,
                        "VERSION_NAME",
                        project.findProperty("versionName") as? String ?: currentVersion(),
                        const = true
                    )
                    buildConfigField(Type.INT, "VERSION_CODE", currentVersionCode().toString(), const = true)
                    buildConfigField(Type.STRING, "APPLICATION_ID", "com.klyx", const = true)

                    buildConfigField(Type.STRING, "BUILD_TYPE", buildType, const = true)
                    buildConfigField(Type.BOOLEAN, "IS_DEBUG", (!isRelease).toString(), const = true)

                    buildConfigField(Type.BOOLEAN, "CI", (env("CI") == "true").toString(), const = true)

                    buildConfigField(Type.STRING, "CI_PROVIDER", env("CI_PROVIDER"), nullable = true)
                    buildConfigField(Type.STRING, "CI_WORKFLOW", env("CI_WORKFLOW"), nullable = true)
                    buildConfigField(Type.STRING, "CI_JOB", env("CI_JOB"), nullable = true)

                    buildConfigField(Type.LONG, "CI_RUN_ID", env("CI_RUN_ID"), nullable = true)
                    buildConfigField(Type.INT, "CI_RUN_NUMBER", env("CI_RUN_NUMBER"), nullable = true)
                    buildConfigField(Type.INT, "CI_ATTEMPT", env("CI_ATTEMPT"), nullable = true)

                    buildConfigField(Type.STRING, "CI_ACTOR", env("CI_ACTOR"), nullable = true)
                    buildConfigField(Type.STRING, "CI_EVENT", env("CI_EVENT"), nullable = true)

                    buildConfigField(Type.STRING, "GIT_COMMIT", env("GIT_COMMIT"), nullable = true)
                    buildConfigField(Type.STRING, "GIT_COMMIT_SHORT", env("GIT_COMMIT")?.take(7), nullable = true)
                    buildConfigField(Type.STRING, "GIT_BRANCH", env("GIT_BRANCH"), nullable = true)
                    buildConfigField(Type.STRING, "GIT_REF", env("GIT_REF"), nullable = true)
                    buildConfigField(Type.STRING, "GIT_REF_TYPE", env("GIT_REF_TYPE"), nullable = true)

                    buildConfigField(Type.STRING, "GIT_REPO", env("GIT_REPO"), nullable = true)
                    buildConfigField(Type.STRING, "GIT_REPO_OWNER", env("GIT_REPO_OWNER"), nullable = true)
                    buildConfigField(
                        Type.STRING,
                        "GIT_REPO_NAME",
                        env("GIT_REPO")?.substringAfter("/"),
                        nullable = true
                    )

                    buildConfigField(
                        Type.BOOLEAN,
                        "IS_PULL_REQUEST",
                        (env("IS_PULL_REQUEST") == "true").toString(),
                        const = true
                    )

                    buildConfigField(
                        Type.LONG,
                        "BUILD_TIMESTAMP",
                        System.currentTimeMillis().toString(),
                        const = true
                    )

                    buildConfigField(Type.STRING, "CI_OS", env("CI_OS"), nullable = true)
                    buildConfigField(Type.STRING, "CI_ARCH", env("CI_ARCH"), nullable = true)

                    buildConfigField(
                        Type.STRING,
                        "KOTLIN_VERSION",
                        libs.findVersion("kotlin").get().toString(),
                        const = true
                    )

                    buildConfigField(
                        Type.BOOLEAN,
                        "ENABLE_STRICT_MODE",
                        (env("ENABLE_STRICT_MODE") != "false").toString(),
                        const = true
                    )
                    buildConfigField(
                        Type.BOOLEAN,
                        "ENABLE_EXPERIMENTAL",
                        (env("ENABLE_EXPERIMENTAL") == "true").toString(),
                        const = true
                    )
                }
            }
        }
    }
}
