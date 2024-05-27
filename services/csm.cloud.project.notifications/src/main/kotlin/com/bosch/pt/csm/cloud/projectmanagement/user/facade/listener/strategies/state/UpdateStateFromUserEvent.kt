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
import com.bosch.pt.csm.cloud.projectmanagement.user.model.GenderEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import datadog.trace.api.Trace
import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component

@Component
class UpdateStateFromUserEvent(private val userService: UserService) :
    AbstractStateStrategy<UserEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    return record.value is UserEventAvro &&
        (record.value as UserEventAvro).name != UserEventEnumAvro.DELETED
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: UserEventAvro): Unit =
      event.aggregate.run {
        userService.save(
            User(
                identifier = getIdentifier(),
                externalIdentifier = userId,
                displayName = "$firstName $lastName",
                gender = gender?.run { GenderEnum.valueOf(name) },
                locale = locale?.let { LocaleUtils.toLocale(it) },
                admin = admin,
                locked = locked,
            ))
      }
}
