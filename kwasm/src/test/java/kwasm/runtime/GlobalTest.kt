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
import kwasm.ast.type.GlobalType
import kwasm.ast.type.ValueType
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class GlobalTest {
    @Test
    fun int_toUnsigned() {
        var int = Global.Int(10, true)
        assertThat(int.unsigned).isEqualTo(10u)

        int = Global.Int(-1, true)
        assertThat(int.unsigned).isEqualTo(UInt.MAX_VALUE)
    }

    @Test
    fun int_setUnsigned() {
        val int = Global.Int(10, true)
        int.unsigned = UInt.MAX_VALUE
        assertThat(int.value).isEqualTo(-1)
    }

    @Test
    fun long_toUnsigned() {
        var long = Global.Long(10L, true)
        assertThat(long.unsigned).isEqualTo(10uL)

        long = Global.Long(-1L, true)
        assertThat(long.unsigned).isEqualTo(ULong.MAX_VALUE)
    }

    @Test
    fun long_setUnsigned() {
        val long = Global.Long(10L, true)
        long.unsigned = ULong.MAX_VALUE
        assertThat(long.value).isEqualTo(-1)
    }

    @Test
    fun allocate_i32_throwsWhenValueIsNotInt() {
        val store = Store()

        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.I32, true), 42L)
        }.also { assertThat(it).hasMessageThat().contains("Expected I32 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.I32, true), 42.0f)
        }.also { assertThat(it).hasMessageThat().contains("Expected I32 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.I32, true), 42.0)
        }.also { assertThat(it).hasMessageThat().contains("Expected I32 value") }
    }

    @Test
    fun allocate_i32() {
        var store = Store()

        store = store.allocate(GlobalType(ValueType.I32, true), 42)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.globals).hasSize(1)
                assertThat(newStore.globals[0].value).isEqualTo(42)
                assertThat(newStore.globals[0].mutable).isTrue()

                newStore
            }
        store.allocate(GlobalType(ValueType.I32, false), 1337)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(1)
                assertThat(newStore.globals).hasSize(2)
                assertThat(newStore.globals[1].value).isEqualTo(1337)
                assertThat(newStore.globals[1].mutable).isFalse()

                newStore
            }
    }

    @Test
    fun allocate_i64_throwsWhenValueIsNotLong() {
        val store = Store()

        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.I64, true), 42)
        }.also { assertThat(it).hasMessageThat().contains("Expected I64 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.I64, true), 42.0f)
        }.also { assertThat(it).hasMessageThat().contains("Expected I64 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.I64, true), 42.0)
        }.also { assertThat(it).hasMessageThat().contains("Expected I64 value") }
    }

    @Test
    fun allocate_i64() {
        var store = Store()

        store = store.allocate(GlobalType(ValueType.I64, true), 42L)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.globals).hasSize(1)
                assertThat(newStore.globals[0].value).isEqualTo(42)
                assertThat(newStore.globals[0].mutable).isTrue()

                newStore
            }
        store.allocate(GlobalType(ValueType.I64, false), 1337L)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(1)
                assertThat(newStore.globals).hasSize(2)
                assertThat(newStore.globals[1].value).isEqualTo(1337L)
                assertThat(newStore.globals[1].mutable).isFalse()

                newStore
            }
    }

    @Test
    fun allocate_f32_throwsWhenValueIsNotLong() {
        val store = Store()

        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.F32, true), 42)
        }.also { assertThat(it).hasMessageThat().contains("Expected F32 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.F32, true), 42L)
        }.also { assertThat(it).hasMessageThat().contains("Expected F32 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.F32, true), 42.0)
        }.also { assertThat(it).hasMessageThat().contains("Expected F32 value") }
    }

    @Test
    fun allocate_f32() {
        var store = Store()

        store = store.allocate(GlobalType(ValueType.F32, true), 42.0f)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.globals).hasSize(1)
                assertThat(newStore.globals[0].value).isEqualTo(42.0f)
                assertThat(newStore.globals[0].mutable).isTrue()

                newStore
            }
        store.allocate(GlobalType(ValueType.F32, false), 1337.0f)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(1)
                assertThat(newStore.globals).hasSize(2)
                assertThat(newStore.globals[1].value).isEqualTo(1337.0f)
                assertThat(newStore.globals[1].mutable).isFalse()

                newStore
            }
    }

    @Test
    fun allocate_f64_throwsWhenValueIsNotLong() {
        val store = Store()

        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.F64, true), 42)
        }.also { assertThat(it).hasMessageThat().contains("Expected F64 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.F64, true), 42.0f)
        }.also { assertThat(it).hasMessageThat().contains("Expected F64 value") }
        assertThrows(IllegalArgumentException::class.java) {
            store.allocate(GlobalType(ValueType.F64, true), 42L)
        }.also { assertThat(it).hasMessageThat().contains("Expected F64 value") }
    }

    @Test
    fun allocate_f64() {
        var store = Store()

        store = store.allocate(GlobalType(ValueType.F64, true), 42.0)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.globals).hasSize(1)
                assertThat(newStore.globals[0].value).isEqualTo(42.0)
                assertThat(newStore.globals[0].mutable).isTrue()

                newStore
            }
        store.allocate(GlobalType(ValueType.F64, false), 1337.0)
            .let { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(1)
                assertThat(newStore.globals).hasSize(2)
                assertThat(newStore.globals[1].value).isEqualTo(1337.0)
                assertThat(newStore.globals[1].mutable).isFalse()

                newStore
            }
    }
}
