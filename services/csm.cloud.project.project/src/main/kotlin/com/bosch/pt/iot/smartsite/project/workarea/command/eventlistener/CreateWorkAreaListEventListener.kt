/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.eventlistener

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.list.CreateWorkAreaListCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
open class CreateWorkAreaListEventListener(
    private val createWorkAreaListCommandHandler: CreateWorkAreaListCommandHandler,
    private val projectRepository: ProjectRepository
) {

  @EventListener
  open fun handle(event: ProjectEventAvro) {
    if (event.name == ProjectEventEnumAvro.CREATED) {
      val projectId = event.aggregate.getIdentifier().asProjectId()

      val project =
          projectRepository.findOneByIdentifier(projectId)
              ?: throw PreconditionViolationException(PROJECT_VALIDATION_ERROR_PROJECT_NOT_FOUND)

      createWorkAreaListCommandHandler.handle(
          CreateWorkAreaListCommand(identifier = WorkAreaListId(), projectRef = project.identifier))
    }
  }
}
