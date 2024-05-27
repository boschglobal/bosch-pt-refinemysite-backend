/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.assembler.TaskConstraintSelectionListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.service.TaskConstraintSelectionQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class TaskConstraintSelectionRestController(
    private val participantQueryService: ParticipantQueryService,
    private val taskConstraintSelectionListResourceAssembler:
        TaskConstraintSelectionListResourceAssembler,
    private val taskConstraintSelectionQueryService: TaskConstraintSelectionQueryService
) {

  companion object {
    const val CONSTRAINTS_ENDPOINT = "/projects/tasks/constraints"
  }

  @GetMapping(CONSTRAINTS_ENDPOINT)
  fun findConstraints(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<TaskConstraintSelectionListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val constraints =
        when (latestOnly) {
          true -> taskConstraintSelectionQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> taskConstraintSelectionQueryService.findAllByProjects(projectIds)
        }

    return ResponseEntity.ok()
        .body(taskConstraintSelectionListResourceAssembler.assemble(constraints, latestOnly))
  }
}
