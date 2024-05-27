/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CommonAttributeNames.ID_TYPE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.ID_TYPE_VALUE_WORK_AREA
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl.ProjectAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.model.WorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.repository.WorkAreaRepositoryExtension
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import java.util.UUID

open class WorkAreaRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : WorkAreaRepositoryExtension {

    override fun findLatest(identifier: UUID, projectIdentifier: UUID): WorkArea =
        mongoOperations.findOne(
            findLatestWorkAreaQuery(identifier, projectIdentifier), WorkArea::class.java, Collections.PROJECT_STATE
        )!!

    override fun deleteWorkArea(identifier: UUID, projectIdentifier: UUID) {
        val criteria = CriteriaOperator.and(belongsToProject(projectIdentifier), isAnyVersionOfWorkArea(identifier))
        mongoOperations.remove(query(criteria), Collections.PROJECT_STATE)
    }

    private fun findLatestWorkAreaQuery(identifier: UUID, projectIdentifier: UUID): Query =
        query(
            Criteria.where(ID_TYPE)
                .`is`(ID_TYPE_VALUE_WORK_AREA)
                .and(ID_IDENTIFIER)
                .`is`(identifier)
                // we provide the shard key here to improve performance
                .and(PROJECT_IDENTIFIER)
                .`is`(projectIdentifier)
        )
            .with(Sort.by(Sort.Order.desc(CommonAttributeNames.ID_VERSION)))
            .limit(1)

    private fun belongsToProject(identifier: UUID): Criteria =
        Criteria.where(PROJECT_IDENTIFIER).`is`(identifier)

    private fun isAnyVersionOfWorkArea(identifier: UUID): Criteria =
        Criteria.where(ID_TYPE).`is`(ID_TYPE_VALUE_WORK_AREA).and(ID_IDENTIFIER).`is`(identifier)
}
