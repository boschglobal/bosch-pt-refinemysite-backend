/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.AuditorAware

/** Spring data jpa auditing utility class to provide information about logged-in user. */
class SpringSecurityUuidAuditorAware : SpringSecurityAuditorAwareBase(), AuditorAware<UUID> {

  override fun getCurrentAuditor(): Optional<UUID> =
      super.getAuditor()?.let { Optional.of(requireNotNull(it.identifier)) } ?: Optional.empty()
}
