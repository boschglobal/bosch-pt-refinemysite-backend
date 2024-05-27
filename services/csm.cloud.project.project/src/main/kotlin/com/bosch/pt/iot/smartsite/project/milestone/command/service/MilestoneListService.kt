/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.service

import com.bosch.pt.iot.smartsite.project.milestone.command.api.AddMilestoneToListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.api.DeleteMilestoneListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.api.RemoveMilestoneFromListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.api.ReorderMilestoneListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.list.AddMilestoneToListCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.list.CreateMilestoneListCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.list.DeleteMilestoneListCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.list.RemoveMilestoneFromListCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.list.ReorderMilestoneListCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import org.springframework.stereotype.Component

@Component
class MilestoneListService(
    private val milestoneListRepository: MilestoneListRepository,
    private val addMilestoneToListCommandHandler: AddMilestoneToListCommandHandler,
    private val createMilestoneListCommandHandler: CreateMilestoneListCommandHandler,
    private val deleteMilestoneListCommandHandler: DeleteMilestoneListCommandHandler,
    private val removeMilestoneFromListCommandHandler: RemoveMilestoneFromListCommandHandler,
    private val reorderMilestoneListCommandHandler: ReorderMilestoneListCommandHandler,
) {

  fun addMilestoneToList(milestoneSnapshot: MilestoneSnapshot, position: Int) {
    val milestoneList = milestoneSnapshot.findList()
    if (milestoneList != null) {
      milestoneSnapshot.addToExistingList(milestoneList.identifier, position)
    } else {
      milestoneSnapshot.addToNewList()
    }
  }

  fun removeMilestoneFromList(milestoneSnapshot: MilestoneSnapshot) {
    val milestoneList = milestoneSnapshot.findListOrFail()
    if (milestoneList.milestones.size == 1) {
      deleteList(milestoneList.identifier)
    } else {
      milestoneSnapshot.removeFromList(milestoneList.identifier)
    }
  }

  fun updateListAfterMilestoneUpdate(
      oldMilestoneSnapshot: MilestoneSnapshot,
      updatedMilestoneSnapshot: MilestoneSnapshot,
      position: Int?
  ) {
    val oldMilestoneList = oldMilestoneSnapshot.findListOrFail()

    if (updatedMilestoneSnapshot.shouldMoveToDifferentListThan(oldMilestoneSnapshot)) {
      removeMilestoneFromList(oldMilestoneSnapshot)
      addMilestoneToList(updatedMilestoneSnapshot, position ?: 0)
    } else if (position != null) {
      updatedMilestoneSnapshot.reorderInList(oldMilestoneList.identifier, position)
    }
  }

  private fun deleteList(milestoneListRef: MilestoneListId) =
      deleteMilestoneListCommandHandler.handle(
          DeleteMilestoneListCommand(identifier = milestoneListRef))

  private fun MilestoneSnapshot.shouldMoveToDifferentListThan(other: MilestoneSnapshot) =
      projectRef != other.projectRef ||
          header != other.header ||
          date != other.date ||
          workAreaRef != other.workAreaRef

  private fun MilestoneSnapshot.reorderInList(milestoneListRef: MilestoneListId, position: Int) {
    reorderMilestoneListCommandHandler.handle(
        ReorderMilestoneListCommand(
            identifier = milestoneListRef, milestoneRef = identifier, position = position))
  }

  private fun MilestoneSnapshot.findList() =
      milestoneListRepository.findOneByKey(projectRef, date, header, workAreaRef)

  private fun MilestoneSnapshot.findListOrFail() =
      findList() ?: error("Could not find milestone list for milestone $identifier")

  private fun MilestoneSnapshot.addToExistingList(
      milestoneListRef: MilestoneListId,
      position: Int
  ) =
      addMilestoneToListCommandHandler.handle(
          AddMilestoneToListCommand(
              identifier = milestoneListRef, milestoneRef = identifier, position = position))

  private fun MilestoneSnapshot.addToNewList() =
      createMilestoneListCommandHandler.handle(
          CreateMilestoneListCommand(
              projectRef = projectRef,
              date = date,
              header = header,
              workAreaRef = workAreaRef,
              milestoneRef = identifier))

  private fun MilestoneSnapshot.removeFromList(milestoneListRef: MilestoneListId) =
      removeMilestoneFromListCommandHandler.handle(
          RemoveMilestoneFromListCommand(
              identifier = identifier, milestoneListRef = milestoneListRef))
}
