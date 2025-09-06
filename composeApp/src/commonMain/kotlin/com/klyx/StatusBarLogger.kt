package com.klyx

import com.klyx.core.logging.Logger
import com.klyx.core.logging.Message
import com.klyx.viewmodel.StatusBarViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object StatusBarLogger : Logger, KoinComponent {
    private val viewModel: StatusBarViewModel by inject()

    override fun log(message: Message) {
        viewModel.setCurrentLogMessage(message)
    }
}
