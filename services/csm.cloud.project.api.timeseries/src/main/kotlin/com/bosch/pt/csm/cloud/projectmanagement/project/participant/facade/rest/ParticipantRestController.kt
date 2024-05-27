/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.ParticipantListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.assembler.ParticipantListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service.UserQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class ParticipantRestController(
    private val participantListResourceAssembler: ParticipantListResourceAssembler,
    private val participantQueryService: ParticipantQueryService,
    private val userQueryService: UserQueryService
) {

  companion object {
    const val PARTICIPANTS_ENDPOINT = "/projects/participants"
  }

  @GetMapping(PARTICIPANTS_ENDPOINT)
  fun findParticipants(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<ParticipantListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()

    // Find all participants and users. If a referenced user is not found,
    // then the user ID is not returned to the client (instead null is returned).
    val projectParticipants =
        when (latestOnly) {
          true -> participantQueryService.findActiveParticipantsOfProjects(projectIds)
          else -> participantQueryService.findParticipantsOfProjects(projectIds)
        }

    val idsOfExistingUsers =
        userQueryService.findAllByIdentifiers(projectParticipants.map { it.user }.distinct()).map {
          it.identifier
        }
    return ResponseEntity.ok()
        .body(
            participantListResourceAssembler.assemble(
                projectParticipants, idsOfExistingUsers, latestOnly))
  }
}
