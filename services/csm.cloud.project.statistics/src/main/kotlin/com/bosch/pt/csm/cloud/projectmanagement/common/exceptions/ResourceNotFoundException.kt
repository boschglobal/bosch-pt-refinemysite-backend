/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.exceptions

/**
 * Constructor to initialize exception with a message key to translate and optional error cause.
 *
 * @param messageKey the message key to translate
 * @param cause the optional cause
 */
class ResourceNotFoundException(val messageKey: String, cause: Throwable?) :
    RuntimeException(cause)
