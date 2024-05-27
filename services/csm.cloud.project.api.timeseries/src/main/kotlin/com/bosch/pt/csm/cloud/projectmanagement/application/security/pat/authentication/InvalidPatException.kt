/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrors.invalidToken

/**
 * Translation of [org.springframework.security.oauth2.server.resource.InvalidBearerTokenException]
 * for PATs.
 */
class InvalidPatException(description: String, cause: Throwable? = null) :
    PatAuthenticationException(invalidToken(description), cause)
