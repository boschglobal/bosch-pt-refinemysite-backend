/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.TOPIC_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository.TopicRepositoryExtension
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class TopicRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : TopicRepositoryExtension {

    override fun findLatest(identifier: UUID, projectIdentifier: UUID): Topic =
        mongoOperations.findOne(
            findLatestTopicQuery(identifier, projectIdentifier),
            Topic::class.java,
            Collections.PROJECT_STATE
        )!!

    override fun deleteTopicAndAllRelatedDocuments(identifier: UUID, projectIdentifier: UUID) {
        val criteria = CriteriaOperator.and(
            belongsToProject(projectIdentifier),
            CriteriaOperator.or(
                isAnyVersionOfTopic(identifier), belongsToTopic(identifier)
            )
        )
        mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
    }

    private fun findLatestTopicQuery(identifier: UUID, projectIdentifier: UUID): Query =
        query(
            where(ID_TYPE)
                .`is`(ID_TYPE_VALUE_TOPIC)
                .and(ID_IDENTIFIER)
                .`is`(identifier)
                // we provide the shard key here to improve performance
                .and(PROJECT_IDENTIFIER)
                .`is`(projectIdentifier)
        )
            .with(Sort.by(desc(ID_VERSION)))
            .limit(1)

    private fun belongsToTopic(identifier: UUID): Criteria =
        where(TOPIC_IDENTIFIER).`is`(identifier)

    private fun belongsToProject(identifier: UUID): Criteria =
        where(PROJECT_IDENTIFIER).`is`(identifier)

    private fun isAnyVersionOfTopic(identifier: UUID): Criteria =
        where(ID_TYPE).`is`(ID_TYPE_VALUE_TOPIC).and(ID_IDENTIFIER).`is`(identifier)
}
