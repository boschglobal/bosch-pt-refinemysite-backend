/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DEESCALATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.topic.command.api.DeescalateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshotStore
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeescalateTopicCommandHandler(
    private val snapshotStore: TopicSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#command.identifier)")
  fun handle(command: DeescalateTopicCommand): Boolean {
    val snapshot = snapshotStore.findOrFail(command.identifier)

    return when (snapshot.criticality) {
      UNCRITICAL -> false
      else -> {

        snapshot
            .toCommandHandler()
            .update { it.copy(criticality = UNCRITICAL) }
            .emitEvent(DEESCALATED)
            .ifSnapshotWasChanged()
            .to(eventBus)

        true
      }
    }
  }
}
