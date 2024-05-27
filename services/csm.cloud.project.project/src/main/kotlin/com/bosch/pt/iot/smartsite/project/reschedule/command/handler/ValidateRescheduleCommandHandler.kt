/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.handler

import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneRescheduleService
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.MilestoneRescheduleResult
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto.FailedDto
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto.SuccessfulDto
import com.bosch.pt.iot.smartsite.project.reschedule.command.precondition.ReschedulePrecondition.assertShiftNotZero
import com.bosch.pt.iot.smartsite.project.task.shared.dto.TaskRescheduleResult
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.TaskRescheduleService
import datadog.trace.api.Trace
import java.util.UUID
import org.slf4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class ValidateRescheduleCommandHandler(
    private val taskRescheduleService: TaskRescheduleService,
    private val milestoneRescheduleService: MilestoneRescheduleService,
    private val logger: Logger
) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize(
      "@rescheduleAuthorizationComponent.hasReschedulePermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: RescheduleCommand): RescheduleResultDto {
    assertSameProjectIdentifier(
        command.taskCriteria, command.milestoneCriteria, command.projectIdentifier)
    assertShiftNotZero(command.shiftDays)

    var taskRescheduleResult = TaskRescheduleResult()
    var milestoneRescheduleResult = MilestoneRescheduleResult()

    if (command.useTaskCriteria) {
      taskRescheduleResult = taskRescheduleService.validate(command.taskCriteria)
    }

    if (command.useMilestoneCriteria) {
      milestoneRescheduleResult =
          milestoneRescheduleService.validateMilestones(command.milestoneCriteria)
    }

    logRequest(command.projectIdentifier.identifier)
    return buildRescheduleResult(taskRescheduleResult, milestoneRescheduleResult)
  }

  private fun buildRescheduleResult(
      taskRescheduleResult: TaskRescheduleResult,
      milestoneRescheduleResult: MilestoneRescheduleResult
  ) =
      RescheduleResultDto(
          SuccessfulDto(milestoneRescheduleResult.successful, taskRescheduleResult.successful),
          FailedDto(milestoneRescheduleResult.failed, taskRescheduleResult.failed))

  private fun logRequest(projectIdentifier: UUID) =
      logger.info("Reschedule validate done for the project $projectIdentifier")

  private fun assertSameProjectIdentifier(
      taskCriteria: SearchTasksDto,
      milestoneCriteria: SearchMilestonesDto,
      projectIdentifier: ProjectId
  ) =
      require(
          taskCriteria.projectIdentifier == projectIdentifier &&
              milestoneCriteria.projectIdentifier == projectIdentifier) {
            "The project identifier needs to be equal in all elements of the function signature."
          }
}
