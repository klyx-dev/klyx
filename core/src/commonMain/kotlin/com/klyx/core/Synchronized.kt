package com.klyx.core

import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
//@OptIn(ExperimentalMultiplatform::class)
//@OptionalExpectation
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
expect annotation class Synchronized()
