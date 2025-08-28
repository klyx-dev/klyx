package com.klyx.di

import com.klyx.filetree.FileTreeViewModel
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.StatusBarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonModule = module {
    viewModelOf(::EditorViewModel)
    viewModelOf(::KlyxViewModel)
    viewModelOf(::FileTreeViewModel)
    viewModelOf(::StatusBarViewModel)
}
