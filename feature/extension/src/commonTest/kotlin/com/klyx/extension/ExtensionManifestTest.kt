package com.klyx.extension

import com.klyx.extension.capabilities.ProcessExecCapability
import io.itsvks.anyhow.AnyhowResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import net.peanuuutz.tomlkt.Toml

class ExtensionManifestTest : FunSpec({

    fun manifest() = ExtensionManifest(
        id = "test",
        name = "Test",
        version = "1.0.0",
        schemaVersion = SchemaVersion.Zero
    )

    fun <T> AnyhowResult<T>.shouldBeOk() = isOk.shouldBeTrue()
    fun <T> AnyhowResult<T>.shouldBeErr() = isErr.shouldBeTrue()

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

    test("parse manifest with agent server archive launcher") {
        val tomlSrc = """
                id = "example.agent-server-ext"
                name = "Agent Server Example"
                version = "1.0.0"
                schema_version = 0

                [agent_servers.foo]
                name = "Foo Agent"

                [agent_servers.foo.targets.linux-x86_64]
                archive = "https://example.com/agent-linux-x64.tar.gz"
                cmd = "./agent"
                args = ["--serve"]
            """.trimIndent()

        val manifest: ExtensionManifest = Toml {
            ignoreUnknownKeys = true
        }.decodeFromString(tomlSrc)

        manifest.id shouldBe "example.agent-server-ext"
        manifest.agentServers.shouldContainKey("foo")

        val entry = manifest.agentServers["foo"]!!
        entry.targets.shouldContainKey("linux-x86_64")

        val target = entry.targets["linux-x86_64"]!!
        target.archive shouldBe "https://example.com/agent-linux-x64.tar.gz"
        target.cmd shouldBe "./agent"
        target.args shouldBe listOf("--serve")
    }
})
