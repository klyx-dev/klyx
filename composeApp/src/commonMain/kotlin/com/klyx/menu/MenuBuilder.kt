package com.klyx.menu

import com.klyx.core.string
import org.jetbrains.compose.resources.StringResource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@DslMarker
private annotation class MenuBuilderDsl

@MenuBuilderDsl
class MenuBuilder {
    private val groups = mutableMapOf<String, List<MenuItem>>()

    @OptIn(ExperimentalTypeInference::class)
    fun group(
        title: String,
        @BuilderInference
        builder: MenuGroupBuilder.() -> Unit
    ) = apply {
        val groupBuilder = MenuGroupBuilder().apply(builder)
        groups[title] = groupBuilder.items
    }

    @OptIn(ExperimentalTypeInference::class)
    fun group(
        resource: StringResource,
        @BuilderInference
        builder: MenuGroupBuilder.() -> Unit
    ) = apply { group(string(resource), builder) }

    @OptIn(ExperimentalTypeInference::class)
    operator fun String.invoke(
        @BuilderInference
        builder: MenuGroupBuilder.() -> Unit
    ) = apply { group(this, builder) }

    fun build(): Map<String, List<MenuItem>> = groups
}

@MenuBuilderDsl
class MenuGroupBuilder {
    @PublishedApi
    internal val items = mutableListOf<MenuItem>()

    @OptIn(ExperimentalTypeInference::class)
    fun item(
        @BuilderInference builder: MenuItemBuilder.() -> Unit
    ) = apply { items += menuItem(builder) }

    @OptIn(ExperimentalTypeInference::class)
    fun item(
        title: String,
        @BuilderInference
        builder: MenuItemBuilder.() -> Unit
    ) = apply {
        items += menuItem {
            title(title)
            apply(builder)
        }
    }

    @OptIn(ExperimentalTypeInference::class)
    fun item(
        resource: StringResource,
        @BuilderInference
        builder: MenuItemBuilder.() -> Unit
    ) = apply {
        items += menuItem {
            title(resource)
            apply(builder)
        }
    }

    operator fun String.invoke(onClick: suspend () -> Unit) = item {
        title(this@invoke)
        onClick { onClick() }
    }

    fun divider() = item { divider() }
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun menu(
    @BuilderInference builder: MenuBuilder.() -> Unit
): Map<String, List<MenuItem>> {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return MenuBuilder().apply(builder).build()
}
