/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.WorkAreaListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.assembler.WorkAreaListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.service.WorkAreaQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class WorkAreaRestController(
    private val workAreaListResourceAssembler: WorkAreaListResourceAssembler,
    private val participantQueryService: ParticipantQueryService,
    private val workAreaQueryService: WorkAreaQueryService
) {

  companion object {
    const val WORK_AREAS_ENDPOINT = "/projects/workareas"
  }

  @GetMapping(WORK_AREAS_ENDPOINT)
  fun findWorkAreas(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<WorkAreaListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val workAreas =
        when (latestOnly) {
          true -> workAreaQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> workAreaQueryService.findAllByProjects(projectIds)
        }
    return ResponseEntity.ok().body(workAreaListResourceAssembler.assemble(workAreas, latestOnly))
  }
}
