/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * This annotation marks code to be a test data scenario that can be used to set up integration
 * tests
 */
@Retention(SOURCE)
@Target(CLASS, FUNCTION)
annotation class TestDataScenario(val explanation: String = "")
