import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.klyx-dev"
version = property("project.version") as String

gradlePlugin {
    plugins {
        register("klyxBundle") {
            id = "io.github.klyx-dev.plugin"
            implementationClass = "com.klyx.gradle.KlyxPluginPublishingPlugin"
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.klyx-dev",
        artifactId = "klyx-gradle-plugin",
        version = property("project.version") as String
    )
}

configure<MavenPublishBaseExtension> {
    pomFromGradleProperties()
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    configure(
        GradlePlugin(
            sourcesJar = SourcesJar.Sources(),
            javadocJar = JavadocJar.Empty()
        )
    )
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.android.tools)
}
