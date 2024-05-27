/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common.exceptions

/**
 * Indicates that an operation referencing another entity cannot be completed as the referenced
 * entity cannot be found.
 */
class ReferencedEntityNotFoundException(messageKey: String, override val cause: Throwable? = null) :
    RuntimeException(messageKey, cause)
