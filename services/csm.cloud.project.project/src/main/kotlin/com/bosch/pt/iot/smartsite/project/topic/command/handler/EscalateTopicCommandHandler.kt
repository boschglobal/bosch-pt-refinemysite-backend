/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.topic.command.api.EscalateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshotStore
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EscalateTopicCommandHandler(
    private val snapshotStore: TopicSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#command.identifier)")
  fun handle(command: EscalateTopicCommand): Boolean {
    val snapshot = snapshotStore.findOrFail(command.identifier)

    return when (snapshot.criticality) {
      CRITICAL -> false
      else -> {

        snapshot
            .toCommandHandler()
            .update { it.copy(criticality = CRITICAL) }
            .emitEvent(ESCALATED)
            .ifSnapshotWasChanged()
            .to(eventBus)

        true
      }
    }
  }
}
