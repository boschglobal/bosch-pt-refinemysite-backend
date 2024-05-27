/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization

import org.springframework.security.access.AccessDeniedException

class InsufficientPatScopeException(message: String, cause: Throwable? = null) :
    AccessDeniedException(message, cause)
