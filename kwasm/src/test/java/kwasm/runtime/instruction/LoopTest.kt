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

package kwasm.runtime.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LoopTest {
    @Test
    fun loop_canLoopThousandsOfTimes_withoutStackOverflow() {
        val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)
        val looper = idStr("looper")
        val limit = idStr("limit")
        val counter = idStr("counter")
        val globalCount = idStr("count")

        val program = KWasmProgram.builder(memoryProvider)
            .withModule(
                name = "testModule",
                source =
                    """
                    (module
                        (global $globalCount (export "count") (mut i32) (i32.const 0)) 
                        (func $looper (export "looper") (param $limit i32) 
                            (local $counter i32)
                            (local.set $counter (i32.const 0))
                            (loop
                                ;; Increment our counter.
                                (local.set $counter (i32.add (local.get $counter) (i32.const 1)))
                                
                                ;; Stash the value in a global.
                                (global.set $globalCount (local.get $counter))
                                
                                ;; Go back to the start of the loop if the counter is less than the 
                                ;; limit.
                                (br_if 0 
                                    (i32.lt_u (local.get $counter) (local.get $limit))
                                )
                            )
                        )
                    )
                    """.trimIndent()
            )
            .build()

        val loops = 50000

        program.getFunction("testModule", "looper").invoke(loops)
        assertThat(program.getGlobalInt("testModule", "count")).isEqualTo(loops)
    }

    private fun idStr(name: String): String = "\$$name"
}
