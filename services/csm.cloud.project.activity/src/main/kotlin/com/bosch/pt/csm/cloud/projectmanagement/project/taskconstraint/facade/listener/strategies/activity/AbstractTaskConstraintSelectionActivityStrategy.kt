/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Value
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_COMMONUNDERSTANDING
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_EQUIPMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_EXTERNALFACTORS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_INFORMATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_PRELIMINARYWORK
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_RESOURCES
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_ENUM_SAFEWORKINGENVIRONMENT
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.COMMON_UNDERSTANDING
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.CUSTOM1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.CUSTOM2
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.CUSTOM3
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.CUSTOM4
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.EQUIPMENT
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.EXTERNAL_FACTORS
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.INFORMATION
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.PRELIMINARY_WORK
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.RESOURCES
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

abstract class AbstractTaskConstraintSelectionActivityStrategy :
    AbstractActivityStrategy<TaskConstraintSelectionEventAvro>() {

  @Autowired lateinit var participantService: ParticipantService

  protected fun mapToMessageKeyValue(constraint: TaskConstraintEnum): Value =
      when (constraint) {
        MATERIAL -> SimpleMessageKey(TASK_ACTION_ENUM_MATERIAL)
        RESOURCES -> SimpleMessageKey(TASK_ACTION_ENUM_RESOURCES)
        INFORMATION -> SimpleMessageKey(TASK_ACTION_ENUM_INFORMATION)
        PRELIMINARY_WORK -> SimpleMessageKey(TASK_ACTION_ENUM_PRELIMINARYWORK)
        EXTERNAL_FACTORS -> SimpleMessageKey(TASK_ACTION_ENUM_EXTERNALFACTORS)
        EQUIPMENT -> SimpleMessageKey(TASK_ACTION_ENUM_EQUIPMENT)
        SAFE_WORKING_ENVIRONMENT -> SimpleMessageKey(TASK_ACTION_ENUM_SAFEWORKINGENVIRONMENT)
        COMMON_UNDERSTANDING -> SimpleMessageKey(TASK_ACTION_ENUM_COMMONUNDERSTANDING)
        CUSTOM1 -> LazyValue(CUSTOM1.name, AggregateType.TASKCONSTRAINTCUSTOMIZATION.name)
        CUSTOM2 -> LazyValue(CUSTOM2.name, AggregateType.TASKCONSTRAINTCUSTOMIZATION.name)
        CUSTOM3 -> LazyValue(CUSTOM3.name, AggregateType.TASKCONSTRAINTCUSTOMIZATION.name)
        CUSTOM4 -> LazyValue(CUSTOM4.name, AggregateType.TASKCONSTRAINTCUSTOMIZATION.name)
      }

  protected fun buildSummary(
      messageKeyAvro: String,
      projectIdentifier: UUID,
      taskConstraintSelectionEventAvro: TaskConstraintSelectionEventAvro
  ): Summary {

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, taskConstraintSelectionEventAvro.getLastModifiedByUserIdentifier())

    val originator =
        UnresolvedObjectReference(
            type = PARTICIPANT.type,
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return Summary(
        templateMessageKey = messageKeyAvro, references = mapOf("originator" to originator))
  }

  companion object {
    val taskConstraintOrder =
        listOf(
            RESOURCES,
            INFORMATION,
            EQUIPMENT,
            MATERIAL,
            PRELIMINARY_WORK,
            SAFE_WORKING_ENVIRONMENT,
            EXTERNAL_FACTORS,
            COMMON_UNDERSTANDING,
            CUSTOM1,
            CUSTOM2,
            CUSTOM3,
            CUSTOM4)
  }
}
