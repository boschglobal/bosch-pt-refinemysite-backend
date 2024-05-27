/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.user.service.UserService
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CleanStateFromUserDeletedEvent(private val userService: UserService) :
    AbstractStateStrategy<UserEventAvro>(), CleanUpStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean {
    val isTombstoneMessage =
        value == null &&
            key is AggregateEventMessageKey &&
            key.aggregateIdentifier.type == USER.value
    val isDeletedMessage = value is UserEventAvro && value.getName() == DELETED

    return isTombstoneMessage || isDeletedMessage
  }

  @Trace
  override fun updateState(key: EventMessageKey, event: UserEventAvro) =
      userService.delete(key.rootContextIdentifier)

  override fun updateStateForTombstone(key: EventMessageKey) =
      userService.delete(key.rootContextIdentifier)
}
