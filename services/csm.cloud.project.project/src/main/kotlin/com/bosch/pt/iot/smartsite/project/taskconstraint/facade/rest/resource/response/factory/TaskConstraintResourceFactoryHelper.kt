/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.TaskConstraintDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintController
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_CONSTRAINT_ACTIVATE
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_CONSTRAINT_DEACTIVATE
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_CONSTRAINT_UPDATE
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
open class TaskConstraintResourceFactoryHelper(
    private val messageSource: MessageSource,
    private val linkFactory: CustomLinkBuilderFactory
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(
      projectIdentifier: ProjectId,
      constraints: List<TaskConstraintDto>
  ): List<TaskConstraintResource> =
      constraints.map { buildConstraintResource(projectIdentifier, it) }

  private fun buildConstraintResource(
      projectIdentifier: ProjectId,
      constraint: TaskConstraintDto
  ): TaskConstraintResource {
    val translatedName =
        constraint.name
            ?: messageSource.getMessage(
                "TaskConstraintEnum_" + constraint.key.name, null, LocaleContextHolder.getLocale())

    return TaskConstraintResource(constraint.key, constraint.active, translatedName).apply {

      // update link to activate or deactivate the task constraint
      add(
          linkFactory
              .linkTo(TaskConstraintController.CONSTRAINTS_BY_PROJECT_ID)
              .withParameters(
                  mapOf(TaskConstraintController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
              .withRel(if (active) LINK_CONSTRAINT_DEACTIVATE else LINK_CONSTRAINT_ACTIVATE))

      // update task constraint link for custom constraints
      addIf(constraint.key.isCustom) {
        linkFactory
            .linkTo(TaskConstraintController.CONSTRAINTS_BY_PROJECT_ID)
            .withParameters(
                mapOf(TaskConstraintController.PATH_VARIABLE_PROJECT_ID to projectIdentifier))
            .withRel(LINK_CONSTRAINT_UPDATE)
      }
    }
  }
}
