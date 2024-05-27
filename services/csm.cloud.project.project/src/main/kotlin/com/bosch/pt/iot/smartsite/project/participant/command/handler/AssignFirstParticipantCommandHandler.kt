/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignFirstParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AssignFirstParticipantCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: AssignFirstParticipantCommand) {
    ParticipantSnapshot(
            identifier = ParticipantId(),
            projectRef = command.projectRef,
            companyRef = command.companyRef,
            userRef = command.userRef,
            role = CSM,
            status = ACTIVE)
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
  }
}
