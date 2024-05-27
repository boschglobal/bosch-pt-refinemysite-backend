/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignActiveParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AssignActiveParticipantCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: AssignActiveParticipantCommand) {
    ParticipantSnapshot(
            identifier = command.identifier,
            projectRef = command.projectRef,
            companyRef = command.companyRef,
            userRef = command.userRef,
            role = command.role,
            status = ACTIVE)
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
  }
}
