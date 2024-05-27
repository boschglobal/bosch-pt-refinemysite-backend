/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_TASK
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.TASK_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepositoryExtension
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query

open class TaskRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    TaskRepositoryExtension {

  override fun find(identifier: UUID, version: Long, projectIdentifier: UUID): Task =
      mongoOperations.findOne(
          findTaskWithVersionQuery(identifier, version, projectIdentifier),
          Task::class.java,
          Collections.PROJECT_STATE)!!

  override fun findLatest(identifier: UUID, projectIdentifier: UUID): Task =
      mongoOperations.findOne(
          findLatestTaskQuery(identifier, projectIdentifier),
          Task::class.java,
          Collections.PROJECT_STATE)!!

  override fun findTasks(projectIdentifier: UUID): List<Task> =
      mongoOperations.find(
          query(
                  CriteriaOperator.and(
                      ProjectContextCriteriaSnippets.belongsToProject(projectIdentifier), isTask()))
              .with(Sort.by(Sort.Direction.DESC, "identifier", "version")),
          Collections.PROJECT_STATE)

  override fun findDisplayName(identifier: UUID, projectIdentifier: UUID): String? {
    val query =
        findLatestTaskQuery(identifier, projectIdentifier).apply {
          fields().include("name").exclude(ID)
        }

    return mongoOperations.findOne(
            query, FindDisplayNameProjection::class.java, Collections.PROJECT_STATE)
        ?.name
  }

  override fun findAssigneeOfTaskWithVersion(
      taskIdentifier: UUID,
      version: Long,
      projectIdentifier: UUID
  ): UUID? {
    val query =
        findTaskWithVersionQuery(taskIdentifier, version, projectIdentifier).apply {
          fields().include("assigneeIdentifier").exclude(ID)
        }
    return mongoOperations.findOne(
            query, FindAssigneeIdentifierProjection::class.java, Collections.PROJECT_STATE)
        ?.assigneeIdentifier
  }

  override fun deleteTaskAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID) {
    val criteria =
        CriteriaOperator.and(
            belongsToProject(projectIdentifier),
            CriteriaOperator.or(isAnyVersionOfTask(identifier), isRelatedToTask(identifier)))
    mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
  }

  private fun findLatestTaskQuery(identifier: UUID, projectIdentifier: UUID): Query =
      query(
              where(ID_TYPE)
                  .`is`(ID_TYPE_VALUE_TASK)
                  .and(ID_IDENTIFIER)
                  .`is`(identifier)
                  // we provide the shard key here to improve performance
                  .and(PROJECT_IDENTIFIER)
                  .`is`(projectIdentifier))
          .with(Sort.by(desc(ID_VERSION)))
          .limit(1)

  private fun findTaskWithVersionQuery(
      identifier: UUID,
      version: Long,
      projectIdentifier: UUID
  ): Query =
      query(
              where(ID_TYPE)
                  .`is`(ID_TYPE_VALUE_TASK)
                  .and(ID_IDENTIFIER)
                  .`is`(identifier)
                  .and(ID_VERSION)
                  .`is`(version)
                  // we provide the shard key here to improve performance
                  .and(PROJECT_IDENTIFIER)
                  .`is`(projectIdentifier))
          .limit(1)

  private fun belongsToProject(identifier: UUID): Criteria =
      where(PROJECT_IDENTIFIER).`is`(identifier)

  private fun isAnyVersionOfTask(identifier: UUID): Criteria =
      where(ID_TYPE).`is`(ID_TYPE_VALUE_TASK).and(ID_IDENTIFIER).`is`(identifier)

  private fun isRelatedToTask(identifier: UUID): Criteria = where(TASK_IDENTIFIER).`is`(identifier)

  private fun isTask(): Criteria = Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_TASK)

  data class FindDisplayNameProjection(var name: String? = null)

  data class FindAssigneeIdentifierProjection(var assigneeIdentifier: UUID? = null)
}
