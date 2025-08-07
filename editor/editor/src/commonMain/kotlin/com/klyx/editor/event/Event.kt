package com.klyx.editor.event

sealed class Event

data class ContentChangeEvent(
    val changedText: CharSequence
) : Event()
