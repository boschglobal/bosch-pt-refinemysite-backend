/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_TASK_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.model.TaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.repository.TaskAttachmentRepositoryExtension
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query.query

open class TaskAttachmentRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    TaskAttachmentRepositoryExtension {

  override fun findTaskAttachments(projectIdentifier: UUID): List<TaskAttachment> =
      mongoOperations.find(
          query(
                  CriteriaOperator.and(
                      ProjectContextCriteriaSnippets.belongsToProject(projectIdentifier),
                      isTaskAttachment()))
              .with(Sort.by(Sort.Direction.DESC, "identifier", "version")),
          Collections.PROJECT_STATE)

  override fun deleteTaskAttachment(identifier: UUID, projectIdentifier: UUID) {
    val criteria =
        CriteriaOperator.and(
            belongsToProject(projectIdentifier), isAnyVersionOfTaskAttachment(identifier))
    mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
  }

  private fun belongsToProject(identifier: UUID): Criteria =
      Criteria.where(PROJECT_IDENTIFIER).`is`(identifier)

  private fun isAnyVersionOfTaskAttachment(identifier: UUID): Criteria =
      Criteria.where(ID_TYPE)
          .`is`(ID_TYPE_VALUE_TASK_ATTACHMENT)
          .and(ID_IDENTIFIER)
          .`is`(identifier)

  private fun isTaskAttachment(): Criteria =
      Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_TASK_ATTACHMENT)
}
