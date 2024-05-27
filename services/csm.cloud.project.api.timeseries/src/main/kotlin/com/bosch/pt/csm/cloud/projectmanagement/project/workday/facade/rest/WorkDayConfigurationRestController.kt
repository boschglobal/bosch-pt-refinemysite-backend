/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.WorkDayConfigurationListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.assembler.WorkDayConfigurationListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.service.WorkDayConfigurationQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class WorkDayConfigurationRestController(
    private val workDayConfigurationListResourceAssembler:
        WorkDayConfigurationListResourceAssembler,
    private val participantQueryService: ParticipantQueryService,
    private val workDayConfigurationQueryService: WorkDayConfigurationQueryService
) {

  companion object {
    const val WORK_DAYS_ENDPOINT = "/projects/workdays"
  }

  @GetMapping(WORK_DAYS_ENDPOINT)
  fun findWorkDayConfigurations(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<WorkDayConfigurationListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val workDays =
        when (latestOnly) {
          true -> workDayConfigurationQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> workDayConfigurationQueryService.findAllByProjects(projectIds)
        }
    return ResponseEntity.ok()
        .body(workDayConfigurationListResourceAssembler.assemble(workDays, latestOnly))
  }
}
