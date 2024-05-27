/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.ID_TYPE_VALUE_TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.TASK_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator.and
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets.belongsToProject
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepositoryExtension
import java.util.UUID
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query

open class TaskRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    TaskRepositoryExtension {

  override fun deleteTaskAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID) {
    val criteria =
        and(
            belongsToProject(projectIdentifier),
            CriteriaOperator.or(isAnyVersion(identifier), isRelatedToTask(identifier)))

    mongoOperations.remove(query(criteria), PROJECT_STATE)
  }

  private fun isAnyVersion(identifier: UUID) =
      where(ID_TYPE).`is`(ID_TYPE_VALUE_TASK).and(ID_IDENTIFIER).`is`(identifier)

  private fun isRelatedToTask(identifier: UUID) = where(TASK_IDENTIFIER).`is`(identifier)
}
