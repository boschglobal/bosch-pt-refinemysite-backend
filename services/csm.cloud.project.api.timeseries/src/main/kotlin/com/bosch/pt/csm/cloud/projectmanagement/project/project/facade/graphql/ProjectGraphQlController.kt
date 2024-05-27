/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.assembler.ProjectPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.service.ProjectQueryService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProjectGraphQlController(
    private val participantQueryService: ParticipantQueryService,
    private val projectPayloadAssembler: ProjectPayloadAssembler,
    private val projectQueryService: ProjectQueryService
) {

  @QueryMapping
  fun projects(): List<ProjectPayloadV1> {
    val participants = participantQueryService.findActiveParticipantsOfCurrentUser()
    val projects = projectQueryService.findAllByIdentifierIn(participants.projects())

    return projects.map { projectPayloadAssembler.assemble(it) }
  }
}
