/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintService
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request.UpdateTaskConstraintResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory.TaskConstraintListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory.TaskConstraintResourceFactory
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Validated
@ApiVersion
@RestController
open class TaskConstraintController(
    private val constraintService: TaskConstraintService,
    private val constraintResourceFactory: TaskConstraintResourceFactory,
    private val constraintListResourceFactory: TaskConstraintListResourceFactory
) {

  @PutMapping(CONSTRAINTS_BY_PROJECT_ID)
  open fun update(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody @Valid updateConstraintResource: UpdateTaskConstraintResource,
  ): ResponseEntity<TaskConstraintResource> {
    val constraint = constraintService.update(updateConstraintResource.toDto(projectId))
    return ResponseEntity.ok().body(constraintResourceFactory.build(projectId, constraint))
  }

  @GetMapping(CONSTRAINTS_BY_PROJECT_ID)
  open fun findAll(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
  ): ResponseEntity<BatchResponseResource<TaskConstraintResource>> {
    val constraints = constraintService.findAll(projectId)
    return ResponseEntity.ok().body(constraintListResourceFactory.build(projectId, constraints))
  }

  companion object {
    const val CONSTRAINTS_BY_PROJECT_ID = "/projects/{projectId}/constraints"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
  }
}
