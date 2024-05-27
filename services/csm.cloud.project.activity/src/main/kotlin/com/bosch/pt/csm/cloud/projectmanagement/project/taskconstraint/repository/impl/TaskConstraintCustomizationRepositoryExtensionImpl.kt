/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.ID_TYPE_VALUE_TASK_CONSTRAINT_CUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository.TaskConstraintCustomizationRepositoryExtension
import java.util.UUID
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query

open class TaskConstraintCustomizationRepositoryExtensionImpl
constructor(private val mongoOperations: MongoOperations) :
    TaskConstraintCustomizationRepositoryExtension {

  @Cacheable(cacheNames = ["task-constraint-customization"])
  override fun findLatestCachedByProjectIdentifierAndKey(
      projectIdentifier: UUID,
      key: TaskConstraintEnum
  ): TaskConstraintCustomization? =
      mongoOperations.findOne(
          query(criteria(projectIdentifier, key)).with(Sort.by(desc(ID_VERSION))).limit(1),
          TaskConstraintCustomization::class.java,
          PROJECT_STATE)

  private fun criteria(projectIdentifier: UUID, key: TaskConstraintEnum) =
      Criteria(ID_TYPE)
          .`is`(ID_TYPE_VALUE_TASK_CONSTRAINT_CUSTOMIZATION)
          .and(PROJECT_IDENTIFIER)
          .`is`(projectIdentifier)
          .and("key")
          .`is`(key)
}
