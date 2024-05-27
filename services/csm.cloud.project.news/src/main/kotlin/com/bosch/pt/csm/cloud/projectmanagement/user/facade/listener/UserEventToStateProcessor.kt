/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.NewsService
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

/**
 * This class is responsible for updating the local state based on a company event. The local state
 * contains the view of the event log required to determine news.
 */
@Component
class UserEventToStateProcessor(
    private val newsService: NewsService,
    private val userService: UserService
) {

  fun updateStateFromUserEvent(message: SpecificRecordBase?) {
    if (message is UserEventAvro) {
      updateFromUserEvent(message)
    }
  }

  fun deleteUser(userIdentifier: UUID) = userService.delete(userIdentifier)

  private fun updateFromUserEvent(message: UserEventAvro) {
    val userAggregate = message.aggregate

    if (message.name == UserEventEnumAvro.DELETED) {
      // User can only be deleted when they are not used yet in the origin service. Therefore, we
      // can also delete them here without any further actions or checks.
      newsService.deleteByUserIdentifier(userAggregate.aggregateIdentifier.identifier.toUUID())
      userService.delete(userAggregate.aggregateIdentifier.identifier.toUUID())
    } else {

      userService.createOrUpdate(
          userAggregate.userId,
          userAggregate.aggregateIdentifier.identifier.toUUID(),
          userAggregate.admin,
          userAggregate.locked)
    }
  }
}
