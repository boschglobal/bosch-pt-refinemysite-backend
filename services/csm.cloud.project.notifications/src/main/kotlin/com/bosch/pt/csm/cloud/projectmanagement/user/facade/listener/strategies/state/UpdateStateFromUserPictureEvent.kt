/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getUserIdentifier
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromUserPictureEvent(private val userService: UserService) :
    AbstractStateStrategy<UserPictureEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord): Boolean = record.value is UserPictureEventAvro &&
      (record.value as UserPictureEventAvro).name != UserPictureEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: UserPictureEventAvro) =
      event.aggregate.run { userService.saveUserPictureIdentifier(getUserIdentifier(), getIdentifier()) }
}
