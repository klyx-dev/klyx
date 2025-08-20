package com.klyx.core

import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
expect annotation class Synchronized()
