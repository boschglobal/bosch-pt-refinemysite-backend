/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.exceptions

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException

/**
 * Exception thrown when an identifier type is passed to a batch endpoint where id types are not
 * supported
 */
class BatchIdentifierTypeNotSupportedException(messageKey: String) :
    PreconditionViolationException(messageKey)
