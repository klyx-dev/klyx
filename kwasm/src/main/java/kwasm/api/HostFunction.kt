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

@file:Suppress("FunctionName")

package kwasm.api

import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.ValueType
import kwasm.runtime.EmptyValue
import kwasm.runtime.Memory
import kwasm.runtime.Value
import kwasm.runtime.toValueType
import kotlin.reflect.KClass
import kwasm.ast.type.Result as AstResult

/**
 * Base for all host-provided functions exposed to WebAssembly programs via imports.
 */
interface HostFunction<ReturnType : Value<*>> {
    /** The classes of the required parameters for the function. */
    val parameterTypes: List<KClass<out Value<*>>>

    /** The class of the [ReturnType] of this [HostFunction]. */
    val returnType: KClass<ReturnType>?

    operator fun invoke(params: List<Value<*>>, context: HostFunctionContext): ReturnType
}

/** Additional context passed to a [HostFunction] at invocation-time. */
interface HostFunctionContext {
    val memory: Memory?
}

/**
 * Creates a [HostFunction] which accepts no parameters and returns no value. (ie. [Unit])
 */
fun UnitHostFunction(
    block: (HostFunctionContext) -> Unit
): HostFunction<EmptyValue> = object : HostFunction<EmptyValue> {
    override val parameterTypes: List<KClass<out Value<*>>> = emptyList()
    override val returnType: KClass<EmptyValue>? = null
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): EmptyValue {
        block(context)
        return EmptyValue
    }
}

/**
 * Creates a [HostFunction] which accepts a single parameter ([P1]) and returns no value. (ie.
 * [Unit])
 */
inline fun <reified P1> UnitHostFunction(
    noinline block: (P1, HostFunctionContext) -> Unit
): HostFunction<EmptyValue> where P1 : Value<*> = object : HostFunction<EmptyValue> {
    override val parameterTypes: List<KClass<out Value<*>>> = listOf(P1::class)
    override val returnType: KClass<EmptyValue>? = null
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): EmptyValue {
        val p1 = requireParam<P1>(params, 0)
        block(p1, context)
        return EmptyValue
    }
}

/**
 * Creates a [HostFunction] which accepts two parameters ([P1] and [P2]) and returns no value.
 * (ie. [Unit])
 */
inline fun <reified P1, reified P2> UnitHostFunction(
    noinline block: (P1, P2, HostFunctionContext) -> Unit
): HostFunction<EmptyValue> where P1 : Value<*>,
                                  P2 : Value<*> = object : HostFunction<EmptyValue> {
    override val parameterTypes: List<KClass<out Value<*>>> = listOf(P1::class, P2::class)
    override val returnType: KClass<EmptyValue>? = null
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): EmptyValue {
        val p1 = requireParam<P1>(params, 0)
        val p2 = requireParam<P2>(params, 1)
        block(p1, p2, context)
        return EmptyValue
    }
}

/**
 * Creates a [HostFunction] which accepts three parameters ([P1], [P2], and [P3]) and returns no
 * value. (ie. [Unit])
 */
inline fun <reified P1, reified P2, reified P3> UnitHostFunction(
    noinline block: (P1, P2, P3, HostFunctionContext) -> Unit
): HostFunction<EmptyValue> where P1 : Value<*>,
                                  P2 : Value<*>,
                                  P3 : Value<*> = object : HostFunction<EmptyValue> {
    override val parameterTypes: List<KClass<out Value<*>>> =
        listOf(P1::class, P2::class, P3::class)
    override val returnType: KClass<EmptyValue>? = null
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): EmptyValue {
        val p1 = requireParam<P1>(params, 0)
        val p2 = requireParam<P2>(params, 1)
        val p3 = requireParam<P3>(params, 2)
        block(p1, p2, p3, context)
        return EmptyValue
    }
}

/**
 * Creates a [HostFunction] which accepts no parameters and returns a value of type [ReturnType].
 */
inline fun <reified ReturnType> HostFunction(
    crossinline block: (HostFunctionContext) -> ReturnType
): HostFunction<ReturnType> where ReturnType : Value<*> = object : HostFunction<ReturnType> {
    override val parameterTypes: List<KClass<out Value<*>>> = emptyList()
    override val returnType: KClass<ReturnType>? = ReturnType::class
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): ReturnType =
        block(context)
}

/**
 * Creates a [HostFunction] which accepts a single parameter ([P1]) and returns a value of type
 * [ReturnType].
 */
inline fun <reified P1, reified ReturnType> HostFunction(
    crossinline block: (P1, HostFunctionContext) -> ReturnType
): HostFunction<ReturnType> where P1 : Value<*>,
                                  ReturnType : Value<*> = object : HostFunction<ReturnType> {
    override val parameterTypes: List<KClass<out Value<*>>> = listOf(P1::class)
    override val returnType: KClass<ReturnType>? = ReturnType::class
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): ReturnType {
        val p1 = requireParam<P1>(params, 0)
        return block(p1, context)
    }
}

/**
 * Creates a [HostFunction] which accepts two parameters ([P1] and [P2]) and returns a value of
 * type [ReturnType].
 */
inline fun <reified P1, reified P2, reified ReturnType> HostFunction(
    crossinline block: (P1, P2, HostFunctionContext) -> ReturnType
): HostFunction<ReturnType> where P1 : Value<*>,
                                  P2 : Value<*>,
                                  ReturnType : Value<*> = object : HostFunction<ReturnType> {
    override val parameterTypes: List<KClass<out Value<*>>> = listOf(P1::class, P2::class)
    override val returnType: KClass<ReturnType>? = ReturnType::class
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): ReturnType {
        val p1 = requireParam<P1>(params, 0)
        val p2 = requireParam<P2>(params, 1)
        return block(p1, p2, context)
    }
}

/**
 * Creates a [HostFunction] which accepts three parameters ([P1], [P2], and [P3]) and returns a
 * value of type [ReturnType].
 */
inline fun <reified P1, reified P2, reified P3, reified ReturnType> HostFunction(
    crossinline block: (P1, P2, P3, HostFunctionContext) -> ReturnType
): HostFunction<ReturnType> where P1 : Value<*>,
                                  P2 : Value<*>,
                                  P3 : Value<*>,
                                  ReturnType : Value<*> = object : HostFunction<ReturnType> {
    override val parameterTypes: List<KClass<out Value<*>>> =
        listOf(P1::class, P2::class, P3::class)
    override val returnType: KClass<ReturnType>? = ReturnType::class
    override fun invoke(params: List<Value<*>>, context: HostFunctionContext): ReturnType {
        val p1 = requireParam<P1>(params, 0)
        val p2 = requireParam<P2>(params, 1)
        val p3 = requireParam<P3>(params, 2)
        return block(p1, p2, p3, context)
    }
}

/** Intended for internal use only. */
inline fun <reified ValueType : Value<*>> requireParam(
    params: List<Value<*>>,
    position: Int
) = requireNotNull(params.getOrNull(position) as? ValueType) {
    "Parameter at position ${position + 1} is null or is not of expected type: ${ValueType::class}"
}

/** The [FunctionType] of the receiving [HostFunction]. */
internal val HostFunction<*>.functionType: FunctionType
    get() = FunctionType(
        parameterTypes.mapIndexed { index, kClass ->
            Param(
                Identifier.Local(null, index),
                requireNotNull(kClass.toValueType())
            )
        },
        returnType?.takeIf { it != EmptyValue::class }
            ?.let { listOf(AstResult(requireNotNull(it.toValueType()))) }
            ?: emptyList()
    )
