@file:UseSerializers(ColorSerializer::class)

package com.klyx.nodegraph

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.core.StandardNodeColors
import com.klyx.nodegraph.util.ColorSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.jvm.JvmName

@Serializable
sealed class PinType(val color: Color = Color.White) {
    @Serializable
    data object Flow : PinType(StandardNodeColors.Types.Exec)

    @Serializable
    data object Float : PinType(StandardNodeColors.Types.Float)

    @Serializable
    data object Integer : PinType(StandardNodeColors.Types.Integer)

    @Serializable
    data class String(val maxLines: Int = 1) : PinType(StandardNodeColors.Types.String) {
        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            return other is String
        }

        override fun hashCode() = this::class.hashCode()
    }

    @Serializable
    data object Boolean : PinType(StandardNodeColors.Types.Boolean)

    @Serializable
    data class Wildcard(
        val allowedTypes: List<PinType> = emptyList() // empty = allow all
    ) : PinType(StandardNodeColors.Types.AnyType) {
        fun accepts(type: PinType): kotlin.Boolean {
            if (allowedTypes.isEmpty()) return true
            // allow if the exact type matches, or if connecting another Wildcard
            return type in allowedTypes || type is Wildcard
        }
    }

    @Serializable
    data class Enum(
        val enumName: kotlin.String,
        val entries: List<kotlin.String>,
        val enumColor: Color = StandardNodeColors.Types.Enum
    ) : PinType(enumColor) {

        init {
            check(entries.isNotEmpty()) {
                "Enum entries cannot be empty."
            }
        }
    }

    @Serializable
    data class Custom(
        val typeName: kotlin.String,
        val typeColor: Color = StandardNodeColors.Types.ObjectReference
    ) : PinType(typeColor)

    companion object {
        private val builtinCasts: Map<Pair<PinType, PinType>, (Any?) -> Any?> = mapOf(
            (Integer to Float) to { v -> (v as? Number)?.toFloat() ?: 0f },
            (Float to Integer) to { v -> (v as? Number)?.toInt() ?: 0 },
            (Boolean to Integer) to { v -> if (v as? kotlin.Boolean == true) 1 else 0 },
            (Boolean to Float) to { v -> if (v as? kotlin.Boolean == true) 1f else 0f },
            (Integer to String()) to { v -> (v as? Number)?.toInt()?.toString() ?: "0" },
            (Float to String()) to { v -> (v as? Number)?.toFloat()?.toString() ?: "0.0" },
            (Boolean to String()) to { v -> (v as? kotlin.Boolean)?.toString() ?: "false" },
        )

        private val customCasts = HashMap<Pair<PinType, PinType>, (Any?) -> Any?>()

        /**
         * Registers an auto-cast rule between two types.
         */
        fun registerCast(
            from: PinType,
            to: PinType,
            cast: (Any?) -> Any?,
        ) {
            customCasts[from to to] = cast
        }

        /**
         * Registers bidirectional cast rules between two types.
         * Convenience for when both directions make sense.
         */
        fun registerCastBidirectional(
            typeA: PinType,
            typeB: PinType,
            aToB: (Any?) -> Any?,
            bToA: (Any?) -> Any?,
        ) {
            customCasts[typeA to typeB] = aToB
            customCasts[typeB to typeA] = bToA
        }

        /**
         * Unregisters a previously registered cast rule.
         */
        fun unregisterCast(from: PinType, to: PinType) {
            customCasts.remove(from to to)
        }

        /**
         * Returns true if [from] can be automatically cast to [to].
         */
        fun canAutoCast(from: PinType, to: PinType): kotlin.Boolean = when {
            from == Flow && to != Flow -> false // Flow cannot connect to data
            to == Flow && from != Flow -> false // data cannot connect to Flow
            from is Wildcard -> from.accepts(to)
            to is Wildcard -> to.accepts(from)
            from is String && to is String -> true
            from == to -> true // exact match
            (from to to) in builtinCasts -> true
            (from to to) in customCasts -> true
            else -> false
        }

        /**
         * Applies the registered cast from [from] to [to].
         * Returns original value if no cast is registered.
         */
        fun applyCast(value: Any?, from: PinType, to: PinType): Any? {
            if (from == to) return value
            val castFn = builtinCasts[from to to] ?: customCasts[from to to]
            return castFn?.invoke(value) ?: value
        }
    }
}

val PinType.typeName
    get() = when (this) {
        PinType.Boolean -> "Boolean"
        PinType.Float -> "Float"
        PinType.Flow -> "Exec"
        PinType.Integer -> "Int"
        is PinType.String -> "String"
        is PinType.Wildcard -> "Any"
        is PinType.Enum -> this.enumName
        is PinType.Custom -> this.typeName
    }

inline fun <reified T : Enum<T>> PinType.Companion.enum(
    color: Color = StandardNodeColors.Types.Enum
): PinType.Enum {
    val name = T::class.simpleName ?: "Enum"
    val entries = enumValues<T>().map { it.name }
    return PinType.Enum(enumName = name, entries = entries, enumColor = color)
}

inline fun <reified T : Enum<T>> enumPinType(
    color: Color = StandardNodeColors.Types.Enum
) = PinType.enum<T>(color)

fun customPinType(
    typeName: String,
    typeColor: Color = StandardNodeColors.Types.ObjectReference
) = PinType.Custom(typeName, typeColor)

@JvmName("_customPinType")
inline fun <reified T> customPinType(
    typeName: String = T::class.simpleName!!,
    typeColor: Color = StandardNodeColors.Types.ObjectReference
) = PinType.Custom(typeName, typeColor)

val BooleanType = PinType.Boolean
val FloatType = PinType.Float
val IntegerType = PinType.Integer
val FlowType = PinType.Flow
val StringType = PinType.String()
val AnyType = PinType.Wildcard()

@Suppress("FunctionName")
fun WildcardType(allowedTypes: List<PinType> = emptyList()) = PinType.Wildcard(allowedTypes)

@Suppress("FunctionName")
fun EnumType(
    enumName: String,
    entries: List<String>,
    enumColor: Color = StandardNodeColors.Types.Enum
) = PinType.Enum(enumName, entries, enumColor)

@Suppress("FunctionName")
fun CustomType(typeName: String, typeColor: Color = StandardNodeColors.Types.ObjectReference) =
    PinType.Custom(typeName, typeColor)
