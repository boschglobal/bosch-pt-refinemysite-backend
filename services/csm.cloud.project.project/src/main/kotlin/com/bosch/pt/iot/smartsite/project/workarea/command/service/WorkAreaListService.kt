/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.service

import com.bosch.pt.iot.smartsite.project.workarea.command.api.AddWorkAreaToListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.api.RemoveWorkAreaFromListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.list.AddWorkAreaToListCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.list.RemoveWorkAreaFromListCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import org.springframework.stereotype.Component

@Component
class WorkAreaListService(
    private val workAreaListRepository: WorkAreaListRepository,
    private val addWorkAreaToListCommandHandler: AddWorkAreaToListCommandHandler,
    private val removeWorkAreaFromListCommandHandler: RemoveWorkAreaFromListCommandHandler,
) {

  fun addWorkAreaToList(
      workAreaSnapshot: WorkAreaSnapshot,
      workAreaListId: WorkAreaListId,
      position: Int,
      version: Long
  ) {
    addWorkAreaToListCommandHandler.handle(
        AddWorkAreaToListCommand(
            projectRef = workAreaSnapshot.projectRef,
            identifier = workAreaListId,
            version = version,
            workAreaRef = workAreaSnapshot.identifier,
            position = position))
  }

  fun removeWorkAreaFromList(workAreaSnapshot: WorkAreaSnapshot) {
    val workAreaList = workAreaSnapshot.findListOrFail()
    removeWorkAreaFromListCommandHandler.handle(
        RemoveWorkAreaFromListCommand(
            identifier = workAreaSnapshot.identifier,
            workAreaListRef = workAreaList.identifier,
            version = workAreaList.version))
  }

  private fun WorkAreaSnapshot.findList() =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectRef)

  private fun WorkAreaSnapshot.findListOrFail() =
      findList() ?: error("Could not find workArea list for workArea $identifier")
}
