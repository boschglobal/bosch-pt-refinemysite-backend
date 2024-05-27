/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2018
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model

enum class TaskConstraintEnum {
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
  CUSTOM4;

  val isCustom: Boolean
    get() = this == CUSTOM1 || this == CUSTOM2 || this == CUSTOM3 || this == CUSTOM4
}
