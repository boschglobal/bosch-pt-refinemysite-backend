/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.assembler.ProjectListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.service.ProjectQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class ProjectRestController(
    private val participantQueryService: ParticipantQueryService,
    private val projectListResourceAssembler: ProjectListResourceAssembler,
    private val projectQueryService: ProjectQueryService
) {

  companion object {
    const val PROJECTS_ENDPOINT = "/projects"
  }

  @GetMapping(PROJECTS_ENDPOINT)
  fun findProjects(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<ProjectListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val projects = projectQueryService.findAllByIdentifierIn(projectIds)
    return ResponseEntity.ok().body(projectListResourceAssembler.assemble(projects, latestOnly))
  }
}
