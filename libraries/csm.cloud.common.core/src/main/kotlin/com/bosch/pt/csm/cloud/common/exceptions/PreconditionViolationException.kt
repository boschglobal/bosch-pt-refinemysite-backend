/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.exceptions

open class PreconditionViolationException
@JvmOverloads
constructor(val messageKey: String, val logMessage: String? = null, cause: Throwable? = null) :
    RuntimeException(messageKey, cause)
