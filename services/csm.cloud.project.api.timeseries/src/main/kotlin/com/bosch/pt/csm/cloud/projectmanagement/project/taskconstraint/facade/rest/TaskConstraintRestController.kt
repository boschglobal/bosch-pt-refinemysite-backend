/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.TaskConstraintListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.assembler.TaskConstraintListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.service.TaskConstraintQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class TaskConstraintRestController(
    private val participantQueryService: ParticipantQueryService,
    private val taskConstraintListResourceAssembler: TaskConstraintListResourceAssembler,
    private val taskConstraintQueryService: TaskConstraintQueryService,
) {

  companion object {
    const val CONSTRAINTS_ENDPOINT = "/projects/constraints"
  }

  @GetMapping(CONSTRAINTS_ENDPOINT)
  fun findConstraints(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<TaskConstraintListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val constraints =
        when (latestOnly) {
          true -> taskConstraintQueryService.findAllByProjectsAndDeletedFalseWithMissing(projectIds)
          else -> taskConstraintQueryService.findAllByProjectsWithMissing(projectIds)
        }

    return ResponseEntity.ok()
        .body(taskConstraintListResourceAssembler.assemble(constraints, latestOnly))
  }
}
