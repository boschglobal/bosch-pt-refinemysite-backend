/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationError
import org.springframework.http.HttpStatus

/**
 * Translation of [org.springframework.security.oauth2.server.resource.BearerTokenError] for PATs.
 */
class PatTokenError(
    val httpStatus: HttpStatus,
    errorCode: String,
    description: String? = null,
    uri: String? = null
) : PatAuthenticationError(errorCode, description, uri)
