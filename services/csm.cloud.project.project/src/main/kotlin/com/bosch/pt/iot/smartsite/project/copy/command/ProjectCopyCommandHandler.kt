/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.command

import com.bosch.pt.iot.smartsite.project.copy.api.ProjectCopyCommand
import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyService
import com.bosch.pt.iot.smartsite.project.copy.command.dto.CopiedProjectResult
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
open class ProjectCopyCommandHandler(private val projectCopyService: ProjectCopyService) {

  @Trace
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: ProjectCopyCommand): CopiedProjectResult =
      projectCopyService.copy(command.projectIdentifier.asProjectId(), command.copyParameters)
}
