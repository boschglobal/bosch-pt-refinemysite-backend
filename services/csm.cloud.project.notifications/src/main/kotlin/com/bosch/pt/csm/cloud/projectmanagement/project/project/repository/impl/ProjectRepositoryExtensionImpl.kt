/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_TITLE
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.repository.ProjectRepositoryExtension
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class ProjectRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : ProjectRepositoryExtension {

    override fun findLatest(identifier: UUID): Project =
        mongoOperations.findOne(findLatestProjectQuery(identifier), Project::class.java, Collections.PROJECT_STATE)!!

    override fun findDisplayName(identifier: UUID): String {
        val query = findLatestProjectQuery(identifier).apply {
            fields().include(PROJECT_TITLE).exclude(ID)
        }
        val displayNameProjection = mongoOperations.findOne(
            query, FindDisplayNameProjection::class.java, Collections.PROJECT_STATE
        )
        return displayNameProjection?.title!!
    }

    override fun deleteProjectAndAllRelatedDocuments(identifier: UUID) {
        mongoOperations.remove(query(belongsToProject(identifier)), Collections.PROJECT_STATE)
    }

    private fun findLatestProjectQuery(identifier: UUID): Query =
        query(
            where(ID_TYPE)
                .`is`(ID_TYPE_VALUE_PROJECT)
                .and(ID_IDENTIFIER)
                .`is`(identifier)
                // we provide the shard key here to improve performance
                .and(PROJECT_IDENTIFIER)
                .`is`(identifier)
        )
            .with(Sort.by(desc(ID_VERSION)))
            .limit(1)

    private fun belongsToProject(identifier: UUID): Criteria =
        where(PROJECT_IDENTIFIER).`is`(identifier)

    data class FindDisplayNameProjection(var title: String)
}
