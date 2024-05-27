/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.user.user.query.UserProjection
import java.util.Optional
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

/** Spring data jpa auditing utility class to provide information about logged-in user. */
class SpringSecurityAuditorAware : AuditorAware<UserProjection> {

  override fun getCurrentAuditor(): Optional<UserProjection> {
    val authentication = SecurityContextHolder.getContext().authentication
    if (!isAuthenticated(authentication)) {
      return Optional.empty()
    }

    val user = authentication.principal as UserProjection? ?: return Optional.empty()

    return Optional.of(user)
  }

  private fun isAuthenticated(authentication: Authentication?): Boolean =
      authentication != null &&
          authentication.isAuthenticated &&
          authentication !is AnonymousAuthenticationToken
}
