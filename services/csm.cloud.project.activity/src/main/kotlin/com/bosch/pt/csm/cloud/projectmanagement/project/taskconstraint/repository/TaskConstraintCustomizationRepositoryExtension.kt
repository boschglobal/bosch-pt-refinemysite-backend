/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import java.util.UUID

interface TaskConstraintCustomizationRepositoryExtension {

  fun findLatestCachedByProjectIdentifierAndKey(
      projectIdentifier: UUID,
      key: TaskConstraintEnum
  ): TaskConstraintCustomization?
}
