package com.klyx.extension

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.klyx.extension.capabilities.ProcessExecCapability
import io.itsvks.anyhow.AnyhowResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ExtensionManifestTest : FunSpec() {
    init {
        test("allow exec exact match") {
            val manifest = manifest().copy(
                capabilities = listOf(
                    ExtensionCapability.ProcessExec(
                        ProcessExecCapability(
                            command = "ls",
                            args = listOf("-la")
                        )
                    )
                )
            )

            manifest.allowExec("ls", arrayOf("-la")).shouldBeOk()
            manifest.allowExec("ls", arrayOf("-l")).shouldBeErr()
            manifest.allowExec("pwd", arrayOf()).shouldBeErr()
        }

        test("allow exec wildcard arg") {
            val manifest = manifest().copy(
                capabilities = listOf(
                    ExtensionCapability.ProcessExec(
                        ProcessExecCapability(
                            command = "git",
                            args = listOf("*")
                        )
                    )
                )
            )

            manifest.allowExec("git", arrayOf("status")).shouldBeOk()
            manifest.allowExec("git", arrayOf("commit")).shouldBeOk()
            manifest.allowExec("git", arrayOf("status", "-s")).shouldBeErr() // too many args
            manifest.allowExec("npm", arrayOf("install")).shouldBeErr() // wrong command
        }

        test("allow exec double wildcard") {
            val manifest = manifest().copy(
                capabilities = listOf(
                    ExtensionCapability.ProcessExec(
                        ProcessExecCapability(
                            command = "cargo",
                            args = listOf("test", "**")
                        )
                    )
                )
            )

            manifest.allowExec("cargo", arrayOf("test")).shouldBeOk()
            manifest.allowExec("cargo", arrayOf("test", "--all")).shouldBeOk()
            manifest.allowExec("cargo", arrayOf("test", "--all", "--no-fail-fast")).shouldBeOk()
            manifest.allowExec("cargo", arrayOf("build")).shouldBeErr() // wrong first arg
        }

        test("allows with mixed wildcards") {
            val manifest = manifest().copy(
                capabilities = listOf(
                    ExtensionCapability.ProcessExec(
                        ProcessExecCapability(
                            command = "docker",
                            args = listOf("run", "*", "**")
                        )
                    )
                )
            )

            manifest.allowExec("docker", arrayOf("run", "ngnix")).shouldBeOk()
            manifest.allowExec("docker", arrayOf("run")).shouldBeErr()
            manifest.allowExec("docker", arrayOf("run", "ubuntu", "bash")).shouldBeOk()
            manifest.allowExec("docker", arrayOf("run", "alpine", "sh", "-c", "echo hello")).shouldBeOk()
            manifest.allowExec("docker", arrayOf("ps")).shouldBeErr() // wrong first arg.
        }
    }

    private fun manifest() = ExtensionManifest(
        id = "test",
        name = "Test",
        version = "1.0.0",
        schemaVersion = SchemaVersion.Zero
    )

    private fun <T> AnyhowResult<T>.shouldBeOk() = isOk.shouldBeTrue()
    private fun <T> AnyhowResult<T>.shouldBeErr() = isErr.shouldBeTrue()
}
