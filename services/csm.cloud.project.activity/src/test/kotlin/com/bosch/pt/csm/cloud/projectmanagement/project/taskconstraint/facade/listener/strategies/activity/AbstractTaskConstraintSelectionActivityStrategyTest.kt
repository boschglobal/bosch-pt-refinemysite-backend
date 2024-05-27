/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
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

abstract class AbstractTaskConstraintSelectionActivityStrategyTest :
    AbstractActivityIntegrationTest() {

  protected fun mapTaskConstraintToKey(constraint: TaskConstraintEnum) =
      when (constraint) {
        EQUIPMENT -> Key.TASK_ACTION_ENUM_EQUIPMENT
        MATERIAL -> Key.TASK_ACTION_ENUM_MATERIAL
        RESOURCES -> Key.TASK_ACTION_ENUM_RESOURCES
        INFORMATION -> Key.TASK_ACTION_ENUM_INFORMATION
        PRELIMINARY_WORK -> Key.TASK_ACTION_ENUM_PRELIMINARYWORK
        SAFE_WORKING_ENVIRONMENT -> Key.TASK_ACTION_ENUM_SAFEWORKINGENVIRONMENT
        EXTERNAL_FACTORS -> Key.TASK_ACTION_ENUM_EXTERNALFACTORS
        COMMON_UNDERSTANDING -> Key.TASK_ACTION_ENUM_COMMONUNDERSTANDING
        CUSTOM1 -> "${TASK_CONSTRAINT_ENUM_PREFIX}_CUSTOM1"
        CUSTOM2 -> "${TASK_CONSTRAINT_ENUM_PREFIX}_CUSTOM2"
        CUSTOM3 -> "${TASK_CONSTRAINT_ENUM_PREFIX}_CUSTOM3"
        CUSTOM4 -> "${TASK_CONSTRAINT_ENUM_PREFIX}_CUSTOM4"
      }

  protected fun buildSummary(messageKey: String) =
      buildSummary(
          messageKey = messageKey,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), displayName(fmUser))))

  companion object {
    private val TASK_CONSTRAINT_ENUM_PREFIX = TaskConstraintEnum::class.java.simpleName
  }
}
