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

import kwasm.KWasmProgram
import kwasm.api.ByteBufferMemoryProvider
import kwasm.api.HostFunction
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.ast.type.TableType
import kwasm.format.ParseContext
import kwasm.runtime.EmptyValue
import kwasm.runtime.IntValue
import kwasm.runtime.Table
import org.junit.runner.RunWith
import java.io.InputStream
import java.io.InputStreamReader

@RunWith(SpecTestRunner::class)
class CoreSpecTest {
    @SpecTest(
        subdir = "core",
        files = [
            "address.wast",
            "align.wast",
            "binary.wast",
            "binary-leb128.wast",
            "comments.wast",
            "const.wast",
            "conversions.wast",
            "custom.wast",
            "data.wast",
            "elem.wast",
            "endianness.wast",
            "exports.wast",
            "forward.wast",
            "int_exprs.wast",
            "int_literals.wast",
            "memory.wast",
            "memory_redundancy.wast",
            "return.wast",
            "token.wast",
            "traps.wast",
            "unreachable.wast",
        ],
    )
    fun scriptTest(input: InputStream, file: String) {
        val memProvider = ByteBufferMemoryProvider(4L * 1024 * 1024 * 1024)
        val builder = KWasmProgram.builder(memProvider)
        builder.withHostFunction(
            "spectest",
            "print_i32",
            HostFunction { p1: IntValue, _ -> println(p1.value); EmptyValue }
        )
        builder.withHostFunction(
            "spectest",
            "print",
            HostFunction { _ -> println("Print called"); EmptyValue }
        )
        builder.withHostMemory(
            "spectest",
            "memory",
            memProvider.buildMemory(1, 10)
        )
        builder.withHostTable("spectest", "table", Table(TableType(Limits(10, 20), ElementType.FunctionReference)))
        builder.withHostGlobal("spectest", "global_i32", 0)

        runScript(InputStreamReader(input), ParseContext(file), builder)
    }
}
