/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.eventlistener

import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workday.command.api.CreateWorkdayConfigurationCommand
import com.bosch.pt.iot.smartsite.project.workday.command.handler.CreateWorkdayConfigurationCommandHandler
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
open class CreateWorkdayConfigurationEventListener(
    private val createWorkdayConfigurationCommandHandler: CreateWorkdayConfigurationCommandHandler
) {

  @EventListener
  open fun handle(event: ProjectEventAvro) {
    if (event.name == ProjectEventEnumAvro.CREATED) {
      createWorkdayConfigurationCommandHandler.handle(
          CreateWorkdayConfigurationCommand(
              projectRef = event.aggregate.getIdentifier().asProjectId()))
    }
  }
}
