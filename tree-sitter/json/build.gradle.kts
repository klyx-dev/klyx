import com.klyx.Configs
import io.github.treesitter.ktreesitter.plugin.GrammarFilesTask
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.support.useToRun
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.PlatformManager
import java.io.OutputStream.nullOutputStream

inline val File.unixPath: String
    get() = if (!os.isWindows) path else path.replace("\\", "/")

val os: OperatingSystem = OperatingSystem.current()
val libsDir = layout.buildDirectory.get().dir("libs")
val grammarDir = projectDir.resolve("tree-sitter-json")

version = grammarDir.resolve("Makefile").readLines()
    .first { it.startsWith("VERSION := ") }.removePrefix("VERSION := ")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("io.github.tree-sitter.ktreesitter-plugin")
}

grammar {
    baseDir = grammarDir
    grammarName = project.name
    className = "TreeSitterJson"
    packageName = "com.klyx.treesitter.json"
    files = arrayOf(
        // grammarDir.resolve("src/scanner.c"),
        grammarDir.resolve("src/parser.c")
    )
}

val generateTask: GrammarFilesTask = tasks.generateGrammarFiles.get()

kotlin {
    jvm {}

    androidTarget {
        withSourcesJar(true)
        publishLibraryVariants("release")
    }

    when {
        os.isLinux -> listOf(linuxX64(), linuxArm64())
        os.isWindows -> listOf(mingwX64())
        os.isMacOsX -> listOf(
            macosArm64(),
            macosX64(),
            iosArm64(),
            iosSimulatorArm64()
        )
        else -> {
            val arch = System.getProperty("os.arch")
            throw GradleException("Unsupported platform: $os ($arch)")
        }
    }.forEach { target ->
        target.compilations.configureEach {
            cinterops.create(grammar.interopName.get()) {
                definitionFile.set(generateTask.interopFile.asFile.get())
                includeDirs.allHeaders(grammarDir.resolve("bindings/c"))
                extraOpts("-libraryPath", libsDir.dir(konanTarget.name))
                tasks.getByName(interopProcessingTaskName).mustRunAfter(generateTask)
            }
        }
    }

    jvmToolchain(17)

    sourceSets {
        val generatedSrc = generateTask.generatedSrc.get()
        configureEach {
            kotlin.srcDir(generatedSrc.dir(name).dir("kotlin"))
        }

        commonMain {
            languageSettings {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }

            dependencies {

            }
        }

        jvmMain {
            resources.srcDir(generatedSrc.dir(name).dir("resources"))
        }
    }
}

android {
    namespace = "com.klyx.treesitter.${grammar.grammarName.get()}"
    compileSdk = Configs.Android.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = Configs.Android.MIN_SDK_VERSION
        ndk {
            moduleName = grammar.libraryName.get()
            //noinspection ChromeOsAbiSupport
            abiFilters += setOf("x86_64", "arm64-v8a", "armeabi-v7a")
        }
        resValue("string", "version", version as String)
    }

    externalNativeBuild {
        cmake {
            path = generateTask.cmakeListsFile.get().asFile
            buildStagingDirectory = file(".cmake")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

tasks.withType<CInteropProcess>().configureEach {
    if (name.startsWith("cinteropTest")) return@configureEach

    val grammarFiles = grammar.files.get()
    val grammarName = grammar.grammarName.get()
    val runKonan = File(konanHome.get()).resolve("bin")
        .resolve(if (os.isWindows) "run_konan.bat" else "run_konan").path
    val libFile = libsDir.dir(konanTarget.name).file("libtree-sitter-$grammarName.a").asFile
    val objectFiles = grammarFiles.map {
        grammarDir.resolve(it.nameWithoutExtension + ".o").path
    }.toTypedArray()
    val loader = PlatformManager(konanHome.get(), false, konanDataDir.orNull).loader(konanTarget)

    doFirst {
        if (!File(loader.absoluteTargetToolchain).isDirectory) loader.downloadDependencies()

        val argsFile = File.createTempFile("args", null)
        argsFile.deleteOnExit()
        argsFile.writer().useToRun {
            write("-I" + grammarDir.resolve("src").unixPath + "\n")
            write("-DTREE_SITTER_HIDE_SYMBOLS\n")
            write("-fvisibility=hidden\n")
            write("-std=c11\n")
            write("-O2\n")
            write("-g\n")
            write("-c\n")
            grammarFiles.forEach { write(it.unixPath + "\n") }
        }

        exec {
            executable = runKonan
            workingDir = grammarDir
            standardOutput = nullOutputStream()
            args("clang", "clang", konanTarget.name, "@" + argsFile.path)
        }

        exec {
            executable = runKonan
            workingDir = grammarDir
            standardOutput = nullOutputStream()
            args("llvm", "llvm-ar", "rcs", libFile.path, *objectFiles)
        }
    }

    inputs.files(*grammarFiles)
    outputs.file(libFile)
}
