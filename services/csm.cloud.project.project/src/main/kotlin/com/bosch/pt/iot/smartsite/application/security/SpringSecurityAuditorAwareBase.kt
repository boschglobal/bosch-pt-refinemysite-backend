/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.model.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

open class SpringSecurityAuditorAwareBase {

  fun getAuditor(): User? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (!isAuthenticated(authentication)) {
      return null
    }
    return authentication.principal as User?
  }

  private fun isAuthenticated(authentication: Authentication?): Boolean =
      authentication != null &&
          authentication.isAuthenticated &&
          authentication !is AnonymousAuthenticationToken
}
