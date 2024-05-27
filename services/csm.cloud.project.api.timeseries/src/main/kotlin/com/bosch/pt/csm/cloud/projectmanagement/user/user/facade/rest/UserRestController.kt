/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserListResource
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.assembler.UserListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service.UserQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class UserRestController(
    private val userListResourceAssembler: UserListResourceAssembler,
    private val userQueryService: UserQueryService,
    private val participantQueryService: ParticipantQueryService
) {

  companion object {
    const val USERS_ENDPOINT = "/users"
  }

  @GetMapping(USERS_ENDPOINT)
  fun findUsers(): ResponseEntity<UserListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val projectParticipants = participantQueryService.findActiveParticipantsOfProjects(projectIds)

    val users =
        userQueryService.findAllByIdentifiers(projectParticipants.map { it.user }.distinct())
    return ResponseEntity.ok().body(userListResourceAssembler.assemble(users))
  }
}
