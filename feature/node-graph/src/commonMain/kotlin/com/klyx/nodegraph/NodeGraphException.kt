package com.klyx.nodegraph

open class NodeGraphException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class UnserializableTypeException(
    typeName: String,
    cause: Throwable
) : NodeGraphException(
    "Failed to register custom type '$typeName'. Ensure the class is annotated with @Serializable.",
    cause
)

class InvalidTypeArgumentException(
    typeName: String,
    cause: Throwable
) : NodeGraphException("Failed to register custom type '$typeName'. Star projections are not allowed.", cause)
