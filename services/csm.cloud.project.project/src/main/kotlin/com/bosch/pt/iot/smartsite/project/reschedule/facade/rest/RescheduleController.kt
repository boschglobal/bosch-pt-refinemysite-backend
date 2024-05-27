/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.JobResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.handler.ValidateRescheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.submitter.RescheduleJobSubmitter
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.request.RescheduleResource
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response.RescheduleResultResource
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response.factory.RescheduleResourceFactory
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion(from = 5)
@RestController
open class RescheduleController(
    private val validateRescheduleCommandHandler: ValidateRescheduleCommandHandler,
    private val rescheduleJobSubmitter: RescheduleJobSubmitter,
    private val rescheduleResourceFactory: RescheduleResourceFactory
) {

  @PostMapping(RESCHEDULE_VALIDATION_ENDPOINT)
  open fun validate(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid resource: RescheduleResource
  ): ResponseEntity<RescheduleResultResource> {
    val rescheduleResultDto =
        validateRescheduleCommandHandler.handle(
            RescheduleCommand(
                resource.shiftDays,
                resource.useTaskCriteria,
                resource.useMilestoneCriteria,
                resource.toSearchTasksDto(projectIdentifier),
                resource.toSearchMilestonesDto(projectIdentifier),
                projectIdentifier))

    return ResponseEntity.ok(rescheduleResourceFactory.build(rescheduleResultDto))
  }

  @PostMapping(RESCHEDULE_ENDPOINT)
  open fun reschedule(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectIdentifier: ProjectId,
      @RequestBody @Valid resource: RescheduleResource
  ): ResponseEntity<JobResponseResource> {
    val jobIdentifier =
        rescheduleJobSubmitter.enqueueRescheduleJob(
            resource.shiftDays,
            resource.useTaskCriteria,
            resource.useMilestoneCriteria,
            resource.toSearchTasksDto(projectIdentifier),
            resource.toSearchMilestonesDto(projectIdentifier),
            projectIdentifier)

    return ResponseEntity.accepted().body(JobResponseResource(jobIdentifier.toString()))
  }

  private fun RescheduleResource.toSearchTasksDto(projectIdentifier: ProjectId) =
      this.criteria.tasks.toSearchTasksDto(projectIdentifier)

  private fun RescheduleResource.toSearchMilestonesDto(projectIdentifier: ProjectId) =
      this.criteria.milestones.toSearchMilestonesDto(projectIdentifier)

  companion object {
    const val PATH_VARIABLE_PROJECT_ID = "projectId"

    const val RESCHEDULE_ENDPOINT = "/projects/{projectId}/reschedule"
    const val RESCHEDULE_VALIDATION_ENDPOINT = "/projects/{projectId}/reschedule/validate"
  }
}
