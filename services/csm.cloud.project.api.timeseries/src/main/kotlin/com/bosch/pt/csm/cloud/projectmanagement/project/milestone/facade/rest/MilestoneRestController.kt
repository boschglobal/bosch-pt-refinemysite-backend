/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.MilestoneListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.assembler.MilestoneListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.service.MilestoneQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class MilestoneRestController(
    private val participantQueryService: ParticipantQueryService,
    private val milestoneListResourceAssembler: MilestoneListResourceAssembler,
    private val milestoneQueryService: MilestoneQueryService
) {

  companion object {
    const val MILESTONE_ENDPOINT = "/projects/milestones"
  }

  @GetMapping(MILESTONE_ENDPOINT)
  fun findMilestone(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<MilestoneListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val milestones =
        when (latestOnly) {
          true -> milestoneQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> milestoneQueryService.findAllByProjects(projectIds)
        }

    return ResponseEntity.ok().body(milestoneListResourceAssembler.assemble(milestones, latestOnly))
  }
}
