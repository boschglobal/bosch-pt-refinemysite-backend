/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.job.submitter

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.precondition.ReschedulePrecondition.assertShiftNotZero
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobContext
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobType.PROJECT_RESCHEDULE
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class RescheduleJobSubmitter(
    private val jobIntegrationService: JobIntegrationService,
    private val projectQueryService: ProjectQueryService
) {

  @Trace
  @PreAuthorize(
      "@rescheduleAuthorizationComponent.hasReschedulePermissionOnProject(#projectIdentifier)")
  @Transactional
  open fun enqueueRescheduleJob(
      shiftDays: Long,
      useTaskCriteria: Boolean,
      useMilestoneCriteria: Boolean,
      taskCriteria: SearchTasksDto,
      milestoneCriteria: SearchMilestonesDto,
      projectIdentifier: ProjectId
  ): UUID {
    assertSameProjectIdentifier(taskCriteria, milestoneCriteria, projectIdentifier)

    assertShiftNotZero(shiftDays)
    val project = projectQueryService.findOneByIdentifier(projectIdentifier)!!

    return jobIntegrationService.enqueueJob(
        PROJECT_RESCHEDULE.name,
        SecurityContextHelper.getInstance().getCurrentUser().identifier!!,
        RescheduleJobContext(ResourceReference.from(project)),
        RescheduleCommand(
            shiftDays = shiftDays,
            useTaskCriteria = useTaskCriteria,
            useMilestoneCriteria = useMilestoneCriteria,
            taskCriteria = taskCriteria,
            milestoneCriteria = milestoneCriteria,
            projectIdentifier = projectIdentifier))
  }

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
