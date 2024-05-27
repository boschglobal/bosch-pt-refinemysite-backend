/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.RelationListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.assembler.RelationListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.service.RelationQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class RelationRestController(
    private val participantQueryService: ParticipantQueryService,
    private val relationQueryService: RelationQueryService,
    private val relationListResourceAssembler: RelationListResourceAssembler
) {

  companion object {
    const val RELATIONS_ENDPOINT = "/projects/relations"
  }

  @GetMapping(RELATIONS_ENDPOINT)
  fun findRelations(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<RelationListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val relations =
        when (latestOnly) {
          true -> relationQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> relationQueryService.findAllByProjects(projectIds)
        }

    return ResponseEntity.ok().body(relationListResourceAssembler.assemble(relations, latestOnly))
  }
}
