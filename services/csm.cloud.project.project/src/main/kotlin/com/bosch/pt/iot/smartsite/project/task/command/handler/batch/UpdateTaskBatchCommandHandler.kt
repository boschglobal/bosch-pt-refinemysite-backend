/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.UpdateTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.helper.TaskCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateTaskBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val updateTaskCommandHandler: UpdateTaskCommandHandler,
    private val taskRepository: TaskRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val participantRepository: ParticipantRepository,
    private val taskCommandHandlerHelper: TaskCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(commands: List<UpdateTaskCommand>) {
    if (commands.isEmpty()) {
      return
    }

    taskCommandHandlerHelper.assertTasksBelongToSameProject(commands.map { it.projectIdentifier })
    val projectIdentifier = commands.first().projectIdentifier

    taskRepository.findAllByIdentifierIn(commands.map { it.identifier }).let {
      if (it.isNotEmpty()) {

        // Due to the update can also perform an assigment, the participants must also be cached
        participantRepository.findAllByIdentifierIn(commands.map { it.assigneeIdentifier })

        // Populate existence cache
        projectCraftRepository.findAllByIdentifierIn(commands.map { it.projectCraftIdentifier })
        workAreaRepository.findAllByIdentifierIn(
            commands.mapNotNull(UpdateTaskCommand::workAreaIdentifier).distinct())

        businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
          commands.map { updateTaskCommandHandler.handle(it) }
        }
      }
    }
  }
}
