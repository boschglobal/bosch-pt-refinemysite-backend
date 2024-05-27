/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler.batch

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.CreateWorkAreaCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_POSITION_VALUE
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateWorkAreaBatchCommandHandler(
    private val createWorkAreaCommandHandler: CreateWorkAreaCommandHandler
) {

  @Trace
  @Transactional
  @NoPreAuthorize // authorization is handled by non-batch command handlers
  open fun handle(projectRef: ProjectId, commands: List<CreateWorkAreaCommand>): List<WorkAreaId> {
    if (commands.isEmpty()) return emptyList()

    require(commands.allBelongToSameProject()) {
      "Multiple workAreas can only be created for one project at at time"
    }
    val projectIdentifierFromCommands = commands.first().projectRef

    require(projectIdentifierFromCommands == projectRef) {
      "WorkAreas cannot be created for a foreign project"
    }

    if ((commands.size) > MAX_WORKAREA_POSITION_VALUE) {
      throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION)
    }

    return commands.sortedBy { it.position }.map { createWorkAreaCommandHandler.handle(it) }
  }

  private fun List<CreateWorkAreaCommand>.allBelongToSameProject(): Boolean =
      map { it.projectRef }.distinct().size == 1
}
