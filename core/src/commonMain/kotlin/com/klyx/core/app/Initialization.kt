package com.klyx.core.app

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class InitStep(
    val label: String,
    val status: InitStepStatus = InitStepStatus.Pending,
)

enum class InitStepStatus {
    Pending,
    Running,
    Done,
    Failed
}

@Stable
data class InitializationState(
    val steps: List<InitStep> = emptyList(),
    val currentStepIndex: Int = -1,
    val isComplete: Boolean = false,
    val error: Throwable? = null,
) {
    val currentLabel: String
        get() = steps.getOrNull(currentStepIndex)?.label ?: "Initializing..."

    val progress: Float
        get() = if (steps.isEmpty()) 0f else (steps.count { it.status == InitStepStatus.Done }).toFloat() / steps.size
}

object Initialization {
    val state: StateFlow<InitializationState>
        field = MutableStateFlow(InitializationState())

    fun defineSteps(vararg labels: String) {
        state.update {
            it.copy(steps = labels.map { label -> InitStep(label) })
        }
    }

    fun beginStep(index: Int) {
        state.update {
            it.copy(
                currentStepIndex = index,
                steps = it.steps.mapIndexed { i, step ->
                    when {
                        i == index -> step.copy(status = InitStepStatus.Running)
                        i < index -> step.copy(status = InitStepStatus.Done)
                        else -> step
                    }
                }
            )
        }
    }

    fun completeStep(index: Int) {
        state.update {
            it.copy(
                steps = it.steps.mapIndexed { i, step ->
                    if (i == index) step.copy(status = InitStepStatus.Done) else step
                }
            )
        }
    }

    fun failStep(index: Int, error: Throwable) {
        state.update {
            it.copy(
                steps = it.steps.mapIndexed { i, step ->
                    if (i == index) step.copy(status = InitStepStatus.Failed) else step
                },
                error = error,
            )
        }
    }

    fun complete() {
        state.update {
            it.copy(
                isComplete = true,
                steps = it.steps.map { step -> step.copy(status = InitStepStatus.Done) },
            )
        }
    }
}
