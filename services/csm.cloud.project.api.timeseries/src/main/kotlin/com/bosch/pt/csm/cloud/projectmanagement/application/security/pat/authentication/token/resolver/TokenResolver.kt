/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.authentication.AuthenticationConverter

interface TokenResolver {

  fun resolve(request: HttpServletRequest): String?

  fun setAuthenticationConverter(authenticationConverter: AuthenticationConverter)
}
