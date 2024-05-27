/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.exceptions

open class AggregateNotFoundException
@JvmOverloads
constructor(val messageKey: String, val identifier: String, cause: Throwable? = null) :
    RuntimeException(messageKey, cause)
