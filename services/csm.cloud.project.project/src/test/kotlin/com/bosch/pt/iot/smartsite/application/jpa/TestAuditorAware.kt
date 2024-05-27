/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.jpa

import com.bosch.pt.iot.smartsite.user.model.User
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

/** Auditor provider for testing purposes. */
class TestAuditorAware : AuditorAware<Any> {
  override fun getCurrentAuditor(): Optional<Any> {
    val authentication = SecurityContextHolder.getContext().authentication
    if (!isAuthenticated(authentication)) {
      return Optional.empty()
    }

    val user = authentication.principal as User?

    return if (user == null) {
      Optional.empty()
    } else if (user.isNew) {
      LOGGER.warn("Detected transient user {}", user)
      Optional.empty()
    } else {
      Optional.of(user)
    }
  }

  private fun isAuthenticated(authentication: Authentication?): Boolean =
      authentication != null &&
          authentication.isAuthenticated &&
          authentication !is AnonymousAuthenticationToken

  companion object {
    private val LOGGER = LoggerFactory.getLogger(TestAuditorAware::class.java)
  }
}
