/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.model.User
import java.util.Optional
import org.springframework.data.domain.AuditorAware

class NoOpAuditorAware : AuditorAware<User> {

  override fun getCurrentAuditor(): Optional<User> = Optional.empty()
}
