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

class NoOpAuditorAware : AuditorAware<UserProjection> {
  override fun getCurrentAuditor(): Optional<UserProjection> = Optional.empty()
}
