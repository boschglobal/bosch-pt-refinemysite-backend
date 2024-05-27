/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.boundary.ProjectService
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromProjectDeletedEvent(
    private val projectService: ProjectService,
    private val participantService: ParticipantService,
    private val notificationSerice: NotificationService
) : AbstractStateStrategy<ProjectEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    return record.value is ProjectEventAvro &&
        (record.value as ProjectEventAvro).name == ProjectEventEnumAvro.DELETED
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: ProjectEventAvro) {
    val projectIdentifier = messageKey.rootContextIdentifier
    val participants = participantService.findAllByProjectIdentifier(projectIdentifier)

    participants.forEach { participant ->
      notificationSerice.deleteNotifications(participant.userIdentifier, projectIdentifier)
    }

    projectService.deleteProjectAndAllRelatedDocuments(projectIdentifier)
  }
}
