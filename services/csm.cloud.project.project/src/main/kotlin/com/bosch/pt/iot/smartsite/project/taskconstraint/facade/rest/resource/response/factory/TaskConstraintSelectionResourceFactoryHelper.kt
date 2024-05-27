/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.NamedEnumReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintService
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.hateoas.Link
import org.springframework.stereotype.Component

@Component
open class TaskConstraintSelectionResourceFactoryHelper(
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val taskConstraintService: TaskConstraintService,
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(
      projectIdentifier: ProjectId,
      constraintSelections: List<TaskConstraintSelectionDto>
  ): List<TaskConstraintSelectionResource> {
    val taskIdentifiers = constraintSelections.map { it.taskIdentifier }
    val contributePermissions =
        taskAuthorizationComponent.filterTasksWithContributePermission(taskIdentifiers.toSet())

    val translatedConstraints = taskConstraintService.resolveProjectConstraints(projectIdentifier)

    return constraintSelections.map { constraintSelection ->
      TaskConstraintSelectionResource(
              constraintSelection.taskIdentifier.toUuid(),
              constraintSelection.constraints.map {
                NamedEnumReference(it, translatedConstraints[it]!!)
              },
              constraintSelection.version)
          .apply {
            // task constraint update link
            addIf(contributePermissions.contains(constraintSelection.taskIdentifier)) {
              updateLink(projectIdentifier, constraintSelection.taskIdentifier.toUuid())
            }
          }
    }
  }
  private fun updateLink(projectIdentifier: ProjectId, taskIdentifier: UUID): Link =
      linkFactory
          .linkTo(TaskConstraintSelectionController.CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID)
          .withParameters(
              mapOf(
                  TaskConstraintSelectionController.PATH_VARIABLE_PROJECT_ID to projectIdentifier,
                  TaskConstraintSelectionController.PATH_VARIABLE_TASK_ID to taskIdentifier))
          .withRel(TaskConstraintSelectionResource.LINK_CONSTRAINTS_UPDATE)
}
