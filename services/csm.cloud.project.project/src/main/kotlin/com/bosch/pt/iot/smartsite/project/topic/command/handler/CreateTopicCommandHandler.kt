/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.topic.command.api.CreateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.TopicSnapshot
import com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateTopicCommandHandler(private val eventBus: ProjectContextLocalEventBus) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#command.taskIdentifier)")
  fun handle(command: CreateTopicCommand): TopicId {

    return TopicSnapshot(
            identifier = command.identifier,
            version = INITIAL_SNAPSHOT_VERSION,
            description = command.description,
            criticality = command.criticality,
            taskIdentifier = command.taskIdentifier,
            projectIdentifier = command.projectIdentifier)
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
