package com.klyx.lsp

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(WorkDoneProgressNotificationSerializer::class)
sealed interface WorkDoneProgressNotification {
    val kind: WorkDoneProgressKind
}

internal object WorkDoneProgressNotificationSerializer
    : JsonContentPolymorphicSerializer<WorkDoneProgressNotification>(WorkDoneProgressNotification::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<WorkDoneProgressNotification> {
        return when (val kind = element.jsonObject["kind"]?.jsonPrimitive?.content) {
            "begin" -> WorkDoneProgressBegin.serializer()
            "report" -> WorkDoneProgressReport.serializer()
            "end" -> WorkDoneProgressEnd.serializer()
            else -> error("Unknown WorkDoneProgressNotification kind: $kind")
        }
    }
}
