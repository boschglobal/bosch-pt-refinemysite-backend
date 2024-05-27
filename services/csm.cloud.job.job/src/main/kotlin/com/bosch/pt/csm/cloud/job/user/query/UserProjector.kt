/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.query

import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class UserProjector(private val userProjectionRepository: UserProjectionRepository) {

  fun handle(event: UserEvent) {
    when (event) {
      is UserChangedEvent -> updateUserProjection(event)
      is UserDeletedEvent -> deleteUserProjection(event)
    }
  }

  private fun updateUserProjection(event: UserChangedEvent) {
    userProjectionRepository.save(
        UserProjection(event.userIdentifier, event.externalUserIdentifier, event.locale))
  }

  private fun deleteUserProjection(event: UserDeletedEvent) {
    userProjectionRepository.deleteById(event.userIdentifier)
  }
}

sealed class UserEvent(open val userIdentifier: UserIdentifier)

data class UserChangedEvent(
    override val userIdentifier: UserIdentifier,
    val externalUserIdentifier: ExternalUserIdentifier,
    val locale: Locale?
) : UserEvent(userIdentifier)

data class UserDeletedEvent(override val userIdentifier: UserIdentifier) :
    UserEvent(userIdentifier)

data class ExternalUserIdentifier(val value: String)
