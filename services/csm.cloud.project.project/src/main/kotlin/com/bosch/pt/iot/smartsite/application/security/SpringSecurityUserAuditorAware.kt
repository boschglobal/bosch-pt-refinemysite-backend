/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.model.User
import java.util.Optional
import org.springframework.data.domain.AuditorAware

/** Spring data jpa auditing utility class to provide information about logged-in user. */
class SpringSecurityUserAuditorAware : SpringSecurityAuditorAwareBase(), AuditorAware<User> {

  override fun getCurrentAuditor(): Optional<User> =
      super.getAuditor()?.let { Optional.of(it) } ?: Optional.empty()
}
