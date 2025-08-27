package com.klyx.core.atomic

import kotlinx.atomicfu.atomic

class AtomicList<E>(initial: List<E> = emptyList()) : MutableList<E> {
    private val _list = atomic(initial.toMutableList())

    private inline fun update(transform: (MutableList<E>) -> Unit): MutableList<E> {
        while (true) {
            val current = _list.value
            val copy = current
            transform(copy)
            if (_list.compareAndSet(current, copy)) return copy
        }
    }

    override val size get() = _list.value.size

    override fun isEmpty(): Boolean = _list.value.isEmpty()

    override fun contains(element: E) = _list.value.contains(element)

    override fun containsAll(elements: Collection<E>) = _list.value.containsAll(elements)

    override fun get(index: Int): E = _list.value[index]

    override fun indexOf(element: E): Int = _list.value.indexOf(element)

    override fun lastIndexOf(element: E): Int = _list.value.lastIndexOf(element)

    override fun iterator(): MutableIterator<E> = _list.value.iterator()

    override fun listIterator() = _list.value.listIterator()

    override fun listIterator(index: Int) = _list.value.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = _list.value.subList(fromIndex, toIndex)

    override fun add(element: E) = update { it.add(element) }.let { true }

    override fun add(index: Int, element: E) {
        update { it.add(index, element) }
    }

    override fun addAll(elements: Collection<E>) = update { it.addAll(elements) }.let { elements.isNotEmpty() }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        return update { it.addAll(index, elements) }.let { elements.isNotEmpty() }
    }

    override fun remove(element: E): Boolean {
        var removed = false
        update { removed = it.remove(element) }
        return removed
    }

    override fun removeAt(index: Int): E {
        var removed: E? = null
        update { removed = it.removeAt(index) }
        return removed!!
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var changed = false
        update { changed = it.removeAll(elements) }
        return changed
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var changed = false
        update { changed = it.retainAll(elements) }
        return changed
    }

    override fun clear() {
        update { it.clear() }
    }

    override fun set(index: Int, element: E): E {
        var old: E? = null
        update { old = it.set(index, element) }
        return old!!
    }
}

fun <E> atomicListOf(vararg elements: E) = AtomicList(if (elements.isNotEmpty()) elements.asList() else emptyList())
fun <E> atomicListOf() = AtomicList<E>()
