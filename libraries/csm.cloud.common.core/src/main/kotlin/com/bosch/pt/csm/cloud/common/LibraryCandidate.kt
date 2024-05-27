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

/** This annotation marks a class to be a candidate for extraction into a library */
@Retention(SOURCE)
@Target(CLASS, FUNCTION)
annotation class LibraryCandidate(val explanation: String = "", val library: String = "")
