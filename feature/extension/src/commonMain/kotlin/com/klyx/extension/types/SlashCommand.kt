package com.klyx.extension.types

/**
 * A slash command for use in the Assistant.
 */
data class SlashCommand(
    /**
     * The name of the slash command.
     */
    val name: String,

    /**
     * The description of the slash command.
     */
    val description: String,

    /**
     * The tooltip text to display for the run button.
     */
    val tooltipText: String,

    /**
     * Whether this slash command requires an argument.
     */
    val requiresArgument: Boolean,
)

/**
 * The output of a slash command.
 */
data class SlashCommandOutput(
    /**
     * The text produced by the slash command.
     */
    val text: String,

    /**
     * The list of sections to show in the slash command placeholder.
     */
    val sections: List<SlashCommandOutputSection>
)

/**
 * A section in the slash command output.
 */
data class SlashCommandOutputSection(
    /**
     * The range this section occupies.
     */
    val range: UIntRange,

    /**
     * The label to display in the placeholder for this section.
     */
    val label: String
)

/**
 * A completion for a slash command argument.
 */
data class SlashCommandArgumentCompletion(
    /**
     * The label to display for this completion.
     */
    val label: String,

    /**
     * The new text that should be inserted into the command when this completion is accepted.
     */
    val newText: String,

    /**
     * Whether the command should be run when accepting this completion.
     */
    val runCommand: Boolean,
)
