/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.model

import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum

enum class ObjectType(val type: String) {
  WORKAREA(ProjectmanagementAggregateTypeEnum.WORKAREA.name),
  TASK(ProjectmanagementAggregateTypeEnum.TASK.name),
  MILESTONE(ProjectmanagementAggregateTypeEnum.MILESTONE.name)
}
