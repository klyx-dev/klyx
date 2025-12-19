package com.klyx.extension.capabilities

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class ProcessExecCapabilityTest : FunSpec() {
    init {
        test("allows with exact match") {
            val capability = ProcessExecCapability(
                command = "ls",
                args = listOf("-la")
            )

            capability.allows("ls", arrayOf("-la")).shouldBeTrue()
            capability.allows("ls", arrayOf("-l")).shouldBeFalse()
            capability.allows("pwd", emptyArray()).shouldBeFalse()
        }

        test("allows with wildcard arg") {
            val capability = ProcessExecCapability(
                command = "git",
                args = listOf("*")
            )

            capability.allows("git", arrayOf("status")).shouldBeTrue()
            capability.allows("git", arrayOf("commit")).shouldBeTrue()
            // Too many args.
            capability.allows("git", arrayOf("status", "-s")).shouldBeFalse()
            // Wrong command.
            capability.allows("npm", arrayOf("install")).shouldBeFalse()
        }

        test("allows with double wildcard") {
            val capability = ProcessExecCapability(
                command = "cargo",
                args = listOf("test", "**")
            )

            capability.allows("cargo", arrayOf("test")).shouldBeTrue()
            capability.allows("cargo", arrayOf("test", "--all")).shouldBeTrue()
            capability.allows("cargo", arrayOf("test", "--all", "--no-fail-fast")).shouldBeTrue()
            // Wrong first arg.
            capability.allows("cargo", arrayOf("build")).shouldBeFalse()
        }
        
        test("allows with mixed wildcards") {
            val capability = ProcessExecCapability(
                command = "docker",
                args = listOf("run", "*", "**")
            )

            capability.allows("docker", arrayOf("run", "ngnix")).shouldBeTrue()
            capability.allows("docker", arrayOf("run")).shouldBeFalse()
            capability.allows("docker", arrayOf("run", "ubuntu", "bash")).shouldBeTrue()
            capability.allows("docker", arrayOf("run", "alpine", "sh", "-c", "echo hello")).shouldBeTrue()
            // Wrong first arg.
            capability.allows("docker", arrayOf("ps")).shouldBeFalse()
        }
    }
}
