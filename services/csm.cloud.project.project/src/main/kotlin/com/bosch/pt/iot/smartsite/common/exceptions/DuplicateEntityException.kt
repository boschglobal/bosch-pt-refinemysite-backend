/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.exceptions

/** This exception indicates that an entity could not be created because it already exists. */
class DuplicateEntityException(val messageKey: String, override val cause: Throwable? = null) :
    RuntimeException(messageKey, cause)
