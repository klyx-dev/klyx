plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("ktreesitter") {
            id = "com.klyx.ktreesitter"
            implementationClass = "com.klyx.ktreesitter.GrammarPlugin"
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}
