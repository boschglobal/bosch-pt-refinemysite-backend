/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.ProjectCraftListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.assembler.ProjectCraftListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.service.ProjectCraftQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class ProjectCraftRestController(
    private val projectCraftListResourceAssembler: ProjectCraftListResourceAssembler,
    private val participantQueryService: ParticipantQueryService,
    private val projectCraftQueryService: ProjectCraftQueryService
) {

  companion object {
    const val PROJECT_CRAFTS_ENDPOINT = "/projects/crafts"
  }

  @GetMapping(PROJECT_CRAFTS_ENDPOINT)
  fun findProjectCrafts(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<ProjectCraftListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()

    val projectCrafts =
        when (latestOnly) {
          true -> projectCraftQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> projectCraftQueryService.findAllByProjects(projectIds)
        }
    return ResponseEntity.ok()
        .body(projectCraftListResourceAssembler.assemble(projectCrafts, latestOnly))
  }
}
