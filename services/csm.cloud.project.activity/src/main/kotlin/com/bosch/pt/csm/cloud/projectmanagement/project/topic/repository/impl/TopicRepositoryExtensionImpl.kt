/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Common.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.ID_TYPE_VALUE_TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.TOPIC_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator.and
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator.or
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectContextCriteriaSnippets.belongsToProject
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.TopicRepositoryExtension
import java.util.UUID
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query

open class TopicRepositoryExtensionImpl(private val mongoOperations: MongoOperations) :
    TopicRepositoryExtension {

  override fun deleteTopicAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID) {
    val query =
        query(
            and(
                belongsToProject(projectIdentifier),
                or(isAnyVersion(identifier), belongsToTopic(identifier))))

    mongoOperations.remove(query, PROJECT_STATE)
  }

  private fun belongsToTopic(identifier: UUID) = where(TOPIC_IDENTIFIER).`is`(identifier)

  private fun isAnyVersion(identifier: UUID) =
      where(ID_TYPE).`is`(ID_TYPE_VALUE_TOPIC).and(ID_IDENTIFIER).`is`(identifier)
}
