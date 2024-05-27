/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.TaskConstraintPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.ProjectTaskConstraints
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraint
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class TaskConstraintPayloadAssembler(private val messageSource: MessageSource) {

  fun assembleAddMissing(
      projectTaskConstraints: ProjectTaskConstraints
  ): List<TaskConstraintPayloadV1> {
    val missingConstraints = getMissingConstraints(projectTaskConstraints)

    return missingConstraints +
        projectTaskConstraints.constraints.map {
          TaskConstraintPayloadMapper.INSTANCE.fromTaskConstraint(it, translate(it.key, it))
        }
  }

  fun assemble(
      projectTaskConstraints: ProjectTaskConstraints,
      constraintKeys: List<TaskConstraintEnum>
  ): List<TaskConstraintPayloadV1> {
    val constraintsWithoutCustomization =
        constraintKeys - projectTaskConstraints.constraints.map { it.key }.toSet()

    return constraintsWithoutCustomization.map { constraintToPayload(it) } +
        projectTaskConstraints.constraints.map {
          TaskConstraintPayloadMapper.INSTANCE.fromTaskConstraint(it, translate(it.key, it))
        }
  }

  private fun getMissingConstraints(
      projectTaskConstraints: ProjectTaskConstraints
  ): List<TaskConstraintPayloadV1> {
    val missingConstraints =
        TaskConstraintEnum.values().toSet() -
            projectTaskConstraints.constraints.map { it.key }.toSet()

    return missingConstraints.map { constraintToPayload(it) }
  }

  private fun constraintToPayload(constraint: TaskConstraintEnum) =
      TaskConstraintPayloadV1(
          id = constraint.id,
          version = -1L,
          key = constraint.shortKey,
          name = translate(constraint, null),
          active = !constraint.isCustom,
          eventDate = constraint.timestamp.toLocalDateTimeByMillis())

  private fun translate(key: TaskConstraintEnum, constraint: TaskConstraint?): String =
      constraint?.name
          ?: messageSource.getMessage(key.messageKey, null, LocaleContextHolder.getLocale())
}
