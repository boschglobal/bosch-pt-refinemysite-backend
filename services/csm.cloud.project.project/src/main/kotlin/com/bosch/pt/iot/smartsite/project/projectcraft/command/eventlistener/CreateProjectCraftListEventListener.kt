/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.eventlistener

import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list.CreateProjectCraftListCommandHandler
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
open class CreateProjectCraftListEventListener(
    private val createProjectCraftListCommandHandler: CreateProjectCraftListCommandHandler
) {

  @EventListener
  open fun handle(event: ProjectEventAvro) {
    if (event.name == ProjectEventEnumAvro.CREATED) {
      createProjectCraftListCommandHandler.handle(
          CreateProjectCraftListCommand(
              projectIdentifier = event.aggregate.getIdentifier().asProjectId()))
    }
  }
}
