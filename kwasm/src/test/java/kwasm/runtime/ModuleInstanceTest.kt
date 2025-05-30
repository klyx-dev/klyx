/*
 * Copyright 2020 Google LLC
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

package kwasm.runtime

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.api.ByteBufferMemoryProvider
import kwasm.ast.Identifier
import kwasm.ast.astNodeListOf
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.ValueType
import kwasm.ast.util.toFunctionIndex
import kwasm.ast.util.toGlobalIndex
import kwasm.ast.util.toMemoryIndex
import kwasm.ast.util.toTableIndex
import kwasm.runtime.memory.ByteBufferMemory
import kwasm.validation.module.validate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModuleInstanceTest {
    @get:Rule
    val parser = ParseRule()

    private val memoryProvider = ByteBufferMemoryProvider(1024 * 1024)

    @Test
    fun allocate_handlesImportedFunctionsCorrectly() = parser.with {
        val module =
            """
            (module
                (func $1 (import "foo" "bar") (param i32) (result i32))
                (func $2 (result i32)
                    i32.const 15
                    call $1
                )
            )
        """.parseModule()

        val context = module.validate()
        val externVals = module.collectImportExterns()
        val (_, moduleAllocation) = module.allocate(context, memoryProvider, externVals)

        assertThat(moduleAllocation.functionAddresses).hasSize(2)
        assertThat(moduleAllocation.functionAddresses["$1".toFunctionIndex()]).isNotNull()
        assertThat(moduleAllocation.functionAddresses["$1".toFunctionIndex()]?.needsInit()).isTrue()
        assertThat(moduleAllocation.functionAddresses["$2".toFunctionIndex()]).isNotNull()
        assertThat(moduleAllocation.functionAddresses["$2".toFunctionIndex()]?.needsInit())
            .isFalse()
    }

    @Test
    fun allocate_handlesImportedFunctionsCorrectly_invertedOrder() = parser.with {
        val module =
            """
            (module
                (func $2 (result i32)
                    i32.const 15
                    call $1
                )
                (func ${'$'}1 (import "foo" "bar") (param i32) (result i32))
            )
        """.parseModule()

        val context = module.validate()
        val externVals = module.collectImportExterns()
        val (_, moduleAllocation) = module.allocate(context, memoryProvider, externVals)

        assertThat(moduleAllocation.functionAddresses).hasSize(2)
        assertThat(moduleAllocation.functionAddresses["$1".toFunctionIndex()]).isNotNull()
        assertThat(moduleAllocation.functionAddresses["$1".toFunctionIndex()]?.needsInit()).isTrue()
        assertThat(moduleAllocation.functionAddresses["$2".toFunctionIndex()]).isNotNull()
        assertThat(moduleAllocation.functionAddresses["$2".toFunctionIndex()]?.needsInit())
            .isFalse()

        assertThat(moduleAllocation.functionAddresses[0])
            .isSameInstanceAs(moduleAllocation.functionAddresses["$1".toFunctionIndex()])
        assertThat(moduleAllocation.functionAddresses[1])
            .isSameInstanceAs(moduleAllocation.functionAddresses["$2".toFunctionIndex()])
    }

    @Test
    fun allocate_handlesImportedTablesCorrectly() = parser.with {
        val module =
            """
            (module
                (table $1 (import "foo" "bar") 0 funcref)
            )
        """.parseModule()

        val context = module.validate()
        val externVals = module.collectImportExterns()
        val (_, moduleAllocation) = module.allocate(context, memoryProvider, externVals)

        assertThat(moduleAllocation.tableAddresses).hasSize(1)
        assertThat(moduleAllocation.tableAddresses["$1".toTableIndex()]).isNotNull()
        assertThat(moduleAllocation.tableAddresses["$1".toTableIndex()]?.needsInit()).isTrue()
    }

    @Test
    fun allocate_handlesImportedMemoriesCorrectly() = parser.with {
        val module =
            """
            (module
                (memory $1 (import "foo" "bar") 0)
            )
        """.parseModule()

        val context = module.validate()
        val externVals = module.collectImportExterns()
        val (_, moduleAllocation) = module.allocate(context, memoryProvider, externVals)

        assertThat(moduleAllocation.memoryAddresses).hasSize(1)
        assertThat(moduleAllocation.memoryAddresses["$1".toMemoryIndex()]).isNotNull()
        assertThat(moduleAllocation.memoryAddresses["$1".toMemoryIndex()]?.needsInit()).isTrue()
    }

    @Test
    fun allocate_handlesImportedGlobalsCorrectly() = parser.with {
        val module =
            """
            (module
                (global $1 (import "foo" "bar") (mut i32))
                (global $2 (mut i32) i32.const 1)
            )
        """.parseModule()

        val context = module.validate()
        val externVals = module.collectImportExterns()
        val (_, moduleAllocation) = module.allocate(context, memoryProvider, externVals)

        assertThat(moduleAllocation.globalAddresses).hasSize(2)
        assertThat(moduleAllocation.globalAddresses["$1".toGlobalIndex()]).isNotNull()
        assertThat(moduleAllocation.globalAddresses["$1".toGlobalIndex()]?.needsInit()).isTrue()
        assertThat(moduleAllocation.globalAddresses["$2".toGlobalIndex()]).isNotNull()
        assertThat(moduleAllocation.globalAddresses["$2".toGlobalIndex()]?.needsInit())
            .isFalse()
    }

    @Test
    fun allocate_handlesImportedGlobalsCorrectly_invertedOrder() = parser.with {
        val module =
            """
            (module
                (global $2 (mut i32) i32.const 1)
                (global ${'$'}1 (import "foo" "bar") (mut i32))
            )
        """.parseModule()

        val context = module.validate()
        val externVals = module.collectImportExterns()
        val (_, moduleAllocation) = module.allocate(context, memoryProvider, externVals)

        assertThat(moduleAllocation.globalAddresses).hasSize(2)
        assertThat(moduleAllocation.globalAddresses["$1".toGlobalIndex()]).isNotNull()
        assertThat(moduleAllocation.globalAddresses["$1".toGlobalIndex()]?.needsInit()).isTrue()
        assertThat(moduleAllocation.globalAddresses["$2".toGlobalIndex()]).isNotNull()
        assertThat(moduleAllocation.globalAddresses["$2".toGlobalIndex()]?.needsInit())
            .isFalse()

        assertThat(moduleAllocation.globalAddresses[0])
            .isSameInstanceAs(moduleAllocation.globalAddresses["$1".toGlobalIndex()])
        assertThat(moduleAllocation.globalAddresses[1])
            .isSameInstanceAs(moduleAllocation.globalAddresses["$2".toGlobalIndex()])
    }

    @Test
    fun allocate_allocates_exportedFunctionsCorrectly() = parser.with {
        val module =
            """
            (module
                (func $1 (export "bar") (param i32))
                (func $0 (export "foo"))
            )
        """.parseModule()

        val context = module.validate()
        val (store, moduleAllocation) = module.allocate(context, memoryProvider)

        assertThat(store.functions).hasSize(2)
        assertThat(moduleAllocation.functionAddresses).hasSize(2)
        assertThat(moduleAllocation.exports).hasSize(2)

        // First function.
        FunctionType(
            astNodeListOf(
                Param(Identifier.Local(null, 0), ValueType.I32)
            ),
            astNodeListOf()
        ).let {
            val storeFunc = store.functions[0] as FunctionInstance.Module
            assertThat(storeFunc.type).isEqualTo(it)
            assertThat(storeFunc.code).isEqualTo(module.functions[0])

            // By address
            assertThat(
                store.functions[moduleAllocation.functionAddresses["\$1".toFunctionIndex()]!!.value]
            ).isEqualTo(storeFunc)
            assertThat(store.functions[moduleAllocation.functionAddresses[0].value])
                .isEqualTo(storeFunc)
            // Check the export
            assertThat(moduleAllocation.exports[0].name).isEqualTo("bar")
            assertThat(store.functions[moduleAllocation.exports[0].address.value])
                .isEqualTo(storeFunc)
        }

        FunctionType(astNodeListOf(), astNodeListOf()).let {
            val storeFunc = store.functions[1] as FunctionInstance.Module
            assertThat(storeFunc.type).isEqualTo(it)
            assertThat(storeFunc.code).isEqualTo(module.functions[1])

            // By address
            assertThat(
                store.functions[moduleAllocation.functionAddresses["\$0".toFunctionIndex()]!!.value]
            ).isEqualTo(storeFunc)
            assertThat(store.functions[moduleAllocation.functionAddresses[1].value])
                .isEqualTo(storeFunc)
            // Check the export
            assertThat(moduleAllocation.exports[1].name).isEqualTo("foo")
            assertThat(store.functions[moduleAllocation.exports[1].address.value])
                .isEqualTo(storeFunc)
        }
    }

    @Test
    fun allocate_allocates_exportedTableCorrectly() = parser.with {
        val module =
            """
            (module
                (table (export "myTable") 0 1 funcref)
            )
        """.parseModule()

        val context = module.validate()
        val (store, moduleAllocation) = module.allocate(context, memoryProvider)

        assertThat(store.tables).hasSize(1)
        assertThat(moduleAllocation.tableAddresses).hasSize(1)

        assertThat(store.tables[0].maxSize).isEqualTo(1)
        assertThat(store.tables[0].elements).isEmpty()

        // By Address
        assertThat(store.tables[moduleAllocation.tableAddresses[0].value])
            .isEqualTo(store.tables[0])
        // Check the export
        val export = moduleAllocation.exports[0] as Export.Table
        assertThat(export.address).isEqualTo(moduleAllocation.tableAddresses[0])
        assertThat(export.name).isEqualTo("myTable")
    }

    @Test
    fun allocate_allocates_exportedMemoryCorrectly() = parser.with {
        val module =
            """
            (module
                (memory (export "myMem") 1 5)
            )
        """.parseModule()

        val context = module.validate()
        val (store, moduleAllocation) = module.allocate(context, memoryProvider)

        assertThat(store.memories).hasSize(1)
        assertThat(moduleAllocation.memoryAddresses).hasSize(1)

        val memory = store.memories[0] as ByteBufferMemory
        val memoryAddress = moduleAllocation.memoryAddresses[0]
        val memoryExport = moduleAllocation.exports[0]
        val exportedMemory = store.memories[memoryExport.address.value]

        assertThat(memoryExport.address).isEqualTo(memoryAddress)
        assertThat(memory.sizePages).isEqualTo(1)
        assertThat(memory.maximumPages).isEqualTo(5)
        assertThat(exportedMemory).isSameInstanceAs(memory)
    }

    @Test
    fun allocate_allocates_exportedGlobalsCorrectly() = parser.with {
        val module =
            """
            (module
                (global ${'$'}globalOne (export "globalOne") i32 (i32.const -10))
                (global ${'$'}globalTwo (export "globalTwo") (mut i64) (i64.const 10))
                (global ${'$'}globalThree (export "globalThree") f32 (f32.const -1.5))
                (global ${'$'}globalFour (export "globalFour") (mut f64) (f64.const 1.5))
            )
        """.parseModule()

        val context = module.validate()
        val (store, moduleAllocation) = module.allocate(context, memoryProvider)

        assertThat(store.globals).hasSize(4)
        assertThat(moduleAllocation.globalAddresses).hasSize(4)

        val globalOne = store.globals[0] as Global.Int
        val globalOneAddress = moduleAllocation.globalAddresses[0]
        val globalOneExport = moduleAllocation.exports[0]
        val exportedGlobalOne = store.globals[globalOneExport.address.value]
        assertThat(globalOne.value).isEqualTo(0) // Not initialized yet.
        assertThat(globalOne.mutable).isFalse()
        assertThat(globalOneExport.address).isEqualTo(globalOneAddress)
        assertThat(exportedGlobalOne).isSameInstanceAs(globalOne)
        assertThat(moduleAllocation.globalAddresses["\$globalOne".toGlobalIndex()])
            .isEqualTo(globalOneAddress)

        val globalTwo = store.globals[1] as Global.Long
        val globalTwoAddress = moduleAllocation.globalAddresses[1]
        val globalTwoExport = moduleAllocation.exports[1]
        val exportedGlobalTwo = store.globals[globalTwoExport.address.value]
        assertThat(globalTwo.value).isEqualTo(0) // Not initialized yet.
        assertThat(globalTwo.mutable).isTrue()
        assertThat(globalTwoExport.address).isEqualTo(globalTwoAddress)
        assertThat(exportedGlobalTwo).isSameInstanceAs(globalTwo)
        assertThat(moduleAllocation.globalAddresses["\$globalTwo".toGlobalIndex()])
            .isEqualTo(globalTwoAddress)

        val globalThree = store.globals[2] as Global.Float
        val globalThreeAddress = moduleAllocation.globalAddresses[2]
        val globalThreeExport = moduleAllocation.exports[2]
        val exportedGlobalThree = store.globals[globalThreeExport.address.value]
        assertThat(globalThree.value).isEqualTo(0.0f) // Not Initialized yet.
        assertThat(globalThree.mutable).isFalse()
        assertThat(globalThreeExport.address).isEqualTo(globalThreeAddress)
        assertThat(exportedGlobalThree).isSameInstanceAs(globalThree)
        assertThat(moduleAllocation.globalAddresses["\$globalThree".toGlobalIndex()])
            .isEqualTo(globalThreeAddress)

        val globalFour = store.globals[3] as Global.Double
        val globalFourAddress = moduleAllocation.globalAddresses[3]
        val globalFourExport = moduleAllocation.exports[3]
        val exportedGlobalFour = store.globals[globalFourExport.address.value]
        assertThat(globalFour.value).isEqualTo(0.0) // Not initialized yet.
        assertThat(globalFour.mutable).isTrue()
        assertThat(globalFourExport.address).isEqualTo(globalFourAddress)
        assertThat(exportedGlobalFour).isSameInstanceAs(globalFour)
        assertThat(moduleAllocation.globalAddresses["\$globalFour".toGlobalIndex()])
            .isEqualTo(globalFourAddress)
    }
}
