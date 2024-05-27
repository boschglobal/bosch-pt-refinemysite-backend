/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.handler

import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneRescheduleService
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.MilestoneRescheduleResult
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto
import com.bosch.pt.iot.smartsite.project.task.shared.dto.TaskRescheduleResult
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.TaskRescheduleService
import datadog.trace.api.Trace
import java.util.UUID
import org.slf4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class RescheduleCommandHandler(
    private val taskRescheduleService: TaskRescheduleService,
    private val milestoneRescheduleService: MilestoneRescheduleService,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val logger: Logger
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@rescheduleAuthorizationComponent.hasReschedulePermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: RescheduleCommand): RescheduleResultDto {

    val rescheduleResult =
        businessTransactionManager.doProjectRescheduleInBusinessTransaction(
            command.projectIdentifier, command.shiftDays) {
              var taskRescheduleResult = TaskRescheduleResult()
              var milestoneRescheduleResult = MilestoneRescheduleResult()

              if (command.useTaskCriteria) {
                taskRescheduleResult =
                    taskRescheduleService.reschedule(command.shiftDays, command.taskCriteria)
              }
              if (command.useMilestoneCriteria) {
                milestoneRescheduleResult =
                    milestoneRescheduleService.rescheduleMilestones(
                        command.shiftDays, command.milestoneCriteria)
              }

              buildRescheduleResult(taskRescheduleResult, milestoneRescheduleResult)
            }

    logStatusChange(command.projectIdentifier.identifier)
    return rescheduleResult
  }

  private fun buildRescheduleResult(
      taskRescheduleResult: TaskRescheduleResult,
      milestoneRescheduleResult: MilestoneRescheduleResult
  ) =
      RescheduleResultDto(
          RescheduleResultDto.SuccessfulDto(
              milestoneRescheduleResult.successful, taskRescheduleResult.successful),
          RescheduleResultDto.FailedDto(
              milestoneRescheduleResult.failed, taskRescheduleResult.failed))

  private fun logStatusChange(projectIdentifier: UUID) =
      logger.info("Reschedule done for the project $projectIdentifier")
}
