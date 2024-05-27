/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanStateFromUserDeletedEvent(private val userService: UserService) :
    AbstractStateStrategy<UserEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    val key = record.key
    val value = record.value

    val isTombstoneMessage =
        value == null &&
            key is AggregateEventMessageKey &&
            key.aggregateIdentifier.type == USER.value
    val isDeletedMessage = value is UserEventAvro && value.name == DELETED

    return isTombstoneMessage || isDeletedMessage
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: UserEventAvro) =
      userService.deleteUser(messageKey.rootContextIdentifier)

  @Trace
  override fun updateStateForTombstone(messageKey: EventMessageKey) =
      userService.deleteUser(messageKey.rootContextIdentifier)
}
