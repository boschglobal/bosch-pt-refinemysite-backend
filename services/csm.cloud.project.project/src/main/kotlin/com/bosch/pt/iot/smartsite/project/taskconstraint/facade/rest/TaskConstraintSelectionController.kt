/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintSelectionService
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request.UpdateTaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory.TaskConstraintSelectionListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory.TaskConstraintSelectionResourceFactory
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.annotation.Nonnull
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@ApiVersion
@RestController
open class TaskConstraintSelectionController(
    private val constraintSelectionService: TaskConstraintSelectionService,
    private val constraintSelectionResourceFactory: TaskConstraintSelectionResourceFactory,
    private val constraintSelectionListResourceFactory: TaskConstraintSelectionListResourceFactory
) {

  @PutMapping(CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID)
  open fun updateConstraintSelection(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @PathVariable(PATH_VARIABLE_TASK_ID) taskId: TaskId,
      @RequestBody @Valid updateConstraintsResource: UpdateTaskConstraintSelectionResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") @Nonnull eTag: ETag
  ): ResponseEntity<TaskConstraintSelectionResource> {
    constraintSelectionService.createEmptySelectionIfNotExists(
        taskId, updateConstraintsResource.constraints)

    constraintSelectionService.updateSelection(
        projectId, taskId, updateConstraintsResource.constraints, eTag)

    val selection = constraintSelectionService.findSelection(taskId)
    return ResponseEntity.ok()
        .body(constraintSelectionResourceFactory.build(projectId, taskId, selection))
  }

  @GetMapping(CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID)
  open fun findConstraintSelection(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @PathVariable(PATH_VARIABLE_TASK_ID) taskId: TaskId
  ): ResponseEntity<TaskConstraintSelectionResource> {
    val constraints = constraintSelectionService.findSelection(taskId)
    return ResponseEntity.ok()
        .body(constraintSelectionResourceFactory.build(projectId, taskId, constraints))
  }

  @PostMapping(CONSTRAINTS_BY_PROJECT_ID)
  open fun findConstraintSelections(
      @PathVariable(PATH_VARIABLE_PROJECT_ID) projectId: ProjectId,
      @RequestBody batchRequestResource: @Valid BatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = BatchRequestIdentifierType.TASK)
      identifierType: String
  ): ResponseEntity<TaskConstraintSelectionListResource> =
      if (identifierType == BatchRequestIdentifierType.TASK) {
        val selections =
            constraintSelectionService.findSelections(batchRequestResource.ids.asTaskIds())
        ResponseEntity.ok()
            .body(constraintSelectionListResourceFactory.build(projectId, selections))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  companion object {
    const val CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID =
        "/projects/{projectId}/tasks/{taskId}/constraints"
    const val CONSTRAINTS_BY_PROJECT_ID = "/projects/{projectId}/tasks/constraints"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
    const val PATH_VARIABLE_TASK_ID = "taskId"
  }
}
