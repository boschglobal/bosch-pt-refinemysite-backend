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
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CleanStateFromUserPictureDeletedEvent(private val userService: UserService) :
    AbstractStateStrategy<UserPictureEventAvro>(), CleanUpStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean {
    val isTombstoneMessage =
        value == null &&
            key is AggregateEventMessageKey &&
            key.aggregateIdentifier.type == USERPICTURE.value
    val isDeletedMessage = value is UserPictureEventAvro && value.getName() == DELETED

    return isTombstoneMessage || isDeletedMessage
  }

  @Trace
  override fun updateState(key: EventMessageKey, event: UserPictureEventAvro) =
      userService.deletePicture(key.rootContextIdentifier)

  override fun updateStateForTombstone(key: EventMessageKey) =
      userService.deletePicture(key.rootContextIdentifier)
}
