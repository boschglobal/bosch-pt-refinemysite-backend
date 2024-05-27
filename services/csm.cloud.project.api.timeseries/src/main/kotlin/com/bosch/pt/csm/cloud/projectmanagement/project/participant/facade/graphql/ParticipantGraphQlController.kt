/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response.ParticipantPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response.assembler.ParticipantPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class ParticipantGraphQlController(
    private val participantPayloadAssembler: ParticipantPayloadAssembler,
    private val participantQueryService: ParticipantQueryService
) {

  @BatchMapping
  fun assignee(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, ParticipantPayloadV1?> {
    val participants =
        participantQueryService.findParticipantsOfProjects(tasks.map { it.projectId }).groupBy {
          it.project
        }

    return tasks.associateWith { task ->
      participants[task.projectId]
          ?.firstOrNull { participant -> task.assigneeId == participant.identifier }
          ?.let { participantPayloadAssembler.assemble(it) }
    }
  }

  @BatchMapping
  fun participants(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, List<ParticipantPayloadV1>> {
    val participants =
        participantQueryService
            .findActiveParticipantsOfProjects(projects.map { it.id.asProjectId() })
            .groupBy { it.project }

    return projects.associateWith {
      (participants[it.id.asProjectId()]?.map { participantPayloadAssembler.assemble(it) }
          ?: emptyList())
    }
  }
}
