/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener

import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.UserService
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.DELETED
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserEventToStateProcessor(private val userService: UserService) {

  @Transactional
  fun updateStateFromUserEvent(message: SpecificRecordBase?) {
    when (message) {
      is UserEventAvro -> updateFromUserEvent(message)
    }
  }

  @Transactional
  fun deleteUser(userIdentifier: UUID) {
    userService.delete(userIdentifier)
  }

  private fun updateFromUserEvent(message: UserEventAvro) {

    val userAggregate = message.getAggregate()

    // User can only be deleted when they are not used yet in the origin service. Therefore, we
    // can also delete them here without any further actions or checks.
    when (message.getName()) {
      DELETED -> userService.delete(userAggregate.getAggregateIdentifier().toUUID())
      else ->
          userService.createOrUpdate(
              userAggregate.getUserId(),
              userAggregate.getAggregateIdentifier().toUUID(),
              userAggregate.getAdmin(),
              userAggregate.getLocked(),
              userAggregate.getLocale()?.let { LocaleUtils.toLocale(it) })
    }
  }
}
