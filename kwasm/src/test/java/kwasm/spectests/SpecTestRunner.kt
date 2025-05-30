/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm.spectests

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

class SpecTestRunner(private val testClass: Class<*>) : Runner() {
    override fun getDescription(): Description {
        return Description.createSuiteDescription(testClass)
    }

    override fun run(notifier: RunNotifier) {
        val testObject = testClass.getDeclaredConstructor().newInstance()
        for (method in testClass.methods) {
            if (method.isAnnotationPresent(SpecTest::class.java)) {
                val annotation = method.getAnnotation(SpecTest::class.java)
                val paths = annotation.files.map { "spec_tests/${annotation.subdir}/$it" }

                paths.forEachIndexed { index, path ->
                    testClass.classLoader.getResourceAsStream(path).use { stream ->
                        val description =
                            Description.createTestDescription(
                                testClass,
                                "${method.name}: ${annotation.subdir}/${annotation.files[index]}"
                            )
                        notifier.fireTestStarted(description)
                        try {
                            method.invoke(testObject, stream, path)
                            notifier.fireTestFinished(description)
                        } catch (e: Throwable) {
                            notifier.fireTestFailure(Failure(description, e.cause))
                        }
                    }
                }
            }
        }
    }
}

annotation class SpecTest(val subdir: String, vararg val files: String)
