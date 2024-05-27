/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.name
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.AbstractNotificationStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.notifications.RecipientDeterminator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import java.util.UUID

abstract class AbstractDayCardNotificationStrategy(
    protected val taskService: TaskService,
    private val recipientDeterminator: RecipientDeterminator
) : AbstractNotificationStrategy<DayCardEventG2Avro>() {

  protected fun determineRecipients(aggregate: DayCardAggregateG2Avro, projectIdentifier: UUID) =
      recipientDeterminator.determineDefaultRecipients(
          taskService.findLatest(aggregate.getTaskIdentifier(), projectIdentifier),
          aggregate.getLastModifiedByUserIdentifier())

  protected fun buildNotificationIdentifier(
      aggregate: DayCardAggregateG2Avro,
      recipientIdentifier: UUID
  ) = aggregate.buildNotificationIdentifier(recipientIdentifier)

  protected fun buildEventInformation(event: DayCardEventG2Avro) =
      EventInformation(
          name = event.name(),
          date = event.getLastModifiedDate(),
          user = event.getLastModifiedByUserIdentifier())

  protected fun buildContext(projectIdentifier: UUID, aggregate: DayCardAggregateG2Avro) =
      Context(project = projectIdentifier, task = aggregate.getTaskIdentifier())
}
